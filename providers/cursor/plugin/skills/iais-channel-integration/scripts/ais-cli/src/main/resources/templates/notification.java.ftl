/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].customize.api;

import [=capability.requestType];
import [=capability.responseType];
import [=packageName].extension.ChannelNotificationExtension;
import org.springframework.stereotype.Component;

/** Map [=capability.method] after successful channel verification/decryption; no HTTP or ACK here. */
@Component
public class [=capability.title]Mapping implements ChannelNotificationExtension<[=capability.responseName]> {
    /** Validate authenticated plaintext and institution-required fields. */
    @Override
    public void validate(BaseChannelRequest request, String plainBody) {
        throw new UnsupportedOperationException("Implement [=capability.method] validation");
    }

    /** Return the platform notification request; the platform owns iPay forwarding and ACK. */
    @Override
    public [=capability.responseName] map(BaseChannelRequest request, String plainBody) {
        throw new UnsupportedOperationException("Implement [=capability.method] notification mapping");
    }
}
