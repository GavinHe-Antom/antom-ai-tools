/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].customize.security;

<#if activeSecurity>
import com.alipay.iacqintegrationhub.channel.sdk.api.security.PlatformChannelSecurityService;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecuritySignRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityVerifyRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityEncryptRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityDecryptRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityKeyRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityKeyMaterial;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityKeyPurpose;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityKeyAlgorithm;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecuritySignAlgorithm;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityCipherAlgorithm;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityCipherType;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityCipherParameters;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityCipherMode;
import com.alipay.iacqintegrationhub.channel.sdk.api.security.SecurityCipherPadding;
import com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext;
</#if>
import [=packageName].extension.ChannelMessageSecurity;
import [=packageName].extension.message.InboundChannelMessage;
import [=packageName].model.ChannelOutboundRequest;
import org.springframework.stereotype.Component;

/** Protocol-specific canonical text and field placement; platform APIs own key lookup and standard computation. */
@Component
public class ChannelSecurityCustomization implements ChannelMessageSecurity {
<#if activeSecurity>
    private final PlatformChannelSecurityService platform;

    /** Inject the channel-scoped platform security service, not an IBCM client. */
    public ChannelSecurityCustomization(PlatformChannelSecurityService platform) {
        this.platform = platform;
    }
</#if>
<#list ["request", "response", "notification"] as direction>
    /** Apply the explicitly selected protocol steps in the confirmed order. */
    @Override
<#if direction == "request">
    public void protectRequestToChannel(ChannelOutboundRequest message) {
<#else>
    public String unprotect[=(direction == "response")?then("Response", "Notification")]FromChannel(InboundChannelMessage message) {
        String plainBody = message.getRawBody();
</#if>
        switch (message.getOperation()) {
<#list capabilities as cap><#if cap.directions?seq_contains(direction)>
            case [=cap.operation]:
<#list security[cap.method][direction] as step>
<#assign id=cap.title + direction?cap_first + step.operation?cap_first>
<#if step.implementation == "none">
                // [=step.operation]: none; protocol reference: [=step.rule]
<#elseif step.implementation == "adapter">
<#if direction == "request">
                apply[=id](message, platform.queryKey(key[=id](message)));
<#else>
                plainBody = apply[=id](message, plainBody, platform.queryKey(key[=id](message, plainBody)));
</#if>
<#elseif step.operation == "sign" || step.operation == "encrypt">
                apply[=id](message, platform.[=step.operation](request[=id](message)));
<#elseif step.operation == "verify">
                if (!platform.verify(request[=id](message, plainBody))) {
                    throw new SecurityException("[=cap.method] [=direction] signature verification failed");
                }
<#else>
                plainBody = platform.decrypt(request[=id](message, plainBody));
</#if>
</#list>
<#if direction == "request">
                return;
<#else>
                return plainBody;
</#if>
</#if></#list>
            default:
                throw new UnsupportedOperationException("Unselected security operation: " + message.getOperation());
        }
    }
</#list>
<#list capabilities as cap><#list cap.directions as direction><#list security[cap.method][direction] as step><#if step.implementation != "none">
<#assign id=cap.title + direction?cap_first + step.operation?cap_first>
<#assign context="InboundChannelMessage message, String plainBody">
<#assign args="message, plainBody">
<#if direction == "request"><#assign context="ChannelOutboundRequest message"><#assign args="message"></#if>
<#if step.implementation == "adapter">
    /** The platform owns identity; operation and storage algorithm come from confirmed configuration. */
    private SecurityKeyRequest key[=id]([=context]) {
        String merchantId = ChannelRequestContext.current().getMerchantId();
        SecurityKeyRequest request = new SecurityKeyRequest();
        request.setMerchantId(merchantId);
        request.setPurpose(SecurityKeyPurpose.[=step.operation?upper_case]);
        request.setKeyAlgorithm(SecurityKeyAlgorithm.[=step.keyAlgorithm]);
        return request;
    }

    /** Rule: [=step.rule]. Use a mature library; never log/store key material; verification failure must throw. */
<#if direction == "request">
    protected void apply[=id]([=context], SecurityKeyMaterial key) {
<#else>
    protected String apply[=id]([=context], SecurityKeyMaterial key) {
</#if>
        throw new UnsupportedOperationException("Implement [=id] custom calculation and wire formatting");
    }
<#else>
<#assign requestType="Security"+step.operation?cap_first+"Request">
    /** Resolve trusted identity before any hook; hooks only supply protocol text and runtime parameters. */
    private [=requestType] request[=id]([=context]) {
        String merchantId = ChannelRequestContext.current().getMerchantId();
        [=requestType] request = build[=id]([=args]);
        request.setMerchantId(merchantId);
<#if step.operation == "sign" || step.operation == "verify">
        request.setAlgorithm(SecuritySignAlgorithm.[=step.algorithm]);
<#else>
        request.setAlgorithm(SecurityCipherAlgorithm.[=step.algorithm]);
        request.setCipherType(SecurityCipherType.[=step.cipherType]);
<#if step.parameters?size gt 0>
        SecurityCipherParameters parameters = request.getParameters();
        if (parameters == null) {
            parameters = new SecurityCipherParameters();
            request.setParameters(parameters);
        }
        parameters.setMode(SecurityCipherMode.[=step.parameters.mode]);
        parameters.setPadding(SecurityCipherPadding.[=step.parameters.padding]);
<#if step.parameters.tagBitLength??>
        parameters.setTagBitLength([=step.parameters.tagBitLength?c]);
</#if>
</#if>
</#if>
        return request;
    }

    /** Rule: [=step.rule]. Supply exact payload/signature and required runtime IV/AAD/PGP options, not identity. */
    protected [=requestType] build[=id]([=context]) {
        // TODO: Build typed SDK request. The wrapper sets merchantId from context; never reuse a fixed nonce.
        throw new UnsupportedOperationException("Implement [=id] canonical text and runtime parameters");
    }
<#if direction == "request">

    /** Place the result in the channel-defined header/body field and encoding; do not alter signed bytes afterward. */
    protected void apply[=id](ChannelOutboundRequest message, String result) {
        throw new UnsupportedOperationException("Implement [=id] result placement");
    }
</#if>
</#if>
</#if></#list></#list></#list>
}
