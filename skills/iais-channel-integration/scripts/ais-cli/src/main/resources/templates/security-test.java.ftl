/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName];

import com.alibaba.fastjson.JSONObject;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpResponse;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.*;
import com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext;
import [=capability.requestType];
import [=capability.responseType];
import [=packageName].customize.security.ChannelSecurityCustomization;
import [=packageName].extension.ChannelApiExtension;
import [=packageName].extension.ChannelNotificationExtension;
import [=packageName].extension.ChannelTransport;
import [=packageName].extension.message.InboundChannelMessage;
import [=packageName].model.ChannelHttpResult;
import [=packageName].model.ChannelOperation;
import [=packageName].model.ChannelOutboundRequest;
import [=packageName].template.ChannelInvocationTemplate;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Generated wiring tests, not institution protocol or cryptographic conformance tests.
 * Synthetic hooks only supply deterministic test data. No key lookup, real crypto or network executes.
 * [=capability.title]DeliveryTest separately requires the real customization hooks and independent fixtures.
 */
class [=capability.title]SecurityContractTest {
    private static final String MERCHANT = "synthetic-test-merchant";
    private static final String PLAIN_REQUEST = "{\"synthetic\":\"plain\"}";
    private static final String RAW_RESPONSE = "{\"synthetic\":\"response\"}";
    private static final String DECRYPTED_RESPONSE = "{\"synthetic\":\"decrypted\"}";
    private static final String CIPHER_BODY = "{\"ciphertext\":\"synthetic-ciphertext\"}";

    @Test
    void hostClearsIdentityBeforeNextInvocationOnSameThread() {
        Fixture first = new Fixture();
        assertSame(first.result, first.invoke());
        assertThrows(IllegalStateException.class, ChannelRequestContext::current);
        Fixture second = new Fixture();
        assertSame(second.result, second.invoke("synthetic-next-merchant"));
<#list capability.directions as direction><#list security[capability.method][direction] as step><#if step.implementation != "none">
<#if step.implementation == "adapter">
        verify(second.platform).queryKey(argThat(request -> request != null
                && "synthetic-next-merchant".equals(request.getMerchantId())
                && request.getPurpose() == SecurityKeyPurpose.[=step.operation?upper_case]));
<#else>
        verify(second.platform).[=step.operation](argThat(request -> request != null
                && "synthetic-next-merchant".equals(request.getMerchantId())));
</#if>
</#if></#list></#list>
        assertThrows(IllegalStateException.class, ChannelRequestContext::current);
    }
<#list capability.directions as direction>
<#assign directionActive=false><#list security[capability.method][direction] as step><#if step.implementation != "none"><#assign directionActive=true></#if></#list>
<#if directionActive>

    @Test
    void [=direction]RejectsMissingContextBeforeHooksOrPlatformCalls() {
        Fixture fixture = new Fixture();
        ChannelRequestContext.clear();
        assertThrows(IllegalStateException.class, fixture::invoke[=direction?cap_first]Security);
        assertEquals(0, fixture.security.hookCalls);
        verifyNoInteractions(fixture.platform);
    }

    @Test
    void [=direction]RejectsMissingMerchantBeforeHooksOrPlatformCalls() {
        for (String merchant : new String[]{null, "", " "}) {
            Fixture fixture = new Fixture();
            ChannelRequestContext.bind("[=channelCode]", merchant, null);
            try {
                assertThrows(IllegalStateException.class, fixture::invoke[=direction?cap_first]Security);
                assertEquals(0, fixture.security.hookCalls);
                verifyNoInteractions(fixture.platform);
            } finally {
                ChannelRequestContext.clear();
            }
        }
        assertThrows(IllegalStateException.class, ChannelRequestContext::current);
    }
</#if></#list>

