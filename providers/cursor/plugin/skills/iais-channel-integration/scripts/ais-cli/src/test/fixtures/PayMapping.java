/* SPDX-License-Identifier: Apache-2.0 */
package com.example.adapter.customize.api;

import com.alibaba.fastjson.JSONObject;
import com.alipay.iacqintegrationhub.channel.sdk.spi.payment.request.PayRequest;
import com.alipay.iacqintegrationhub.channel.sdk.spi.payment.response.PayResponse;
import com.example.adapter.extension.ChannelApiExtension;
import com.example.adapter.model.ChannelHttpResult;
import org.springframework.stereotype.Component;

/** Synthetic protocol used only by the CLI maintainer regression, never a channel implementation. */
@Component
public class PayMapping implements ChannelApiExtension<PayRequest, PayResponse> {
    @Override
    public void validate(PayRequest request) {
        if (request == null || request.getPaymentRequestId() == null) {
            throw new IllegalArgumentException("paymentRequestId required");
        }
    }

    @Override
    public JSONObject mapRequestBody(PayRequest request) {
        JSONObject body = new JSONObject(true);
        body.put("reference", request.getPaymentRequestId());
        return body;
    }

    @Override
    public PayResponse mapResponse(PayRequest request, ChannelHttpResult response) {
        if (!"accepted".equals(JSONObject.parseObject(response.getBody()).getString("state"))) {
            throw new IllegalStateException("Unexpected synthetic state");
        }
        return new PayResponse();
    }
}
