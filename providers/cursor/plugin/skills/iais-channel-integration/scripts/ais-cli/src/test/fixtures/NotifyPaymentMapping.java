/* SPDX-License-Identifier: Apache-2.0 */
package com.example.adapter.customize.api;

import com.alibaba.fastjson.JSONObject;
import com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseChannelRequest;
import com.alipay.iacqintegrationhub.channel.sdk.spi.notify.request.PaymentNotifyRequest;
import com.example.adapter.extension.ChannelNotificationExtension;
import org.springframework.stereotype.Component;

/** Synthetic notification protocol used only by the CLI maintainer regression. */
@Component
public class NotifyPaymentMapping implements ChannelNotificationExtension<PaymentNotifyRequest> {
    @Override
    public void validate(BaseChannelRequest request, String plainBody) {
        JSONObject payload = JSONObject.parseObject(plainBody);
        if (payload == null || payload.getString("reference") == null || payload.getString("extension") == null) {
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
}