    @Test
    void selectedStepsPassTypedArgumentsInOrder() {
        Fixture fixture = new Fixture();
        assertSame(fixture.result, fixture.invoke());
        InOrder calls = inOrder(fixture.platform, fixture.http);
<#assign requestBody="PLAIN_REQUEST"><#assign inboundBody="RAW_RESPONSE">
<#list capability.directions as direction>
<#if direction == "response">
        ArgumentCaptor<HttpRequest> sent = ArgumentCaptor.forClass(HttpRequest.class);
        calls.verify(fixture.http).executeDynamicUrl(same(fixture.input), eq(Collections.emptyMap()), sent.capture());
        assertEquals([=requestBody], sent.getValue().getBody(), "HTTP must receive the protected body, never stale plaintext");
<#list security[capability.method]["request"] as requestStep><#if requestStep.operation == "sign" && requestStep.implementation != "none">
        assertEquals("synthetic-signature", sent.getValue().getHeaders().get("X-Synthetic-Signature"));
</#if></#list>
</#if>
<#list security[capability.method][direction] as step><#if step.implementation != "none">
<#assign id=direction+step.operation?cap_first>
<#if step.implementation == "adapter">
        SecurityKeyRequest [=id] = new SecurityKeyRequest(MERCHANT,
                SecurityKeyPurpose.[=step.operation?upper_case], SecurityKeyAlgorithm.[=step.keyAlgorithm]);
        calls.verify(fixture.platform).queryKey(eq([=id]));
<#else>
<#assign type="Security"+step.operation?cap_first+"Request">
        ArgumentCaptor<[=type]> [=id] = ArgumentCaptor.forClass([=type].class);
        calls.verify(fixture.platform).[=step.operation]([=id].capture());
        assertEquals(MERCHANT, [=id].getValue().getMerchantId());
<#if step.operation == "sign" || step.operation == "verify">
        assertEquals(SecuritySignAlgorithm.[=step.algorithm], [=id].getValue().getAlgorithm());
<#else>
        assertEquals(SecurityCipherAlgorithm.[=step.algorithm], [=id].getValue().getAlgorithm());
        assertEquals(SecurityCipherType.[=step.cipherType], [=id].getValue().getCipherType());
        assertEquals("synthetic-runtime-iv", [=id].getValue().getParameters().getIvBase64());
        assertEquals("synthetic-runtime-aad", [=id].getValue().getParameters().getAad());
<#if step.parameters?size gt 0>
        assertEquals(SecurityCipherMode.[=step.parameters.mode], [=id].getValue().getParameters().getMode());
        assertEquals(SecurityCipherPadding.[=step.parameters.padding], [=id].getValue().getParameters().getPadding());
<#if step.parameters.tagBitLength??>
        assertEquals(Integer.valueOf([=step.parameters.tagBitLength?c]), [=id].getValue().getParameters().getTagBitLength());
</#if>
</#if>
</#if>
<#if direction == "request">
        assertEquals([=requestBody], [=id].getValue().getContent());
<#elseif step.operation == "verify">
        assertEquals([=inboundBody], [=id].getValue().getContent());
        assertEquals("synthetic-response-signature", [=id].getValue().getSignature());
<#else>
        assertEquals([=inboundBody], [=id].getValue().getCiphertext());
</#if>
</#if>
<#if step.operation == "encrypt"><#assign requestBody="CIPHER_BODY"></#if>
<#if step.operation == "decrypt"><#assign inboundBody="DECRYPTED_RESPONSE"></#if>
</#if></#list></#list>
        calls.verifyNoMoreInteractions();
<#if capability.notification>
        verifyNoInteractions(fixture.http);
        verify(fixture.mapping).validate(same(fixture.input), eq([=inboundBody]));
        verify(fixture.mapping).map(same(fixture.input), eq([=inboundBody]));
<#else>
        ArgumentCaptor<ChannelHttpResult> mapped = ArgumentCaptor.forClass(ChannelHttpResult.class);
        verify(fixture.mapping).mapResponse(same(fixture.input), mapped.capture());
        assertEquals([=inboundBody], mapped.getValue().getBody());
        assertEquals(RAW_RESPONSE, mapped.getValue().getRawBody());
</#if>
    }
<#list capability.directions as direction><#list security[capability.method][direction] as step><#if step.implementation != "none">
<#assign id=direction+step.operation?cap_first>

