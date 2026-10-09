# CaptureNotifyRequest

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.notify.request.CaptureNotifyRequest`

Capture result notification request object.

Extends: [BaseIpayRequest](BaseIpayRequest.md).

## Fields (including inherited fields)

The table includes inherited fields and describes requirements, formats and business meaning. Business requiredness does not imply automatic SDK validation. Confirm unspecified constraints before integration testing.

| Field | Java type / nested definition | Source | Requirement / format | Business description |
| -------------------- | -------------------------------- | ---------------------- | ------------------------ | ---------------------------------------------------------------------------------------------------------------- |
| `requestHeaders` | `Map<String, String>` | BaseIpayRequest | Retained by the adapter from the notification input | Copy requestHeaders from the notification input to the returned object. The platform then prepares headers for iPay. |
| `rawBody` | `String` | BaseIpayRequest | Retained by the adapter from the notification input | Copy rawBody from the notification input to retain the institution's original payload. It is not the serialized request body sent to iPay. |
| `domain` | `String` | BaseIpayRequest | Populated by the platform later | The platform fills the base address before calling iPay over HTTP. It may be empty when the SPI returns; adapters need not set it. |
| `result` | `Result` → [Result](Result.md) | CaptureNotifyRequest | Required; Result | Standard result of this business call or notification, including status, error code and message; not an HTTP status.<br>Specific error codes are determined by platform result-code mapping configuration. |
| `captureRequestId` | `String` | CaptureNotifyRequest | Required; String(64) | Unique capture request ID used for capture idempotency.<br>Repeated requests with the same ID that have reached final S/F status should return a consistent result; the scaffold provides no idempotency storage. |
| `captureId` | `String` | CaptureNotifyRequest | Optional; String(64) | Unique capture acceptance ID generated after the institution accepts the capture.<br>Not the paymentId of the original payment. |
| `captureAmount` | `Amount` → [Amount](Amount.md) | CaptureNotifyRequest | Required; Amount | Amount corresponding to this capture result. |
| `urlParam` | `String` | CaptureNotifyRequest | Requiredness and length unspecified | URL parameter. |

Java types, inheritance and explicit initializers come from source; the table describes business meaning. An absent initializer does not imply a business default. Apply requiredness according to the method and scenario. String fields do not perform automatic enum validation. Construct full paths from the parent object, such as paymentAmount.currency; address List elements using an actual array index or applicable wildcard rule.

## Protocol reference

- alipay.ais.payments.notifyCapture → [notifyCapture output](../spi/notifyCapture.md).

Fields with the same name have method-specific requirements. See [Payment field definitions](../payment-sources.md) for API references.

## Usage

- SPI: [NotificationService.notifyCapture](../spi/notifyCapture.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/notify/request/CaptureNotifyRequest.java`  
SHA-256: `fa55840caf2c0fb69b490ce40fb7e51d9aa6da66ab8427435feb01f1e431e05e`
