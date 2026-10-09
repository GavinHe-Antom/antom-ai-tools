/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import java.util.Arrays;
import java.util.List;

/** Platform-connected SPI methods for template 0.1.0 and SDK 1.5.2. */
public enum Capability {
    PAY("pay", "payment", "Pay", "PAY"),
    AUTHORIZE("authenticateAuthorize", "payment", "AuthenticateAuthorize", "AUTHORIZE"),
    CANCEL("cancel", "payment", "Cancel", "CANCEL"),
    CAPTURE("capture", "payment", "Capture", "CAPTURE"),
    INQUIRY_PAYMENT("inquiryPayment", "payment", "InquiryPayment", "INQUIRY_PAYMENT"),
    REFUND("refund", "refund", "Refund", "REFUND"),
    INQUIRY_REFUND("inquiryRefund", "refund", "InquiryRefund", "INQUIRY_REFUND"),
    NOTIFY_PAYMENT("notifyPayment", "notify", "PaymentNotify", "PAYMENT_NOTIFICATION"),
    NOTIFY_CAPTURE("notifyCapture", "notify", "CaptureNotify", "CAPTURE_NOTIFICATION"),
    NOTIFY_REFUND("notifyRefund", "notify", "RefundNotify", "REFUND_NOTIFICATION");

    private final String method;
    private final String family;
    private final String model;
    private final String operation;

    Capability(String method, String family, String model, String operation) {
        this.method = method;
        this.family = family;
        this.model = model;
        this.operation = operation;
    }

    /** @return exact SDK method name */
    public String getMethod() { return method; }
    /** @return SDK interface package segment */
    public String getFamily() { return family; }
    /** @return invocation-template operation constant */
    public String getOperation() { return operation; }
    /** @return class-name prefix for this method's mapping and test */
    public String getTitle() { return Character.toUpperCase(method.charAt(0)) + method.substring(1); }
    /** @return whether this method maps a notification instead of issuing HTTP */
    public boolean isNotification() { return "notify".equals(family); }

    /** @return fully qualified standard input type */
    public String getRequestType() {
        if (isNotification()) {
            return "com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseChannelRequest";
        }
        return "com.alipay.iacqintegrationhub.channel.sdk.spi." + family + ".request." + model + "Request";
    }

    /** @return fully qualified response or forwarded-notification type */
    public String getResponseType() {
        if (isNotification()) {
            return "com.alipay.iacqintegrationhub.channel.sdk.spi.notify.request." + model + "Request";
        }
        return "com.alipay.iacqintegrationhub.channel.sdk.spi." + family + ".response." + model + "Response";
    }

    /** @return simple request class name for generated Java imports */
    public String getRequestName() { return getRequestType().substring(getRequestType().lastIndexOf('.') + 1); }
    /** @return simple result class name for generated Java imports */
    public String getResponseName() { return getResponseType().substring(getResponseType().lastIndexOf('.') + 1); }

    /** @return security directions applicable to this SPI method */
    public List<String> getDirections() {
        if (isNotification()) {
            return Arrays.asList("notification");
        }
        return Arrays.asList("request", "response");
    }

    static Capability find(String method) {
        for (Capability capability : values()) {
            if (capability.method.equals(method)) {
                return capability;
            }
        }
        throw new GenerationException(2, "Unsupported or unconnected SPI method: " + method);
    }
}