    @Test
    void [=id]FailureAbortsBefore<#if direction == "request">Http<#else>Mapping</#if>() {
        Fixture fixture = new Fixture();
        SecurityException failure = new SecurityException("synthetic failure, no real keys");
<#if step.implementation == "adapter">
        when(fixture.platform.queryKey(argThat(request -> request != null
                && request.getPurpose() == SecurityKeyPurpose.[=step.operation?upper_case]))).thenThrow(failure);
<#else>
        when(fixture.platform.[=step.operation](any(Security[=step.operation?cap_first]Request.class))).thenThrow(failure);
</#if>
        assertSame(failure, assertThrows(SecurityException.class, fixture::invoke));
        assertThrows(IllegalStateException.class, ChannelRequestContext::current);
<#if direction == "request" || capability.notification>
        verifyNoInteractions(fixture.http);
<#else>
        verify(fixture.http).executeDynamicUrl(same(fixture.input), anyMap(), any(HttpRequest.class));
</#if>
<#if capability.notification>
        verifyNoInteractions(fixture.mapping);
<#else>
        verify(fixture.mapping, never()).mapResponse(any(), any());
</#if>
    }
<#if step.implementation == "platform" && step.operation == "verify">

    @Test
    void [=id]FalseAbortsBeforeMapping() {
        Fixture fixture = new Fixture();
        when(fixture.platform.verify(any(SecurityVerifyRequest.class))).thenReturn(false);
        assertThrows(SecurityException.class, fixture::invoke);
        assertThrows(IllegalStateException.class, ChannelRequestContext::current);
<#if capability.notification>
        verifyNoInteractions(fixture.http, fixture.mapping);
<#else>
        verify(fixture.http).executeDynamicUrl(same(fixture.input), anyMap(), any(HttpRequest.class));
        verify(fixture.mapping, never()).mapResponse(any(), any());
</#if>
    }
