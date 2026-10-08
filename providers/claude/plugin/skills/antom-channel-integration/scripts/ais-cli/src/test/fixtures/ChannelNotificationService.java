/* SPDX-License-Identifier: Apache-2.0 */
package com.example.adapter.spi;

import com.alibaba.fastjson.JSONObject;
import com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseChannelRequest;
import com.alipay.iacqintegrationhub.channel.sdk.spi.notify.NotificationService;
import com.alipay.iacqintegrationhub.channel.sdk.spi.notify.request.PaymentNotifyRequest;
import com.example.adapter.extension.ChannelNotificationExtension;
import com.example.adapter.model.ChannelOperation;
import com.example.adapter.template.ChannelInvocationTemplate;
import org.springframework.stereotype.Component;

/** Synthetic notification protocol used only by the CLI maintainer regression. */
@Component
public class ChannelNotificationService implements NotificationService {
    private final ChannelInvocationTemplate template;

    public ChannelNotificationService(ChannelInvocationTemplate template) {
        this.template = template;
    }

    @Override
    public PaymentNotifyRequest notifyPayment(BaseChannelRequest request) {
        return template.executeNotification(ChannelOperation.PAYMENT_NOTIFICATION, request,
                new ChannelNotificationExtension<PaymentNotifyRequest>() {
                    @Override
                    public void validate(BaseChannelRequest request, String plainBody) {
                        JSONObject payload = JSONObject.parseObject(plainBody);
                        if (payload == null || payload.getString("reference") == null
                                || payload.getString("extension") == null) {
                            throw new IllegalArgumentException("Synthetic notification requires reference and extension");
                        }
                    }

                    @Override
                    public PaymentNotifyRequest map(BaseChannelRequest request, String plainBody) {
                        JSONObject payload = JSONObject.parseObject(plainBody);
                        return PaymentNotifyRequest.builder()
                                .paymentRequestId(payload.getString("reference"))
                                .extendInfo(payload.getString("extension"))
                                .build();
                    }
                });
    }
}
