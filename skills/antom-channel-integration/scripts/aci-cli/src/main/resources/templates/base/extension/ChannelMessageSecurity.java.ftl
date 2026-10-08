/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].extension;

import [=packageName].extension.message.InboundChannelMessage;
import [=packageName].model.ChannelOutboundRequest;

/**
 * Extension points for channel message security.
 *
 * <p>The template controls the invocation order. This interface handles message security only
 * between the adapter and the channel. The platform verifies and decrypts requests before they
 * enter a non-notification SPI; adapters must not repeat that processing. The platform also
 * signs and encrypts platform calls made after notification mapping.</p>
 *
 * <p>Implementations must not log keys or complete sensitive messages. Signature verification
 * or decryption failures must throw an exception rather than return unverified content.</p>
 */
public interface ChannelMessageSecurity {

    /**
     * Encrypts or signs the outbound JSON object, or adds channel authentication information.
     *
     * @param request mutable outbound request prepared for the channel
     */
    void protectRequestToChannel(ChannelOutboundRequest request);

    /**
     * Verifies and decrypts a synchronous channel response as required by the channel protocol.
     *
     * @param message original inbound response and its context
     * @return plaintext for field mapping, or {@link InboundChannelMessage#getRawBody()}
     *         when no security processing is required
     */
    String unprotectResponseFromChannel(InboundChannelMessage message);

    /**
     * Verifies and decrypts a channel notification as required by the channel protocol.
     *
     * @param message original inbound notification and its context
     * @return plaintext for field mapping, or {@link InboundChannelMessage#getRawBody()}
     *         when no security processing is required
     */
    String unprotectNotificationFromChannel(InboundChannelMessage message);
}
