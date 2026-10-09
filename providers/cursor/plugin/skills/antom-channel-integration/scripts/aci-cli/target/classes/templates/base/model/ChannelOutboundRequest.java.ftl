/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].model;

import com.alibaba.fastjson.JSONObject;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.ContentType;
import com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseChannelRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured request prepared for delivery to the channel.
 *
 * <p>This object deliberately exposes no URL setter. The domain and path must come from
 * the platform-populated BaseChannelRequest. External adapter JARs may configure protocol
 * properties such as the method, headers, query, form, and body. The platform controls
 * connection settings, timeouts, and retry policies.</p>
 */
public final class ChannelOutboundRequest {

    /**
     * Current channel operation, used to select API-specific protocol rules.
     */
    private final ChannelOperation operation;

    /**
     * Platform-standard request and platform-injected routing context.
     */
    private final BaseChannelRequest platformRequest;

    /**
     * JSON snapshot after field mapping, used to construct signature input.
     */
    private final JSONObject mappedBody;

    /**
     * Mapped dynamic path parameters, available for signatures that include the path.
     */
    private final Map<String, String> pathParameters;

    /**
     * Final outbound JSON; security extensions may add signature fields or an encrypted envelope.
     */
    private JSONObject body;

    /** Exact protocol text, or null for a bodyless request, when explicitly selected. */
    private String rawBody;

    /** Distinguishes an explicitly absent body from the default mapped JSON body. */
    private boolean rawBodySelected;

    /**
     * HTTP method, initially set to the default defined by ChannelOperation.
     */
    private String method;

    /**
     * Final outbound HTTP headers.
     */
    private final Map<String, String> headers = new LinkedHashMap<>();

    /**
     * Final outbound query parameters.
     */
    private final Map<String, String> queryParameters = new LinkedHashMap<>();

    /**
     * Final outbound form parameters; usually empty for JSON APIs.
     */
    private final Map<String, String> formParameters = new LinkedHashMap<>();

    /**
     * Platform protocol attributes; must not contain URL, connection, timeout, or retry settings.
     */
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * Request content type; JSON by default.
     */
    private ContentType contentType = ContentType.JSON;

    /**
     * Creates an outbound request for a fixed URL.
     */
    public ChannelOutboundRequest(ChannelOperation operation, BaseChannelRequest platformRequest,
                                  JSONObject mappedBody) {
        this(operation, platformRequest, Collections.<String, String>emptyMap(), mappedBody);
    }

    /**
     * Creates an outbound request from mapped fields, before transport and security customization.
     */
    public ChannelOutboundRequest(ChannelOperation operation, BaseChannelRequest platformRequest,
                                  Map<String, String> pathParameters, JSONObject mappedBody) {
        this.operation = operation;
        this.platformRequest = platformRequest;
        this.pathParameters = pathParameters == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(pathParameters));
        this.mappedBody = copy(mappedBody);
        this.body = copy(mappedBody);
        this.method = operation.getDefaultHttpMethod();
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
     * Returns a shallow JSON copy of the mapped fields. Nested objects remain shared;
     * security extensions must not modify them in place.
     */
    public JSONObject getMappedBody() {
        return copy(mappedBody);
    }

    /**
     * Returns dynamic path parameters, or an empty map for a fixed URL.
     */
    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    /**
     * Returns the final request body; security extensions may directly add or remove JSON fields.
     */
    public JSONObject getBody() {
        return body;
    }

    /**
     * Replaces the final request body with a JSON envelope, typically for full-payload encryption.
     */
    public void setBody(JSONObject body) {
        this.body = body;
        this.rawBody = null;
        this.rawBodySelected = false;
    }

    /**
     * Selects an exact text payload without JSON reserialization, or an intentionally absent body.
     *
     * @param rawBody exact protocol text; null means no request body
     */
    public void setRawBody(String rawBody) {
        this.body = null;
        this.rawBody = rawBody;
        this.rawBodySelected = true;
    }

    /**
     * Returns the body text that the template supplies to platform HTTP.
     * Use this same representation for a header signature or body digest, and do not change
     * the payload after signing unless the institution protocol explicitly requires it.
     *
     * @return exact raw text, serialized JSON, or null for an absent body
     */
    public String getSerializedBody() {
        if (rawBodySelected) {
            return rawBody;
        }
        if (body == null) {
            return null;
        }
        return body.toJSONString();
    }

    /**
     * Returns the final HTTP method.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the final HTTP method.
     */
    public void setMethod(String method) {
        this.method = method;
    }

    /**
     * Returns the mutable header map.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Adds or replaces a header.
     */
    public void putHeader(String name, String value) {
        headers.put(name, value);
    }

    /**
     * Returns the mutable query parameter map.
     */
    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    /**
     * Returns the mutable form parameter map.
     */
    public Map<String, String> getFormParameters() {
        return formParameters;
    }

    /**
     * Returns mutable attributes for platform protocol extensions.
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Returns the request content type.
     */
    public ContentType getContentType() {
        return contentType;
    }

    /**
     * Sets the request content type.
     */
    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    /**
     * Copies only top-level fields. Extensions must explicitly copy nested structures before
     * modifying them; this is not a deep copy.
     */
    private JSONObject copy(JSONObject source) {
        if (source == null) {
            return null;
        }
        return new JSONObject(new LinkedHashMap<>(source));
    }
}
