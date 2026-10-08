/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName];

import com.alipay.iacqintegrationhub.channel.sdk.api.error.ResultCodeService;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.PlatformChannelSecurityService;
import com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext;
import com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseChannelRequest;
import com.alipay.iacqintegrationhub.channel.sdk.spi.notify.request.PaymentNotifyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/** Wiring-only tests: passing these does not establish institution protocol correctness. */
class GeneratedStructureTest {
    @Test
    void currentSdkFieldsAndContextAreAvailable() {
        BaseChannelRequest request = new BaseChannelRequest();
        request.setRuntimeEnv("synthetic-env");
        assertEquals("synthetic-env", request.getRuntimeEnv());
        assertEquals("synthetic-builder-env", BaseChannelRequest.builder()
                .runtimeEnv("synthetic-builder-env").build().getRuntimeEnv());
        PaymentNotifyRequest notification = PaymentNotifyRequest.builder()
                .paymentRequestId("synthetic-payment").extendInfo("synthetic-extension").build();
        assertEquals("synthetic-extension", notification.getExtendInfo());
        notification.setExtendInfo("synthetic-updated-extension");
        assertEquals("synthetic-updated-extension", notification.getExtendInfo());
        ChannelRequestContext.bind("[=channelCode]", "synthetic-merchant", null);
        try {
            assertEquals("[=channelCode]", ChannelRequestContext.current().getChannelCode());
            assertEquals("synthetic-merchant", ChannelRequestContext.current().getMerchantId());
            assertNull(ChannelRequestContext.current().getRuntimeEnv());
        } finally {
            ChannelRequestContext.clear();
        }
        assertThrows(IllegalStateException.class, ChannelRequestContext::current);
    }

    @Test
    void selectedSpiBeansAndMethodsAreWired() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(PlatformChannelHttpService.class, () -> mock(PlatformChannelHttpService.class));
            context.registerBean(PlatformChannelSecurityService.class, () -> mock(PlatformChannelSecurityService.class));
            context.registerBean(ResultCodeService.class, () -> mock(ResultCodeService.class));
            context.scan("[=packageName]");
            context.refresh();
<#list ["payment", "refund", "notify"] as family>
<#assign methods=[]><#list capabilities as cap><#if cap.family == family><#assign methods=methods+[cap]></#if></#list>
<#assign service="PaymentService"><#if family == "refund"><#assign service="RefundService"></#if><#if family == "notify"><#assign service="NotificationService"></#if>
<#if methods?size gt 0>
            assertEquals(1, context.getBeansOfType(com.alipay.iacqintegrationhub.channel.sdk.spi.[=family].[=service].class).size());
            Object [=family] = context.getBean(com.alipay.iacqintegrationhub.channel.sdk.spi.[=family].[=service].class);
            assertSelectedSpiMethods(com.alipay.iacqintegrationhub.channel.sdk.spi.[=family].[=service].class,
                    [=family], new HashSet<>(Arrays.asList(<#list methods as method>
                            "[=method.method]([=method.requestType]):[=method.responseType]"<#sep>,</#list>)));
<#else>
            assertEquals(0, context.getBeansOfType(com.alipay.iacqintegrationhub.channel.sdk.spi.[=family].[=service].class).size());
</#if>
</#list>
        }
    }

    /** Check SPI overrides, not constructors or protocol implementation helpers. */
    private static void assertSelectedSpiMethods(Class<?> spi, Object bean, Set<String> expected) {
        Set<String> spiNames = new HashSet<>();
        Arrays.stream(spi.getMethods()).forEach(method -> spiNames.add(method.getName()));
        Set<String> actual = new HashSet<>();
        for (Class<?> type = bean.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
                        && !method.isBridge() && !method.isSynthetic() && spiNames.contains(method.getName())) {
                    actual.add(signature(method));
                }
            }
        }
        assertEquals(expected, actual, "Missing, extra or incorrectly typed SPI overrides");
    }

    private static String signature(Method method) {
        StringBuilder signature = new StringBuilder(method.getName()).append('(');
        Class<?>[] parameters = method.getParameterTypes();
        for (int index = 0; index < parameters.length; index++) {
            if (index > 0) {
                signature.append(',');
            }
            signature.append(parameters[index].getName());
        }
        return signature.append("):").append(method.getReturnType().getName()).toString();
    }
}
