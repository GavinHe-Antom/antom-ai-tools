/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].extension.message;

import com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseChannelRequest;
import [=packageName].model.ChannelOperation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Original inbound message for a synchronous response or asynchronous notification.
 * Security extensions must always verify signatures against rawBody.
 */
public final class InboundChannelMessage {

    /**
     * Current API operation, used to select security rules.
     */
    private final ChannelOperation operation;

    /**
     * Platform-standard request and routing context.
     */
    private final BaseChannelRequest platformRequest;

    /**
     * HTTP status code for a synchronous response; null for inbound requests and notifications.
     */
    private final Integer statusCode;

    /**
     * Read-only copy of the original HTTP headers.
     */
    private final Map<String, String> headers;

    /**
     * Original wire payload, without deserialization or modification.
     */
    private final String rawBody;

    /**
     * Creates an inbound context with read-only fields and headers for security extensions.
     */
    public InboundChannelMessage(ChannelOperation operation, BaseChannelRequest platformRequest,
                                 Integer statusCode, Map<String, String> headers, String rawBody) {
        this.operation = operation;
        this.platformRequest = platformRequest;
        this.statusCode = statusCode;
        this.headers = headers == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.rawBody = rawBody;
    }

    /**
     * Returns the current channel operation.
     */
    public ChannelOperation getOperation() {
        return operation;
    }

    /**
     * Returns the platform-standard request and routing context.
     */
    public BaseChannelRequest getPlatformRequest() {
        return platformRequest;
    }

    /**
     * Returns the HTTP status code, or null for an inbound request or notification.
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * Returns a read-only map of the original headers.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Returns the unmodified original wire payload.
     */
    public String getRawBody() {
        return rawBody;
    }

    /**
     * Looks up a header name case-insensitively, following HTTP semantics.
     */
    public String getHeader(String name) {
        if (name == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (name.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
