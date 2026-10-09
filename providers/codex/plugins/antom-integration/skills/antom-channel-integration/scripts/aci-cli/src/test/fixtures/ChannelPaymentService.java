/* SPDX-License-Identifier: Apache-2.0 */
package com.example.adapter.spi;

import com.alibaba.fastjson.JSONObject;
import com.alipay.iacqintegrationhub.channel.sdk.api.error.ResultCodeService;
import com.alipay.iacqintegrationhub.channel.sdk.spi.payment.PaymentService;
import com.alipay.iacqintegrationhub.channel.sdk.spi.payment.request.PayRequest;
import com.alipay.iacqintegrationhub.channel.sdk.spi.payment.response.PayResponse;
import com.example.adapter.extension.ChannelApiExtension;
import com.example.adapter.model.ChannelHttpResult;
import com.example.adapter.model.ChannelOperation;
import com.example.adapter.template.ChannelInvocationTemplate;
import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Synthetic protocol used only by the CLI maintainer regression, never a channel implementation. */
@Component
public class ChannelPaymentService implements PaymentService {
    private final ChannelInvocationTemplate template;
    private final ResultCodeService resultCodes;

    public ChannelPaymentService(ChannelInvocationTemplate template, ResultCodeService resultCodes) {
        this.template = template;
        this.resultCodes = resultCodes;
    }

    @Override
    public PayResponse pay(PayRequest request) {
        return template.executeDynamicUrl(ChannelOperation.PAY, request,
                new ChannelApiExtension<PayRequest, PayResponse>() {
                    @Override
                    public void validate(PayRequest request) {
                        if (request == null || request.getPaymentRequestId() == null) {
                            throw new IllegalArgumentException("paymentRequestId required");
                        }
                        if (request.getPaymentAmount() == null || request.getPaymentAmount().getValue() == null) {
                            throw new IllegalArgumentException("paymentAmount required");
                        }
                    }

                    @Override
                    public JSONObject mapRequestBody(PayRequest request) {
                        JSONObject body = new JSONObject(true);
                        body.put("reference", request.getPaymentRequestId());
                        body.put("amount", request.getPaymentAmount().getValue());
                        body.put("currency", request.getPaymentAmount().getCurrency());
                        return body;
                    }

                    @Override
                    public Map<String, String> mapUrlParameters(PayRequest request) {
                        return Collections.emptyMap();
                    }

                    @Override
                    public boolean acceptResponseStatus(int statusCode) {
                        return statusCode == 200 || statusCode == 422;
                    }

                    @Override
                    public PayResponse mapResponse(PayRequest request, ChannelHttpResult response) {
                        JSONObject payload = JSONObject.parseObject(response.getBody());
                        String state = payload.getString("state");
                        if (!"accepted".equals(state) && !"declined".equals(state) && !"processing".equals(state)) {
                            throw new IllegalStateException("Unexpected synthetic state");
                        }
                        PayResponse result = new PayResponse();
                        result.setResult(resultCodes.mapping(request.getChannelCode(), payload.getString("code"),
                                payload.getString("message"), "pay"));
                        result.setPaymentId(payload.getString("payment_id"));
                        result.setPaymentAmount(request.getPaymentAmount());
                        result.setExtendInfo(state);
                        return result;
                    }
                });
    }
}
