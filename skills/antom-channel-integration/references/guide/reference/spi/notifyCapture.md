# notifyCapture — Capture result notification

[SPI index](README.md) · [Integration guide](../../README.md)

## Contract and platform availability

- Interface: `com.alipay.iacqintegrationhub.channel.sdk.spi.notify.NotificationService`
- Signature: `CaptureNotifyRequest notifyCapture(BaseChannelRequest request)`
- Java requirement: Optional; the SDK throws UnsupportedOperationException unless overridden.
- Platform capability: `ChannelSpiOperation.CAPTURE_NOTIFY`
- Platform endpoint: `POST /{channelCode}/channel/notifyCapture/{merchantId}`; requires channel-module registration and an SPI bean.
- Institution route key: This SPI does not select an institution destination route.

Notify the capture result.

This represents a capture result; do not substitute paymentRequestId/paymentId for captureRequestId/captureId. If supporting this notification without notifyPayment,
first verify the abstract-method requirements of NotificationService.

## Request

The tables include inherited fields, requirements, formats, and business meanings. A business-required field is not necessarily validated automatically by the SDK. Confirm unspecified constraints before integration testing.

Type: `com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseChannelRequest`. Inherited fields are included below; this does not mean that external HTTP
callers must supply every platform field.

| Field | Java type / nested definition | Source | Requirements / format | Business meaning |
| --- | --- | --- | --- | --- |
| `requestHeaders` | `Map<String, String>` | BaseChannelRequest | Provided by the platform; present | Inbound headers collected by the platform, available for protocol mapping. They are not necessarily the headers ultimately sent downstream. |
| `channelCode` | `String` | BaseChannelRequest | Provided by the platform; present | Channel identity declared by the platform module. The Adapter must not select or override it from the message. |
| `runtimeEnv` | `String` | BaseChannelRequest | Provided by the platform; may be absent | Platform runtime environment (source examples: test, dev, pre, gray, prod; not an enum constraint). Absence must not imply a test environment. This field does not select shadow/sandbox routes; see [Request context](../context.md). |
| `rawBody` | `String` | BaseChannelRequest | Provided by the platform; present | Message text received by the platform, used for parsing and signature verification; not the original byte stream. |
| `domain` | `String` | BaseChannelRequest | Not applicable | Receiving a notification does not call the channel destination. The platform leaves this field unset; it cannot provide the notification receiver URL. |
| `path` | `String` | BaseChannelRequest | Not applicable | Receiving a notification does not call the channel destination. The platform leaves this field unset; it cannot provide the notification receiver URL. |

**Validation:** Platform fields are supplied at the invocation stages listed in the table. The Adapter must map and validate business fields; missing validation annotations do not make a field optional.

## Response

Type: `com.alipay.iacqintegrationhub.channel.sdk.spi.notify.request.CaptureNotifyRequest`. This is the request returned to the platform for forwarding to iPay,
not the Controller's final response.

| Field | Java type / nested definition | Source | Requirements / format | Business meaning |
| --- | --- | --- | --- | --- |
| `requestHeaders` | `Map<String, String>` | BaseIpayRequest | Preserved by the Adapter from the notification input. | Copy requestHeaders from the notification input into the return object. The platform subsequently builds the headers sent to iPay. |
| `rawBody` | `String` | BaseIpayRequest | Preserved by the Adapter from the notification input. | Copy rawBody from the notification input into the return object to preserve the original institution message. It is not the serialized request body sent to iPay. |
| `domain` | `String` | BaseIpayRequest | Populated later by the platform | The platform fills the base URL before making the iPay HTTP call. It may be null when SPI returns; the Adapter need not set it. |
| `result` | `Result` → [Result](../models/Result.md) | CaptureNotifyRequest | Required; Result | Standard result of the business call or notification, including status, code, and message; not an HTTP status.<br>The platform result-code mapping configuration determines the specific code. |
| `captureRequestId` | `String` | CaptureNotifyRequest | Required; String(64) | Unique capture request ID used for capture idempotency.<br>Repeated requests with the same ID after an S/F final result must return consistent results. The scaffold does not provide idempotency storage. |
| `captureId` | `String` | CaptureNotifyRequest | Optional; String(64) | Unique capture acceptance ID generated after the institution accepts the capture.<br>Not the original paymentId. |
| `captureAmount` | `Amount` → [Amount](../models/Amount.md) | CaptureNotifyRequest | Required; Amount | Amount associated with this capture result. |
| `urlParam` | `String` | CaptureNotifyRequest | Required status and length are unspecified | URL parameter. |

## Usage constraints

- The input is the institution notification; the output CaptureNotifyRequest is the standard capture notification. Do not confuse their field structures.
- The platform handles the notification ACK based on the actual iPay response. Confirm the institution-required ACK format during integration.

API definition: alipay.ais.payments.notifyCapture. See [Payment field definitions](../payment-sources.md)
and [Machine-readable field notes](../payment-field-notes.json).

## Implementation and verification

The CLI connects executeNotification through `spi/ChannelNotificationService.notifyCapture`. Implement validate and map in that method's anonymous `ChannelNotificationExtension`, plus enabled verification/decryption hooks in the security customization layer. No per-method Mapping helper class is generated.

Verify the signature before mapping. Confirm that the institution ACK requirements are compatible with the platform endpoint.

Test real field mapping, missing fields, exceptions that must not become success, verification failure preventing forwarding, notification correlation IDs, and ACK behavior. For operations making HTTP calls, also cover non-2xx responses.
See [Mapping rules](../../04-mapping-and-results.md); [Security](../../05-security.md); [Notifications](../../07-notifications-and-callbacks.md).

## Source evidence

Interface:
`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/notify/NotificationService.java:55`.
Request: `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/base/BaseChannelRequest.java`.
Response:
`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/notify/request/CaptureNotifyRequest.java`.
Field and inheritance definitions with source SHA-256 hashes are recorded in [contracts.json](../contracts.json).
