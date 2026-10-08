/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].extension;

import [=packageName].model.ChannelOutboundRequest;

/**
 * Customizes HTTP protocol details: method, headers, query, form, and content type.
 *
 * <p>This extension must not change the URL; the platform runtime controls the domain and path.</p>
 */
public interface ChannelTransport {

    /**
     * Configures non-URL transport parameters after field mapping and before security processing.
     *
     * @param request mutable outbound request to customize
     */
    void customize(ChannelOutboundRequest request);
}
