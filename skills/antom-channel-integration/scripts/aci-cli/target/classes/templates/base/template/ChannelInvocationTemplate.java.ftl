/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].template;

import com.alibaba.fastjson.JSONObject;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpRequest;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.HttpResponse;
import com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService;
import com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseChannelRequest;
import [=packageName].extension.ChannelApiExtension;
import [=packageName].extension.ChannelMessageSecurity;
import [=packageName].extension.ChannelNotificationExtension;
import [=packageName].extension.ChannelTransport;
import [=packageName].extension.message.InboundChannelMessage;
import [=packageName].model.ChannelHttpResult;
import [=packageName].model.ChannelOperation;
import [=packageName].model.ChannelOutboundRequest;
import [=packageName].support.ChannelIntegrationException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * Standard channel adapter workflow, with JSON/2xx defaults and narrow protocol extensions.
 * Protocol-driven changes use setRawBody for exact text/bodyless calls and acceptResponseStatus
 * for non-2xx business responses. Preserve validation/mapping order, security failure boundaries and
 * platform-owned HTTP routing; do not add platform business calls to the adapter pipeline.
 *
 * <p>Every outbound call follows this sequence:</p>
 * <ol>
 *   <li>Validate standard fields already verified and decrypted by the platform.</li>
 *   <li>Map path parameters and an optional JSONObject request body.</li>
 *   <li>Configure non-URL HTTP properties.</li>
 *   <li>Sign or encrypt the outbound payload as required by the channel.</li>
 *   <li>Invoke the channel through the shared platform HTTP service.</li>
 *   <li>Verify or decrypt the response and map it to a platform-standard response.</li>
 * </ol>
 */
@Component
public class ChannelInvocationTemplate {

    /**
     * Channel security extension.
     */
    private final ChannelMessageSecurity security;

    /**
     * HTTP property extension that must not change the URL.
     */
    private final ChannelTransport transport;

    /**
     * Shared platform HTTP service.
     */
    private final PlatformChannelHttpService platformHttpService;

    public ChannelInvocationTemplate(ChannelMessageSecurity security,
                                     ChannelTransport transport,
                                     PlatformChannelHttpService platformHttpService) {
        this.security = security;
        this.transport = transport;
        this.platformHttpService = platformHttpService;
    }

    /**
     * Executes the standard workflow using a platform-configured fixed URL.
     */
    public <REQUEST extends BaseChannelRequest, RESPONSE> RESPONSE executeFixedUrl(
            ChannelOperation operation,
            REQUEST request,
            ChannelApiExtension<REQUEST, RESPONSE> extension) {
        // The platform already verified and decrypted non-notification requests; start with validation.
        extension.validate(request);
        JSONObject requestBody = extension.mapRequestBody(request);
        ChannelOutboundRequest outbound = prepareOutbound(
                operation, request, Collections.<String, String>emptyMap(), requestBody);
        HttpResponse httpResponse = platformHttpService.executeFixedUrl(request, toHttpRequest(outbound));
        ChannelHttpResult rawResponse = toChannelHttpResult(operation, httpResponse, extension);
        ChannelHttpResult plainResponse = unprotectChannelResponse(operation, request, rawResponse);
        return extension.mapResponse(request, plainResponse);
    }

    /**
     * Executes the standard workflow using a platform-configured dynamic path template.
     */
    public <REQUEST extends BaseChannelRequest, RESPONSE> RESPONSE executeDynamicUrl(
            ChannelOperation operation,
            REQUEST request,
            ChannelApiExtension<REQUEST, RESPONSE> extension) {
        // The platform already verified and decrypted non-notification requests; do not repeat it.
        extension.validate(request);
        JSONObject requestBody = extension.mapRequestBody(request);
        Map<String, String> urlParameters = extension.mapUrlParameters(request);
        ChannelOutboundRequest outbound = prepareOutbound(operation, request, urlParameters, requestBody);
        HttpResponse httpResponse = platformHttpService.executeDynamicUrl(
                request, urlParameters, toHttpRequest(outbound));
        ChannelHttpResult rawResponse = toChannelHttpResult(operation, httpResponse, extension);
        ChannelHttpResult plainResponse = unprotectChannelResponse(operation, request, rawResponse);
        return extension.mapResponse(request, plainResponse);
    }

