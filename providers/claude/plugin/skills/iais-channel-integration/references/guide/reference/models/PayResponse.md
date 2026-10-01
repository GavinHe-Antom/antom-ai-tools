# PayResponse

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.payment.response.PayResponse`

Payment response object.

Extends: [BaseResponse](BaseResponse.md).

## Fields (including inherited fields)

The table includes inherited fields and describes requirements, formats and business meaning. Business requiredness does not imply automatic SDK validation. Confirm unspecified constraints before integration testing.

| Field | Java type / nested definition | Source | Requirement / format | Business description |
| --------------------- | ----------------------------------------------------------------- | -------------- | --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `result` | `Result` → [Result](Result.md) | BaseResponse | Required; Result | Standard result of this business call or notification, including status, error code and message; not an HTTP status.<br>Specific error codes are determined by platform result-code mapping configuration. |
| `paymentId` | `String` | PayResponse | Optional; String(64) | Unique payment acceptance ID returned after the institution accepts the payment.<br>Maintain separately from paymentRequestId. |
| `paymentAmount` | `Amount` → [Amount](Amount.md) | PayResponse | Optional; Amount | Amount the institution expects to collect from the buyer for this payment. |
| `paymentCreateTime` | `String` | PayResponse | Optional; Datetime; Java type: String | Creation time of the institution's payment record.<br>Payment-record creation time, not the time payment reaches a successful final state. |
| `normalUrl` | `String` | PayResponse | Optional; applicable when payment requires further user action; URL, up to 2048 characters | Address for opening a WAP/WEB payment page in the default browser or an embedded WebView.<br>Return according to institution interaction when payment continuation or redirection is required; processing status does not always require a redirect. |
| `paymentResultInfo` | `PaymentResultInfo` → [PaymentResultInfo](PaymentResultInfo.md) | PayResponse | Optional; PaymentResultInfo | Additional payment result information, which may be returned for CARD.<br>See PaymentResultInfo for nested fields. |
| `orderCodeForm` | `OrderCodeForm` → [OrderCodeForm](OrderCodeForm.md) | PayResponse | Optional; OrderCodeForm | Order-code information returned when the payment method supports it. |
| `extendInfo` | `String` | PayResponse | Optional; String(2048) | Extension information.<br>Java type is String, not Map; agree on the contents before mapping. |

Java types, inheritance and explicit initializers come from source; the table describes business meaning. An absent initializer does not imply a business default. Apply requiredness according to the method and scenario. String fields do not perform automatic enum validation. Construct full paths from the parent object, such as paymentAmount.currency; address List elements using an actual array index or applicable wildcard rule.

## Protocol reference

- alipay.ais.payments.pay → [pay output](../spi/pay.md).

Fields with the same name have method-specific requirements. See [Payment field definitions](../payment-sources.md) for API references.

## Usage

- SPI: [PaymentService.pay](../spi/pay.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/payment/response/PayResponse.java`  
SHA-256: `47cc8b7a9756cb2f42a0bf59371c71d04b31ca0a8f2c80266d8f0dd9bf252290`
