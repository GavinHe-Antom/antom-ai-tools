/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Channel HTTP response. Request bodies use JSON; after security processing, the API extension
 * parses the response body according to the channel's JSON structure.
 */
public class ChannelHttpResult {

    /**
     * HTTP status code.
     */
    private final int statusCode;

    /**
     * Response headers.
     */
    private final Map<String, String> headers;

    /**
     * Original response body received from the channel.
     */
    private final String rawBody;

    /**
     * Response plaintext after configured signature verification and decryption.
     */
    private final String body;

    /**
     * Creates a raw result before response security processing.
     */
    public ChannelHttpResult(int statusCode, Map<String, String> headers, String body) {
        this(statusCode, headers, body, body);
    }

    /**
     * Creates a result retaining both the original body and the plaintext after security processing.
     */
    public ChannelHttpResult(int statusCode, Map<String, String> headers, String rawBody, String body) {
        this.statusCode = statusCode;
        this.headers = headers == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.rawBody = rawBody;
        this.body = body;
    }

    /**
     * Returns the HTTP status code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the response headers.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Returns the original channel response for audit summaries or protocol diagnostics only.
     * Sensitive content must not be logged.
     */
    public String getRawBody() {
        return rawBody;
    }

    /**
     * Returns field-mapping input after the security extension's configured processing.
     */
    public String getBody() {
        return body;
    }
}