</#if>
</#if></#list></#list>

    private static final class Fixture {
        final PlatformChannelSecurityService platform = mock(PlatformChannelSecurityService.class);
        final PlatformChannelHttpService http = mock(PlatformChannelHttpService.class);
        final SecurityKeyMaterial key = mock(SecurityKeyMaterial.class);
        final [=capability.requestName] input = new [=capability.requestName]();
        final [=capability.responseName] result = mock([=capability.responseName].class);
<#if capability.notification>
        final ChannelNotificationExtension<[=capability.responseName]> mapping;
<#else>
        final ChannelApiExtension<[=capability.requestName], [=capability.responseName]> mapping;
</#if>
        final ChannelInvocationTemplate template;
        final SyntheticSecurity security;

        @SuppressWarnings("unchecked")
        Fixture() {
<#if capability.notification>
            mapping = mock(ChannelNotificationExtension.class);
            input.setRawBody(RAW_RESPONSE);
            when(mapping.map(same(input), anyString())).thenReturn(result);
<#else>
            mapping = mock(ChannelApiExtension.class);
            when(mapping.mapRequestBody(input)).thenReturn(JSONObject.parseObject(PLAIN_REQUEST));
            when(mapping.mapUrlParameters(input)).thenReturn(Collections.emptyMap());
            when(mapping.mapResponse(same(input), any(ChannelHttpResult.class))).thenReturn(result);
            when(http.executeDynamicUrl(same(input), anyMap(), any(HttpRequest.class))).thenReturn(
                    HttpResponse.builder().statusCode(200).headers(Collections.emptyMap()).body(RAW_RESPONSE).build());
</#if>
<#list capability.directions as direction><#list security[capability.method][direction] as step><#if step.implementation != "none">
<#if step.implementation == "adapter">
            when(platform.queryKey(argThat(request -> request != null
                    && request.getPurpose() == SecurityKeyPurpose.[=step.operation?upper_case]))).thenReturn(key);
<#elseif step.operation == "verify">
            when(platform.verify(any(SecurityVerifyRequest.class))).thenReturn(true);
<#elseif step.operation == "decrypt">
            when(platform.decrypt(any(SecurityDecryptRequest.class))).thenReturn(DECRYPTED_RESPONSE);
<#else>
            when(platform.[=step.operation](any(Security[=step.operation?cap_first]Request.class))).thenReturn("synthetic-<#if step.operation == "encrypt">ciphertext<#else>signature</#if>");
</#if>
</#if></#list></#list>
            security = new SyntheticSecurity(<#if activeSecurity>platform, key</#if>);
            template = new ChannelInvocationTemplate(security, mock(ChannelTransport.class), http);
        }

        [=capability.responseName] invoke() {
            return invoke(MERCHANT);
        }

        [=capability.responseName] invoke(String merchant) {
            // Only this test host owns bind/clear; adapter production code only reads the context.
            input.setChannelCode("[=channelCode]");
            input.setRuntimeEnv(null);
            input.setRequestHeaders(Collections.singletonMap("merchantaccount", "synthetic-untrusted-business-merchant"));
            ChannelRequestContext.bind("[=channelCode]", merchant, null);
            try {
<#if capability.notification>
                return template.executeNotification(ChannelOperation.[=capability.operation], input, mapping);
<#else>
                return template.executeDynamicUrl(ChannelOperation.[=capability.operation], input, mapping);
</#if>
            } finally {
                ChannelRequestContext.clear();
            }
        }
<#list capability.directions as direction>
<#assign directionActive=false><#list security[capability.method][direction] as step><#if step.implementation != "none"><#assign directionActive=true></#if></#list>
<#if directionActive>

        void invoke[=direction?cap_first]Security() {
<#if direction == "request">
            security.protectRequestToChannel(new ChannelOutboundRequest(ChannelOperation.[=capability.operation],
                    input, JSONObject.parseObject(PLAIN_REQUEST)));
<#else>
            security.unprotect[=direction?cap_first]FromChannel(new InboundChannelMessage(ChannelOperation.[=capability.operation],
                    input, 200, Collections.emptyMap(), RAW_RESPONSE));
</#if>
        }
</#if></#list>
    }

    /** Test-only hooks. They deliberately do not implement a real institution's protocol or crypto. */
    private static final class SyntheticSecurity extends ChannelSecurityCustomization {
        int hookCalls;
<#if activeSecurity>
        private final SecurityKeyMaterial expectedKey;

        SyntheticSecurity(PlatformChannelSecurityService platform, SecurityKeyMaterial expectedKey) {
            super(platform);
            this.expectedKey = expectedKey;
        }
</#if>
<#list capability.directions as direction><#list security[capability.method][direction] as step><#if step.implementation != "none">
<#assign id=capability.title+direction?cap_first+step.operation?cap_first>
<#assign context="InboundChannelMessage message, String plainBody">
<#if direction == "request"><#assign context="ChannelOutboundRequest message"></#if>
<#if step.implementation == "adapter">

        @Override
        protected <#if direction == "request">void<#else>String</#if> apply[=id]([=context], SecurityKeyMaterial key) {
            hookCalls++;
            assertSame(expectedKey, key, "Pass the exact platform key object to the custom hook");
<#if step.operation == "sign">
            message.putHeader("X-Synthetic-Signature", "synthetic-signature");
<#elseif step.operation == "encrypt">
            message.setBody(JSONObject.parseObject(CIPHER_BODY));
<#elseif step.operation == "decrypt">
            return DECRYPTED_RESPONSE;
<#else>
            return plainBody;
</#if>
        }
<#else>
<#assign type="Security"+step.operation?cap_first+"Request">

        @Override
        protected [=type] build[=id]([=context]) {
            hookCalls++;
            [=type] request = new [=type]();
            // Deliberately wrong test identity proves hooks cannot override the platform context.
            request.setMerchantId("synthetic-untrusted-hook-merchant");
<#if direction == "request">
            request.setContent(message.getBody().toJSONString());
<#elseif step.operation == "verify">
            request.setContent(plainBody);
            request.setSignature("synthetic-response-signature");
<#else>
            request.setCiphertext(plainBody);
</#if>
<#if step.operation == "encrypt" || step.operation == "decrypt">
            SecurityCipherParameters parameters = new SecurityCipherParameters();
            // Opaque sentinel values: no crypto runs and these are NOT usable production IV/AAD defaults.
            parameters.setIvBase64("synthetic-runtime-iv");
            parameters.setAad("synthetic-runtime-aad");
            request.setParameters(parameters);
</#if>
            return request;
        }
<#if direction == "request">

        @Override
        protected void apply[=id](ChannelOutboundRequest message, String result) {
<#if step.operation == "sign">
            message.putHeader("X-Synthetic-Signature", result);
<#else>
            JSONObject envelope = new JSONObject();
            envelope.put("ciphertext", result);
            message.setBody(envelope);
</#if>
        }
</#if>
</#if>
</#if></#list></#list>
    }
}
