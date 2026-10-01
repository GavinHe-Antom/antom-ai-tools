# RefundResponse

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.refund.response.RefundResponse`

Refund response object.

Extends: [BaseResponse](BaseResponse.md).

## Fields (including inherited fields)

The table includes inherited fields and describes requirements, formats and business meaning. Business requiredness does not imply automatic SDK validation. Confirm unspecified constraints before integration testing.

| Field | Java type / nested definition | Source | Requirement / format | Business description |
| ---------------- | -------------------------------- | ---------------- | ------------------ | ------------------------------------------------------------------------------------------------------ |
| `result` | `Result` → [Result](Result.md) | BaseResponse | Required; Result | Standard result for this call or refund notification, expressing status and error information.<br>Not an HTTP status; specific result codes follow platform mapping configuration. |
| `refundId` | `String` | RefundResponse | Required; String(64) | Unique refund acceptance ID returned after the institution accepts the refund.<br>Maintain separately from refundRequestId; requiredness varies by method. |
| `refundAmount` | `Amount` → [Amount](Amount.md) | RefundResponse | Optional; Amount | Amount to refund to the buyer for this request, from the institution's perspective.<br>Use Amount for currency and the amount in the smallest currency unit. |
| `arn` | `String` | RefundResponse | Optional; String(64) | Acquirer Reference Number supplied by the acquiring bank.<br>Used to correlate the acquiring bank's refund reference record. |

Java types, inheritance and explicit initializers come from source; the table describes business meaning. An absent initializer does not imply a business default. Adapters must validate the requirements and formats in the table; String fields do not perform automatic enum validation. Construct full paths from the parent object, such as paymentAmount.currency; address List elements using an actual array index or applicable wildcard rule.

## Protocol reference

alipay.ais.payments.refund, see [refund output](../spi/refund.md).

Fields with the same name have method-specific requirements. See [Refund field definitions](../refund-sources.md) for API references.

## Usage

- SPI: [RefundService.refund](../spi/refund.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/refund/response/RefundResponse.java`  
SHA-256: `52f81514d13601bcfffa45517f8a1a4b0da29216937e8878ad81011195ea4a47`
