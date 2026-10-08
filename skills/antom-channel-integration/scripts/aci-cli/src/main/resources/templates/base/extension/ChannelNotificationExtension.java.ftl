/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].extension;

import com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseChannelRequest;

/**
 * Customization points for channel notifications.
 *
 * <p>This extension handles only plaintext fields from channel notifications. The platform
 * signs and encrypts platform service calls after mapping, so this interface does not expose
 * outbound security methods for calls to the platform.</p>
 *
 * @param <RESPONSE> platform-standard notification request type defined by common-sdk
 */
public interface ChannelNotificationExtension<RESPONSE> {

    /**
     * Validates channel notification plaintext after signature verification and decryption.
     *
     * @param request platform request and routing context
     * @param plainBody notification plaintext after configured security processing
     */
    void validate(BaseChannelRequest request, String plainBody);

    /**
     * Maps validated channel notification plaintext to a platform-standard notification request.
     *
     * @param request platform request and routing context
     * @param plainBody validated notification plaintext
     * @return platform-standard notification request
     */
    RESPONSE map(BaseChannelRequest request, String plainBody);
}
