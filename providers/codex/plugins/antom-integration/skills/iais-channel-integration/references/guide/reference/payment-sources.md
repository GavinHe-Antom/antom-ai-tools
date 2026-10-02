# Payment field definitions and usage rules

This page covers field usage for payment, inquiry, capture, cancellation, and payment callbacks. Each method page provides complete request and response tables. Field names, types, and inheritance match the SDK.

## Field usage rules

- Required, optional, and conditional business fields are defined per method; do not reuse requirements across methods.
- External callers do not populate platform-provided fields. See each method table for the injection stage.
- Use Amount objects for amounts. Do not convert objects, booleans, or lists indiscriminately to strings.
- A required parent object does not make every child field required. Confirm unspecified child-field constraints before integration testing.
- The SDK does not automatically enforce all required fields, lengths, or String enum constraints. Implement the relevant validation in the Adapter.

## Key business constraints

| Scenario | Integration requirement |
| --- | --- |
| Payment query type | transactionType uses PAYMENT, CAPTURE, CANCEL, or REFUND; validate the institution-supported scope |
| Payment validity | paymentExpiryTime uses ISO 8601; the platform does not universally supply a validity period |
| User redirection | Return normalUrl when further user action is required; processing does not necessarily require redirection |
| Notification URL | Payment and capture use paymentNotifyUrl and captureNotifyUrl, respectively; no Dashboard fallback |
| Capture identity | captureId is required in synchronous capture responses and optional in notifications |
| Payment query validation | paymentId is optional; implement validate and path parameters in inquiryPayment's anonymous ChannelApiExtension according to institution query criteria |
| Notification direction | The SPI input is the institution message and its output is the standard notification; the platform forwards to iPay and handles ACK |
| ACS | The platform endpoint forwards directly to iPay and returns HTTP 302 without calling an adapter SPI; gateway DTOs belong to sdk.api.gateway |
| Online banking callback | The callback SPI has been removed from the SDK; there is no connected platform endpoint or generated implementation |

## API definitions

| SPI or platform flow | Business field definition |
| --- | --- |
| [pay](spi/pay.md) | alipay.ais.payments.pay |
| [inquiryPayment](spi/inquiryPayment.md) | alipay.ais.payments.inquiryPayment |
| [capture](spi/capture.md) | alipay.ais.payments.capture |
| [cancel](spi/cancel.md) | alipay.ais.payments.cancel |
| [Platform ACS callback](../07-notifications-and-callbacks.md#6-acs-browser-callback) | alipay.ais.payments.acsUrlCallback |
| [notifyPayment](spi/notifyPayment.md) | alipay.ais.payments.notifyPayment |
| [notifyCapture](spi/notifyCapture.md) | alipay.ais.payments.notifyCapture |

## Reading with an AI agent

First read [contracts.json](contracts.json) for types and fields, then use `method + input/output + field`
to read business constraints from [payment-field-notes.json](payment-field-notes.json). Source IDs and update timestamps locate the supporting definition; they do not enter messages.

authenticateAuthorize has its own input; do not directly reuse pay fields or required-field rules. See [Refund field definitions](refund-sources.md) for refund rules.
