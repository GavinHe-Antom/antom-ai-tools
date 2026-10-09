/* SPDX-License-Identifier: Apache-2.0 */
package com.example.adapter.customize.security;

import com.alipay.iacqintegrationhub.channel.sdk.api.security.PlatformChannelSecurityService;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecuritySignAlgorithm;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecuritySignRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityVerifyRequest;
import com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext;
import com.example.adapter.extension.ChannelMessageSecurity;
import com.example.adapter.extension.message.InboundChannelMessage;
import com.example.adapter.model.ChannelOutboundRequest;
import org.springframework.stereotype.Component;

/** Synthetic delegation protocol. The test host supplies identity through the SDK context. */
@Component
public class ChannelSecurityCustomization implements ChannelMessageSecurity {
    private final PlatformChannelSecurityService platform;

    public ChannelSecurityCustomization(PlatformChannelSecurityService platform) {
        this.platform = platform;
    }

    @Override
    public void protectRequestToChannel(ChannelOutboundRequest message) {
        String merchantId = ChannelRequestContext.current().getMerchantId();
        SecuritySignRequest request = buildPayRequestSign(message);
        request.setMerchantId(merchantId);
        request.setAlgorithm(SecuritySignAlgorithm.HMAC_SHA256_BASE64);
        applyPayRequestSign(message, platform.sign(request));
    }

    protected SecuritySignRequest buildPayRequestSign(ChannelOutboundRequest message) {
        SecuritySignRequest request = new SecuritySignRequest();
        String content = message.getSerializedBody();
        if (content == null) {
            content = "GET\n" + message.getMappedBody().getString("reference");
        }
        request.setContent(content);
        return request;
    }

    protected void applyPayRequestSign(ChannelOutboundRequest message, String signature) {
        message.putHeader("Signature", signature);
    }

    @Override
    public String unprotectResponseFromChannel(InboundChannelMessage message) {
        String merchantId = ChannelRequestContext.current().getMerchantId();
        SecurityVerifyRequest request = buildPayResponseVerify(message, message.getRawBody());
        request.setMerchantId(merchantId);
        request.setAlgorithm(SecuritySignAlgorithm.HMAC_SHA256_BASE64);
        if (!platform.verify(request)) {
            throw new SecurityException("Synthetic response verification failed");
        }
        return message.getRawBody();
    }

    protected SecurityVerifyRequest buildPayResponseVerify(InboundChannelMessage message, String plainBody) {
        SecurityVerifyRequest request = new SecurityVerifyRequest();
        request.setContent(plainBody);
        request.setSignature(message.getHeader("Signature"));
        return request;
    }

    @Override
    public String unprotectNotificationFromChannel(InboundChannelMessage message) {
        // The independent synthetic notification protocol explicitly selects no security steps.
        return message.getRawBody();
    }
}
