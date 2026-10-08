/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName];

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alipay.iacqintegrationhub.channel.sdk.api.error.ResultCodeService;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpResponse;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.*;
import com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext;
import [=capability.requestType];
import [=capability.responseType];
<#assign service="PaymentService"><#if capability.family == "refund"><#assign service="RefundService"></#if><#if capability.family == "notify"><#assign service="NotificationService"></#if>
import com.alipay.iacqintegrationhub.channel.sdk.spi.[=capability.family].[=service];
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Executes the real SPI and customization hooks against independent protocol cases.
 * Platform boundary mocks verify assembly and failure handling, not cryptographic conformance.
 * Add confirmed JSON cases under scenarios/[=capability.method]; never derive expectations from the mapper.
 */
class [=capability.title]DeliveryTest {
    static Stream<DeliveryScenario> scenarios() throws Exception {
        return DeliveryScenario.load([=capability.title]DeliveryTest.class, "[=capability.method]");
    }

    @DisplayName("selectedSpiMatchesConfirmedProtocol")
    @ParameterizedTest(name = "case={0}")
    @MethodSource("scenarios")
    void selectedSpiMatchesConfirmedProtocol(DeliveryScenario scenario) throws Exception {
        // All fixtures and expected exceptions are validated BEFORE invoking application code.
        PlatformChannelHttpService http = mock(PlatformChannelHttpService.class);
        PlatformChannelSecurityService security = mock(PlatformChannelSecurityService.class);
        ResultCodeService resultCodes = mock(ResultCodeService.class);
        [=capability.requestName] input = scenario.input([=capability.requestName].class);
        JSONObject contextFixture = scenario.context();
        String channelCode = scenario.text(contextFixture, "channelCode");
        String merchantId = contextFixture.getString("merchantId");
        if (input != null) {
            input.setChannelCode(channelCode);
            input.setRuntimeEnv(contextFixture.getString("runtimeEnv"));
        }
        JSONObject securityFixture = scenario.security();
        scenario.requireSecuritySteps(<#assign first=true><#list capability.directions as direction><#list security[capability.method][direction] as step><#if step.implementation != "none"><#if !first>, </#if>"[=direction][=step.operation?cap_first]"<#assign first=false></#if></#list></#list>);
<#list capability.directions as direction><#list security[capability.method][direction] as step><#if step.implementation != "none">
<#assign id=direction+step.operation?cap_first>
<#assign type="Security"+step.operation?cap_first+"Request"><#if step.implementation == "adapter"><#assign type="SecurityKeyRequest"></#if>
        JSONObject [=id] = scenario.boundary(securityFixture, "[=id]");
        [=type] [=id]Request = null;
        if (scenario.calls([=id]) > 0) {
            [=id]Request = scenario.object([=id], "request", [=type].class);
<#assign method=step.operation><#if step.implementation == "adapter"><#assign method="queryKey"></#if>
            if ([=id].containsKey("exception")) {
                when(security.[=method](eq([=id]Request))).thenThrow(scenario.exception([=id], "exception"));
            } else {
<#if step.implementation == "adapter">
                when(security.queryKey(eq([=id]Request))).thenReturn(scenario.object([=id], "result", SecurityKeyMaterial.class));
<#elseif step.operation == "verify">
                when(security.verify(eq([=id]Request))).thenReturn(scenario.bool([=id], "result"));
<#else>
                when(security.[=step.operation](eq([=id]Request))).thenReturn(scenario.text([=id], "result"));
</#if>
            }
        }
</#if></#list></#list>
        scenario.stubResultCodes(resultCodes);
<#if capability.notification>
        assertEquals(0, scenario.httpCalls(), "Notification mapping must not make an outbound HTTP call");
<#else>
        if (scenario.httpCalls() > 0) {
            JSONObject expected = scenario.expectedRequest();
            Map<String, String> expectedPath = expectedPath(scenario, expected);
            RuntimeException httpFailure = null;
            HttpResponse httpResponse = null;
            if (scenario.http().containsKey("exception")) {
                httpFailure = scenario.exception(scenario.http(), "exception");
            } else {
                httpResponse = scenario.httpResponse();
            }
            RuntimeException configuredFailure = httpFailure;
            HttpResponse configuredResponse = httpResponse;
            // Assert at the service boundary. Later mutations of a shared header/path map cannot hide bad delivery.
            when(http.executeDynamicUrl(any(), any(), any())).thenAnswer(call -> {
                assertSame(input, call.getArgument(0), scenario.label("request.context"));
                assertEquals(expectedPath, call.getArgument(1), scenario.label("request.pathParameters"));
                assertHttpRequest(scenario, expected, call.getArgument(2));
                if (configuredFailure != null) {
                    throw configuredFailure;
                }
                return configuredResponse;
            });
        }
</#if>
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(PlatformChannelHttpService.class, () -> http);
            context.registerBean(PlatformChannelSecurityService.class, () -> security);
            context.registerBean(ResultCodeService.class, () -> resultCodes);
            context.scan("[=packageName]");
            context.refresh();
            [=capability.responseName] result = null;
            RuntimeException failure = null;
            ChannelRequestContext.clear();
            if (scenario.bool(contextFixture, "present")) {
                ChannelRequestContext.bind(channelCode, merchantId, contextFixture.getString("runtimeEnv"));
            }
            try {
                result = context.getBean([=service].class).[=capability.method](input);
            } catch (RuntimeException thrown) {
                failure = thrown;
            } finally {
                ChannelRequestContext.clear();
            }
            assertThrows(IllegalStateException.class, ChannelRequestContext::current);
            scenario.assertOutcome(result, failure);
<#if capability.notification>
            verifyNoInteractions(http);
<#else>
            if (scenario.httpCalls() == 0) {
                verifyNoInteractions(http);
            } else {
                ArgumentCaptor<HttpRequest> sent = ArgumentCaptor.forClass(HttpRequest.class);
                verify(http, times(scenario.httpCalls())).executeDynamicUrl(same(input), any(), sent.capture());
                verifyNoMoreInteractions(http);
            }
</#if>
<#list capability.directions as direction><#list security[capability.method][direction] as step><#if step.implementation != "none">
<#assign id=direction+step.operation?cap_first><#assign method=step.operation><#if step.implementation == "adapter"><#assign method="queryKey"></#if>
            if (scenario.calls([=id]) > 0) {
                verify(security, times(scenario.calls([=id]))).[=method](eq([=id]Request));
            }
</#if></#list></#list>
            // Restrict only host service boundaries, never internal helper implementation details.
            verifyNoMoreInteractions(security);
            scenario.verifyResultCodes(resultCodes);
        }
    }
<#if !capability.notification>

