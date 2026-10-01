/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName];

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpResponse;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.PlatformChannelSecurityService;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecuritySignRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityVerifyRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityEncryptRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityDecryptRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityKeyRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityKeyMaterial;
import com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext;
import [=capability.requestType];
import [=capability.responseType];
<#assign service="PaymentService"><#if capability.family == "refund"><#assign service="RefundService"></#if><#if capability.family == "notify"><#assign service="NotificationService"></#if>
import com.alipay.iacqintegrationhub.channel.sdk.spi.[=capability.family].[=service];
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Replace synthetic fixtures with independently specified protocol examples; never use production secrets. */
class [=capability.title]DeliveryTest {
    @Test
    void selectedSpiMatchesConfirmedProtocol() throws Exception {
        PlatformChannelHttpService http = mock(PlatformChannelHttpService.class);
        PlatformChannelSecurityService security = mock(PlatformChannelSecurityService.class);
        // Independently specify each typed SDK request and mock result in security.json.
        // This calls the REAL customization hooks. Contract-test subclasses cannot satisfy this test.
        // Use only test keys/vectors; mocked computation is not an algorithm-conformance proof.
        JSONObject securityFixture = JSON.parseObject(fixture("security"));
        // Simulate host-owned identity independently of the business request or notification payload.
        JSONObject contextFixture = JSON.parseObject(fixture("context"));
        String channelCode = requiredText(contextFixture, "channelCode");
        String merchantId = requiredText(contextFixture, "merchantId");
        assertTrue(contextFixture.containsKey("runtimeEnv"), "Specify runtimeEnv explicitly, including null");
        assertTrue(contextFixture.get("runtimeEnv") == null || contextFixture.get("runtimeEnv") instanceof String,
                "runtimeEnv must be a string or explicit null");
<#list capability.directions as direction><#list security[capability.method][direction] as step><#if step.implementation != "none">
<#assign id=direction+step.operation?cap_first>
<#if step.implementation == "adapter">
        SecurityKeyRequest [=id]Request = requiredObject(securityFixture, "[=id]Request", SecurityKeyRequest.class);
        SecurityKeyMaterial [=id]Result = requiredObject(securityFixture, "[=id]Result", SecurityKeyMaterial.class);
        when(security.queryKey(eq([=id]Request))).thenReturn([=id]Result);
<#else>
<#assign type="Security"+step.operation?cap_first+"Request">
        [=type] [=id]Request = requiredObject(securityFixture, "[=id]Request", [=type].class);
<#if step.operation == "verify">
        when(security.verify(eq([=id]Request))).thenReturn(true);
<#else>
        when(security.[=step.operation](eq([=id]Request))).thenReturn(requiredText(securityFixture, "[=id]Result"));
</#if>
</#if>
</#if></#list></#list>
<#if !capability.notification>
        when(http.executeDynamicUrl(any(), anyMap(), any())).thenReturn(HttpResponse.builder()
                .statusCode(200).headers(JSON.parseObject(fixture("response-headers"),
                        new TypeReference<Map<String, String>>() { })).body(fixture("response")).build());
</#if>
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(PlatformChannelHttpService.class, () -> http);
            context.registerBean(PlatformChannelSecurityService.class, () -> security);
            context.scan("[=packageName]");
            context.refresh();
            [=capability.requestName] input = JSON.parseObject(fixture("input"), [=capability.requestName].class);
            // Simulate the facade overwriting routing fields from its trusted context, not from the body.
            input.setChannelCode(channelCode);
            input.setRuntimeEnv(contextFixture.getString("runtimeEnv"));
            [=capability.responseName] result;
            ChannelRequestContext.bind(channelCode, merchantId, contextFixture.getString("runtimeEnv"));
            try {
                result = context.getBean([=service].class).[=capability.method](input);
            } finally {
                ChannelRequestContext.clear();
            }
            assertThrows(IllegalStateException.class, ChannelRequestContext::current);
            assertNotNull(result, "A selected SPI must not return null");
            assertEquals(JSON.parseObject(fixture("expected-result")), JSON.toJSON(result));
<#if capability.notification>
            verifyNoInteractions(http);
<#else>
            ArgumentCaptor<HttpRequest> sent = ArgumentCaptor.forClass(HttpRequest.class);
            JSONObject expected = JSON.parseObject(fixture("expected-request"));
            JSONObject pathFixture = requiredMap(expected, "pathParameters");
            Map<String, String> expectedPath = null;
            if (pathFixture != null) {
                expectedPath = JSON.parseObject(pathFixture.toJSONString(),
                        new TypeReference<Map<String, String>>() { });
            }
            verify(http).executeDynamicUrl(same(input), eq(expectedPath), sent.capture());
            assertEquals(requiredText(expected, "method"), sent.getValue().getMethod());
            assertEquals(requiredText(expected, "contentType"), sent.getValue().getContentType().name());
            assertEquals(requiredMap(expected, "headers"), JSON.toJSON(sent.getValue().getHeaders()));
            assertEquals(requiredMap(expected, "queryParameters"), JSON.toJSON(sent.getValue().getQueryParams()));
            assertEquals(requiredMap(expected, "formParameters"), JSON.toJSON(sent.getValue().getFormParams()));
            assertEquals(requiredMap(expected, "attributes"), JSON.toJSON(sent.getValue().getAttributes()));
            assertTrue(expected.containsKey("body"), "Specify body explicitly, including null for an intentionally absent body");
            assertTrue(expected.get("body") == null || expected.get("body") instanceof String,
                    "body must be exact serialized text or explicit null, not a JSON object");
            assertEquals(expected.getString("body"), sent.getValue().getBody(), "Assert the exact serialized body supplied to platform HTTP");
</#if>
<#list capability.directions as direction><#list security[capability.method][direction] as step><#if step.implementation != "none">
<#assign id=direction+step.operation?cap_first>
<#if step.implementation == "adapter">
            verify(security).queryKey(eq([=id]Request));
<#else>
            verify(security).[=step.operation](eq([=id]Request));
</#if>
</#if></#list></#list>
        }
    }

    private <T> T requiredObject(JSONObject fixture, String name, Class<T> type) {
        assertNotNull(fixture.getJSONObject(name), "Complete independent fixture object: " + name);
        return fixture.getJSONObject(name).toJavaObject(type);
    }

    private String requiredText(JSONObject fixture, String name) {
        assertTrue(fixture.get(name) instanceof String, "Complete independent fixture string: " + name);
        return fixture.getString(name);
    }

    private JSONObject requiredMap(JSONObject fixture, String name) {
        assertTrue(fixture.containsKey(name), "Specify map explicitly ({} and null are different): " + name);
        assertTrue(fixture.get(name) == null || fixture.get(name) instanceof JSONObject,
                "Expected a JSON object or explicit null: " + name);
        return fixture.getJSONObject(name);
    }

    private String fixture(String name) throws Exception {
        String path = "/scenarios/[=capability.method]/" + name + ".json";
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertNotNull(input, path);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) != -1) {
                bytes.write(buffer, 0, length);
            }
            return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
