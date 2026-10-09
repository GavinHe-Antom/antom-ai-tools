# PaymentResultInfo

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.PaymentResultInfo`

Payment result information object.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------------------ | ----------------------------------------------------- | ------------------- | --------------------------------------------- |
| `avsResultRaw` | `String` | PaymentResultInfo | Raw AVS result value. |
| `cvvResultRaw` | `String` | PaymentResultInfo | Raw CVV result value. |
| `networkTransactionId` | `String` | PaymentResultInfo | Network transaction ID. |
| `threeDSResult` | `ThreeDSResult` → [ThreeDSResult](ThreeDSResult.md) | PaymentResultInfo | 3DS authentication result. |
| `authenticationFlow` | `String` | PaymentResultInfo | Authentication flow. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [PaymentNotifyRequest.paymentResultInfo](PaymentNotifyRequest.md)
- Parent-object field: [AuthenticateAuthorizeResponse.paymentResultInfo](AuthenticateAuthorizeResponse.md)
- Parent-object field: [InquiryPaymentResponse.paymentResultInfo](InquiryPaymentResponse.md)
- Parent-object field: [PayResponse.paymentResultInfo](PayResponse.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/PaymentResultInfo.java`  
SHA-256: `a1ab6e89c9f43f9db51b91c470c3d8676a186bcd9676327409d47609342224e9`
