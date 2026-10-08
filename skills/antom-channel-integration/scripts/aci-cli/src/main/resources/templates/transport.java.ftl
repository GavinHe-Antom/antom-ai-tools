/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].customize.transport;

import [=packageName].extension.ChannelTransport;
import [=packageName].model.ChannelOutboundRequest;
import org.springframework.stereotype.Component;

/** Configure method, headers, query and content type; URLs and connection settings remain platform-owned. */
@Component
public class ChannelTransportCustomization implements ChannelTransport {
    /** Adjust each operation according to the confirmed institution protocol. */
    @Override
    public void customize(ChannelOutboundRequest request) {
        // TODO: Defaults are JSON and POST. Select GET explicitly for inquiry protocols that require it.
        request.putHeader("Content-Type", "application/json; charset=utf-8");
    }
}
