# InquiryPaymentResponse

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.payment.response.InquiryPaymentResponse`

Payment inquiry response object.

Extends: [BaseResponse](BaseResponse.md).

## Fields (including inherited fields)

The table includes inherited fields and describes requirements, formats and business meaning. Business requiredness does not imply automatic SDK validation. Confirm unspecified constraints before integration testing.

| Field | Java type / nested definition | Source | Requirement / format | Business description |
| --------------------- | ----------------------------------------------------------------- | ------------------------ | --------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `result` | `Result` → [Result](Result.md) | BaseResponse | Required; Result | Standard result of this business call or notification, including status, error code and message; not an HTTP status.<br>Specific error codes are determined by platform result-code mapping configuration. |
| `paymentAmount` | `Amount` → [Amount](Amount.md) | InquiryPaymentResponse | Optional; Amount | Amount the institution expects to collect from the buyer for this payment. |
| `normalUrl` | `String` | InquiryPaymentResponse | Optional; applicable when payment requires further user action; URL, up to 2048 characters | Address for opening a WAP/WEB payment page in the default browser or an embedded WebView.<br>Return according to institution interaction when payment continuation or redirection is required; processing status does not always require a redirect. |
| `orderCodeForm` | `OrderCodeForm` → [OrderCodeForm](OrderCodeForm.md) | InquiryPaymentResponse | Optional; OrderCodeForm | Order-code information returned when the payment method supports it. |
| `paymentResultInfo` | `PaymentResultInfo` → [PaymentResultInfo](PaymentResultInfo.md) | InquiryPaymentResponse | Optional; PaymentResultInfo | Additional payment result information, which may be returned for CARD.<br>See PaymentResultInfo for nested fields. |
| `transactions` | `List<Transaction>` → [Transaction](Transaction.md) | InquiryPaymentResponse | Optional; Array; Java type: List<Transaction> | Results of subsequent operations associated with the original payment.<br>Represents subsequent operations such as capture and cancellation; actual scope depends on institution capabilities. Each sub-result uses transactionResult. |
| `extendInfo` | `String` | InquiryPaymentResponse | Optional; String(2048) | Extension information.<br>Java type is String, not Map; agree on the contents before mapping. |

Java types, inheritance and explicit initializers come from source; the table describes business meaning. An absent initializer does not imply a business default. Apply requiredness according to the method and scenario. String fields do not perform automatic enum validation. Construct full paths from the parent object, such as paymentAmount.currency; address List elements using an actual array index or applicable wildcard rule.

## Protocol reference

- alipay.ais.payments.inquiryPayment → [inquiryPayment output](../spi/inquiryPayment.md).

Fields with the same name have method-specific requirements. See [Payment field definitions](../payment-sources.md) for API references.

## Usage

- SPI: [PaymentService.inquiryPayment](../spi/inquiryPayment.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/payment/response/InquiryPaymentResponse.java`  
SHA-256: `399827ff473ca56cafe7e4aa96dfb54b7d893b1c8f822bbdf785e28b9c2807d5`
