/* SPDX-License-Identifier: Apache-2.0 */
package [=packageName].model;

/**
 * Channel operations supported by the adapter. Outbound calls default to POST;
 * ChannelTransportCustomization may override the method for channel-specific requirements.
 */
public enum ChannelOperation {
    PAY("pay", "POST"),
    AUTHORIZE("authorize", "POST"),
    CANCEL("cancel", "POST"),
    CAPTURE("capture", "POST"),
    INQUIRY_PAYMENT("inquiryPayment", "POST"),
    REFUND("refund", "POST"),
    INQUIRY_REFUND("inquiryRefund", "POST"),
    PAYMENT_NOTIFICATION("paymentNotify", null),
    ONLINE_BANK_PAYMENT_NOTIFICATION("onlineBankPaymentNotify", null),
    RECEIVE_PAYMENT_NOTIFICATION("receivePaymentNotify", null),
    CAPTURE_NOTIFICATION("captureNotify", null),
    REFUND_NOTIFICATION("refundNotify", null),
    ACS_URL_CALLBACK("acsUrlCallback", null),
    ONLINE_BANK_URL_CALLBACK("onlineBankUrlCallback", null);

    /**
     * Stable API name used by ResultCodeService and log summaries.
     */
    private final String apiName;

    /**
     * Default HTTP method for outbound calls; null for notifications with no downstream call.
     */
    private final String defaultHttpMethod;

    ChannelOperation(String apiName, String defaultHttpMethod) {
        this.apiName = apiName;
        this.defaultHttpMethod = defaultHttpMethod;
    }

    /**
     * Returns the API name used for platform result-code mapping.
     */
    public String getApiName() {
        return apiName;
    }

    /**
     * Returns the default HTTP method, which the restricted transport extension may override.
     */
    public String getDefaultHttpMethod() {
        return defaultHttpMethod;
    }
}