    /**
     * Processes a channel notification: verify/decrypt, validate, then map fields.
     *
     * <p>The NotificationService SDK contract only converts the standard notification object;
     * the host performs subsequent platform business calls. This template neither generates
     * a channel ACK nor sends a separate HTTP request for the notification. Validation failures
     * stop processing before field mapping.</p>
     */
    public <RESPONSE> RESPONSE executeNotification(ChannelOperation operation,
                                                   BaseChannelRequest request,
                                                   ChannelNotificationExtension<RESPONSE> extension) {
        String plainBody = security.unprotectNotificationFromChannel(inbound(operation, request, null,
                request.getRequestHeaders(), request.getRawBody()));
        extension.validate(request, plainBody);
        // Return only the standard notification object; the platform signs/encrypts platform calls.
        return extension.map(request, plainBody);
    }

    /**
     * Applies transport customization and outbound security; the platform service still owns the URL.
     */
    private ChannelOutboundRequest prepareOutbound(ChannelOperation operation,
                                                   BaseChannelRequest request,
                                                   Map<String, String> urlParameters,
                                                   JSONObject requestBody) {
        ChannelOutboundRequest outbound = new ChannelOutboundRequest(
                operation, request, urlParameters, requestBody);
        transport.customize(outbound);
        security.protectRequestToChannel(outbound);
        return outbound;
    }

    /**
     * Converts adapter outbound properties to an SDK request; the host supplies the final URL.
     */
    private HttpRequest toHttpRequest(ChannelOutboundRequest outbound) {
        return HttpRequest.builder()
                .method(outbound.getMethod())
                .headers(outbound.getHeaders())
                .body(outbound.getSerializedBody())
                .contentType(outbound.getContentType())
                .queryParams(outbound.getQueryParameters())
                .formParams(outbound.getFormParameters())
                .attributes(outbound.getAttributes())
                .build();
    }

    /**
     * Validates the platform HTTP result and converts it to the adapter's response model.
     */
    private ChannelHttpResult toChannelHttpResult(ChannelOperation operation, HttpResponse response,
                                                 ChannelApiExtension<?, ?> extension) {
        if (response == null) {
            throw new ChannelIntegrationException("Empty HTTP response for " + operation);
        }
        // An institution-defined error body still goes through verification/decryption before mapping.
        if (!extension.acceptResponseStatus(response.getStatusCode())) {
            throw new ChannelIntegrationException("Channel HTTP request failed: operation="
                    + operation + ", status=" + response.getStatusCode());
        }
        return new ChannelHttpResult(response.getStatusCode(), response.getHeaders(), response.getBody());
    }

    /**
     * Applies verification or decryption to the raw channel response while preserving its original body.
     */
    private ChannelHttpResult unprotectChannelResponse(ChannelOperation operation,
                                                       BaseChannelRequest request,
                                                       ChannelHttpResult rawResponse) {
        String plainBody = security.unprotectResponseFromChannel(inbound(operation, request,
                rawResponse.getStatusCode(), rawResponse.getHeaders(), rawResponse.getRawBody()));
        return new ChannelHttpResult(rawResponse.getStatusCode(), rawResponse.getHeaders(),
                rawResponse.getRawBody(), plainBody);
    }

    /**
     * Creates an inbound security context with read-only fields and headers.
     */
    private InboundChannelMessage inbound(ChannelOperation operation,
                                          BaseChannelRequest request,
                                          Integer statusCode,
                                          Map<String, String> headers,
                                          String rawBody) {
        return new InboundChannelMessage(operation, request, statusCode, headers, rawBody);
    }

}
