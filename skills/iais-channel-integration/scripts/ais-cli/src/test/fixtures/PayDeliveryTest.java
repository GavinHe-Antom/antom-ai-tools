/* SPDX-License-Identifier: Apache-2.0 */
package com.example.adapter;

import com.alibaba.fastjson.JSONObject;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpResponse;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.PlatformChannelSecurityService;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecuritySignRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityVerifyRequest;
import com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext;
import com.alipay.iacqintegrationhub.channel.sdk.spi.payment.PaymentService;
import com.alipay.iacqintegrationhub.channel.sdk.spi.payment.request.PayRequest;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Synthetic real-SPI tests: field mapping, platform delegation and security failure boundaries. */
class PayDeliveryTest {
    @Test
    void mapsAndSignsRequestThenVerifiesResponse() {
        PlatformChannelHttpService http = mock(PlatformChannelHttpService.class);
        PlatformChannelSecurityService security = mock(PlatformChannelSecurityService.class);
        when(security.sign(any())).thenReturn("synthetic-signature");
        when(security.verify(any())).thenReturn(true);
        when(http.executeDynamicUrl(any(), anyMap(), any())).thenReturn(HttpResponse.builder()
                .statusCode(200).body("{\"state\":\"accepted\"}")
                .headers(Collections.singletonMap("Signature", "synthetic-response-signature")).build());
        ChannelRequestContext.bind("example", "synthetic-merchant", null);
        try (AnnotationConfigApplicationContext context = context(http, security)) {
            PayRequest request = request();
            assertNotNull(context.getBean(PaymentService.class).pay(request));
            ArgumentCaptor<HttpRequest> sent = ArgumentCaptor.forClass(HttpRequest.class);
            verify(http).executeDynamicUrl(same(request), eq(Collections.emptyMap()), sent.capture());
            assertEquals(JSONObject.parseObject("{\"reference\":\"synthetic-order\"}"), JSONObject.parseObject(sent.getValue().getBody()));
            assertEquals("POST", sent.getValue().getMethod());
            assertEquals("synthetic-signature", sent.getValue().getHeaders().get("Signature"));
            ArgumentCaptor<SecuritySignRequest> sign = ArgumentCaptor.forClass(SecuritySignRequest.class);
            verify(security).sign(sign.capture());
            assertEquals(sent.getValue().getBody(), sign.getValue().getContent());
            assertEquals("synthetic-merchant", sign.getValue().getMerchantId());
            ArgumentCaptor<SecurityVerifyRequest> check = ArgumentCaptor.forClass(SecurityVerifyRequest.class);
            verify(security).verify(check.capture());
            assertEquals("{\"state\":\"accepted\"}", check.getValue().getContent());
            assertEquals("synthetic-response-signature", check.getValue().getSignature());
        } finally {
            ChannelRequestContext.clear();
        }
    }

    @Test
    void signingFailureNeverCallsHttp() {
        PlatformChannelHttpService http = mock(PlatformChannelHttpService.class);
        PlatformChannelSecurityService security = mock(PlatformChannelSecurityService.class);
        when(security.sign(any())).thenThrow(new SecurityException("synthetic key unavailable"));
        ChannelRequestContext.bind("example", "synthetic-merchant", null);
        try (AnnotationConfigApplicationContext context = context(http, security)) {
            assertThrows(SecurityException.class, () -> context.getBean(PaymentService.class).pay(request()));
            verifyNoInteractions(http);
        } finally {
            ChannelRequestContext.clear();
        }
    }

    @Test
    void invalidSignatureStopsBeforeResponseMapping() {
        PlatformChannelHttpService http = mock(PlatformChannelHttpService.class);
        PlatformChannelSecurityService security = mock(PlatformChannelSecurityService.class);
        when(http.executeDynamicUrl(any(), anyMap(), any())).thenReturn(HttpResponse.builder()
                .statusCode(200).body("not-json-and-must-not-be-mapped").headers(Collections.emptyMap()).build());
        when(security.verify(any())).thenReturn(false);
        ChannelRequestContext.bind("example", "synthetic-merchant", null);
        try (AnnotationConfigApplicationContext context = context(http, security)) {
            assertThrows(SecurityException.class, () -> context.getBean(PaymentService.class).pay(request()));
        } finally {
            ChannelRequestContext.clear();
        }
    }

    private PayRequest request() {
        PayRequest request = new PayRequest();
        request.setPaymentRequestId("synthetic-order");
        return request;
    }

    private AnnotationConfigApplicationContext context(PlatformChannelHttpService http, PlatformChannelSecurityService security) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(PlatformChannelHttpService.class, () -> http);
        context.registerBean(PlatformChannelSecurityService.class, () -> security);
        context.scan("com.example.adapter");
        context.refresh();
        return context;
    }
}
