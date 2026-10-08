/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName];

import com.alibaba.fastjson.JSONObject;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpResponse;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService;
import com.alipay.iacqintegrationhub.channel.sdk.spi.payment.request.PayRequest;
import [=packageName].extension.ChannelApiExtension;
import [=packageName].extension.ChannelMessageSecurity;
import [=packageName].extension.ChannelTransport;
import [=packageName].model.ChannelHttpResult;
import [=packageName].model.ChannelOperation;
import [=packageName].model.ChannelOutboundRequest;
import [=packageName].support.ChannelIntegrationException;
import [=packageName].template.ChannelInvocationTemplate;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Framework behavior only; real institution acceptance belongs to the SPI delivery scenarios. */
class TemplateRuntimeTest {
    private final PlatformChannelHttpService http = mock(PlatformChannelHttpService.class);
    private final ChannelMessageSecurity security = mock(ChannelMessageSecurity.class);
    private final ChannelTransport transport = mock(ChannelTransport.class);
    private final ChannelInvocationTemplate template = new ChannelInvocationTemplate(security, transport, http);
    private final PayRequest request = new PayRequest();

    @Test
    void bodylessGetKeepsAbsentBodyAndPlatformOwnedRoute() {
        doAnswer(call -> {
            ChannelOutboundRequest message = call.getArgument(0);
            message.setMethod("GET");
            message.setRawBody(null);
            return null;
        }).when(transport).customize(any());
        when(http.executeDynamicUrl(same(request), anyMap(), any())).thenReturn(response(200, "accepted"));
        when(security.unprotectResponseFromChannel(any())).thenReturn("accepted");

        assertEquals("accepted", template.executeDynamicUrl(ChannelOperation.PAY, request, mapping(null, false)));
        HttpRequest sent = sentRequest();
        assertEquals("GET", sent.getMethod());
        assertNull(sent.getBody());
        verify(http).executeDynamicUrl(same(request), eq(Collections.emptyMap()), any());
        verifyNoMoreInteractions(http);
    }

    @Test
    void headerSignatureSeesTheExactRawTextSuppliedToHttp() {
        String raw = "<order>one\n&amp;two</order>";
        doAnswer(call -> {
            ChannelOutboundRequest message = call.getArgument(0);
            message.setRawBody(raw);
            message.setContentType(null);
            message.putHeader("Content-Type", "text/plain; charset=utf-8");
            return null;
        }).when(transport).customize(any());
        doAnswer(call -> {
            ChannelOutboundRequest message = call.getArgument(0);
            message.putHeader("Signed-Body", message.getSerializedBody());
            return null;
        }).when(security).protectRequestToChannel(any());
        when(http.executeDynamicUrl(same(request), anyMap(), any())).thenReturn(response(200, "accepted"));
        template.executeDynamicUrl(ChannelOperation.PAY, request, mapping(null, false));

        HttpRequest sent = sentRequest();
        assertEquals(raw, sent.getBody());
        assertNull(sent.getContentType());
        assertEquals("text/plain; charset=utf-8", sent.getHeaders().get("Content-Type"));
        assertEquals(sent.getBody(), sent.getHeaders().get("Signed-Body"));
    }

    @Test
    void encryptedTextIsNotWrappedOrReserializedAsJson() {
        doAnswer(call -> {
            ChannelOutboundRequest message = call.getArgument(0);
            message.setRawBody("encoded-test-ciphertext");
            return null;
        }).when(security).protectRequestToChannel(any());
        when(http.executeDynamicUrl(same(request), anyMap(), any())).thenReturn(response(200, "accepted"));
        template.executeDynamicUrl(ChannelOperation.PAY, request, mapping(new JSONObject(), false));
        assertEquals("encoded-test-ciphertext", sentRequest().getBody());
    }

    @Test
    void explicitlyAcceptedBusinessErrorIsSecuredBeforeMapping() {
        when(http.executeDynamicUrl(same(request), anyMap(), any())).thenReturn(response(422, "signed-error"));
        when(security.unprotectResponseFromChannel(any())).thenReturn("business-rejected");
        assertEquals("business-rejected", template.executeDynamicUrl(ChannelOperation.PAY, request,
                mapping(new JSONObject(), true)));
        verify(security).unprotectResponseFromChannel(argThat(message ->
                Integer.valueOf(422).equals(message.getStatusCode()) && "signed-error".equals(message.getRawBody())));
    }

