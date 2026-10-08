/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].extension;

import com.alibaba.fastjson.JSONObject;
import [=packageName].model.ChannelHttpResult;

import java.util.Collections;
import java.util.Map;

/**
 * Customization points for a single channel API.
 *
 * <p>External developers do not need to extend framework classes. Each SPI method delegates
 * API-specific behavior to this interface. The template controls the sequence: field validation,
 * URL/body mapping, channel request signing/encryption, then channel invocation. The platform
 * verifies and decrypts non-notification requests before they enter the SPI; this extension
 * does not handle inbound platform security.</p>
 *
 * @param <REQUEST> platform-standard request type defined by common-sdk
 * @param <RESPONSE> platform-standard response type defined by common-sdk
 */
public interface ChannelApiExtension<REQUEST, RESPONSE> {

    /**
     * Validates standard fields already verified, decrypted, and converted by the platform.
     *
     * @param request platform-standard request
     */
    void validate(REQUEST request);

    /**
     * Maps platform-standard fields to the channel's JSON request body.
     *
     * @param request platform-standard request
     * @return mapped JSONObject, or null for a bodyless/raw-text protocol; transport or security
     *         customization may supply exact text through ChannelOutboundRequest.setRawBody
     */
    JSONObject mapRequestBody(REQUEST request);

    /**
     * Maps platform-standard fields to path placeholders for a dynamic URL.
     *
     * <p>For a platform-configured path such as {@code /payments/{paymentId}}, return
     * {@code {"paymentId": "P123"}}. Fixed-URL invocations do not call this method.</p>
     *
     * @param request platform-standard request
     * @return mapped path parameters; empty by default
     */
    default Map<String, String> mapUrlParameters(REQUEST request) {
        return Collections.emptyMap();
    }

    /**
     * Selects HTTP statuses whose bodies should enter response security processing and mapping.
     * Override only when the confirmed institution protocol defines business responses outside 2xx.
     * Accepting a status does not imply business success; mapResponse must map the actual outcome.
     *
     * @param statusCode institution HTTP status
     * @return true to verify/decrypt and map the response body; defaults to 2xx only
     */
    default boolean acceptResponseStatus(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Maps the channel response plaintext, after security processing, to a standard response.
     *
     * @param request original platform-standard request
     * @param response channel response after signature verification and decryption, as configured
     * @return platform-standard response
     */
    RESPONSE mapResponse(REQUEST request, ChannelHttpResult response);
}
