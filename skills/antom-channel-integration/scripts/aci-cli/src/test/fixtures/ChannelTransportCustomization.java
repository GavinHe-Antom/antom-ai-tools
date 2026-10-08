/* SPDX-License-Identifier: Apache-2.0 */
package com.example.adapter.customize.transport;

import com.alibaba.fastjson.JSONObject;
import com.example.adapter.extension.ChannelTransport;
import com.example.adapter.model.ChannelOutboundRequest;
import org.springframework.stereotype.Component;

/** Synthetic exact-text and bodyless protocols used only in generator regression tests. */
@Component
public class ChannelTransportCustomization implements ChannelTransport {
    @Override
    public void customize(ChannelOutboundRequest request) {
        request.putHeader("Content-Type", "application/json; charset=utf-8");
        JSONObject body = request.getMappedBody();
        String reference = body.getString("reference");
        if ("synthetic-raw-order".equals(reference)) {
            request.setRawBody("reference=" + reference + "&amount=" + body.getString("amount")
                    + "&currency=" + body.getString("currency"));
            request.setContentType(null);
            request.putHeader("Content-Type", "text/plain; charset=utf-8");
        } else if ("synthetic-bodyless-order".equals(reference)) {
            request.setMethod("GET");
            request.setRawBody(null);
            request.setContentType(null);
            request.getQueryParameters().put("reference", reference);
        }
    }
}