    private Map<String, String> expectedPath(DeliveryScenario scenario, JSONObject expected) {
        JSONObject path = scenario.map(expected, "pathParameters");
        if (path == null) { return null; }
        return JSON.parseObject(path.toJSONString(), new TypeReference<Map<String, String>>() { });
    }

    private void assertHttpRequest(DeliveryScenario scenario, JSONObject expected, HttpRequest actual) {
        assertNotNull(actual, scenario.label("request"));
        assertEquals(scenario.text(expected, "method"), actual.getMethod(), scenario.label("request.method"));
        String contentType = null;
        if (actual.getContentType() != null) {
            contentType = actual.getContentType().name();
        }
        assertEquals(scenario.body(expected, "contentType"), contentType, scenario.label("request.contentType"));
        assertEquals(scenario.map(expected, "headers"), JSON.toJSON(actual.getHeaders()), scenario.label("request.headers"));
        assertEquals(scenario.map(expected, "queryParameters"), JSON.toJSON(actual.getQueryParams()), scenario.label("request.queryParameters"));
        assertEquals(scenario.map(expected, "formParameters"), JSON.toJSON(actual.getFormParams()), scenario.label("request.formParameters"));
        assertEquals(scenario.map(expected, "attributes"), JSON.toJSON(actual.getAttributes()), scenario.label("request.attributes"));
        assertEquals(scenario.body(expected, "body"), actual.getBody(), scenario.label("request.body"));
    }
</#if>
}
