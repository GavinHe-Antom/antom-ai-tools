# CaptureResponse

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.payment.response.CaptureResponse`

Capture response object.

Extends: [BaseResponse](BaseResponse.md).

## Fields (including inherited fields)

The table includes inherited fields and describes requirements, formats and business meaning. Business requiredness does not imply automatic SDK validation. Confirm unspecified constraints before integration testing.

| Field | Java type / nested definition | Source | Requirement / format | Business description |
| ----------------- | -------------------------------- | ----------------- | ------------------------------------------ | ---------------------------------------------------------------------------------------------------------------- |
| `result` | `Result` → [Result](Result.md) | BaseResponse | Required; Result | Standard result of this business call or notification, including status, error code and message; not an HTTP status.<br>Specific error codes are determined by platform result-code mapping configuration. |
| `captureId` | `String` | CaptureResponse | Required; String(64) | Unique capture acceptance ID generated after the institution accepts the capture.<br>Not the paymentId of the original payment. |
| `captureAmount` | `Amount` → [Amount](Amount.md) | CaptureResponse | Required; Amount | Amount corresponding to this institution capture. |
| `extendInfo` | `String` | CaptureResponse | As agreed in the integration contract; no length or requiredness constraint is declared in code | Extension information. Agree on its structure; do not assume Map. Confirm that the delivered SDK JAR contains this field before use. |

Java types, inheritance and explicit initializers come from source; the table describes business meaning. An absent initializer does not imply a business default. Apply requiredness according to the method and scenario. String fields do not perform automatic enum validation. Construct full paths from the parent object, such as paymentAmount.currency; address List elements using an actual array index or applicable wildcard rule.

## Protocol reference

- alipay.ais.payments.capture → [capture output](../spi/capture.md).

Fields with the same name have method-specific requirements. See [Payment field definitions](../payment-sources.md) for API references.

## Usage

- SPI: [PaymentService.capture](../spi/capture.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/payment/response/CaptureResponse.java`  
SHA-256: `a5ac687f46ab8c890a2e1c8a136b91da86006067a196902150edb860342966e8`