    @Test
    void defaultPolicyRejectsHttpFailureBeforeResponseSecurity() {
        when(http.executeDynamicUrl(same(request), anyMap(), any())).thenReturn(response(502, "bad gateway"));
        ChannelIntegrationException failure = assertThrows(ChannelIntegrationException.class,
                () -> template.executeDynamicUrl(ChannelOperation.PAY, request, mapping(new JSONObject(), false)));
        assertTrue(failure.getMessage().contains("status=502"));
        verify(security, never()).unprotectResponseFromChannel(any());
    }

    @Test
    void emptyHttpResultFailsClearlyWithoutResponseSecurity() {
        assertThrows(ChannelIntegrationException.class,
                () -> template.executeDynamicUrl(ChannelOperation.PAY, request, mapping(new JSONObject(), false)));
        verify(security, never()).unprotectResponseFromChannel(any());
    }

    @Test
    void requestSecurityFailureStopsHttpForFixedAndDynamicCalls() {
        doThrow(new SecurityException("test protection failure")).when(security).protectRequestToChannel(any());
        assertThrows(SecurityException.class,
                () -> template.executeFixedUrl(ChannelOperation.PAY, request, mapping(new JSONObject(), false)));
        assertThrows(SecurityException.class,
                () -> template.executeDynamicUrl(ChannelOperation.PAY, request, mapping(new JSONObject(), false)));
        verifyNoInteractions(http);
    }

    @Test
    void switchingBetweenJsonAndRawBodiesDoesNotKeepStaleText() {
        JSONObject json = new JSONObject(true);
        json.put("reference", "one");
        ChannelOutboundRequest message = new ChannelOutboundRequest(ChannelOperation.PAY, request, json);
        assertEquals(json.toJSONString(), message.getSerializedBody());
        message.setRawBody("raw");
        assertNull(message.getBody());
        assertEquals("raw", message.getSerializedBody());
        message.setBody(json);
        assertEquals(json.toJSONString(), message.getSerializedBody());
        message.setRawBody(null);
        assertNull(message.getSerializedBody());
    }

    @Test
    void mappedSnapshotAndFinalBodyHaveIndependentOrderedTopLevelFields() {
        JSONObject mapped = new JSONObject(true);
        mapped.put("reference", "one");
        mapped.put("amount", "2");
        ChannelOutboundRequest message = new ChannelOutboundRequest(ChannelOperation.PAY, request, mapped);
        mapped.put("reference", "changed-after-mapping");
        assertEquals("one", message.getBody().getString("reference"));
        message.getMappedBody().put("reference", "changed-copy");
        assertEquals("one", message.getMappedBody().getString("reference"));
        assertEquals("one", message.getBody().getString("reference"));
        message.getBody().put("Signature", "synthetic");
        assertFalse(message.getMappedBody().containsKey("Signature"));
        assertEquals("{\"reference\":\"one\",\"amount\":\"2\",\"Signature\":\"synthetic\"}",
                message.getSerializedBody());
    }

    private HttpRequest sentRequest() {
        ArgumentCaptor<HttpRequest> sent = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).executeDynamicUrl(same(request), anyMap(), sent.capture());
        return sent.getValue();
    }

    private HttpResponse response(int status, String body) {
        return HttpResponse.builder().statusCode(status).headers(Collections.emptyMap()).body(body).build();
    }

    private ChannelApiExtension<PayRequest, String> mapping(JSONObject body, boolean acceptsBusinessError) {
        return new ChannelApiExtension<PayRequest, String>() {
            @Override
            public void validate(PayRequest input) {
                assertSame(request, input);
            }

            @Override
            public JSONObject mapRequestBody(PayRequest input) {
                return body;
            }

            @Override
            public boolean acceptResponseStatus(int statusCode) {
                if (acceptsBusinessError && statusCode == 422) {
                    return true;
                }
                return ChannelApiExtension.super.acceptResponseStatus(statusCode);
            }

            @Override
            public String mapResponse(PayRequest input, ChannelHttpResult result) {
                return result.getBody();
            }
        };
    }
}
