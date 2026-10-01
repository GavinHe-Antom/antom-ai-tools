# Amount

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.base.Amount`

Amount in the smallest currency unit, together with its currency. This object performs no amount validation or unit conversion. If the channel uses major currency units or fixed decimal places, the adapter must convert explicitly using currency precision instead of treating value as a decimal amount.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------ | ---------------------- | -------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| `currency` | `String` | Amount | Three-letter ISO 4217 currency code, such as "USD", "EUR" or "CNY". |
| `value` | `String` | Amount | Amount in the smallest currency unit. For example, 100 cents represents USD 1.00, while 100 represents JPY 100. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [Good.goodsUnitAmount](Good.md)
- Parent-object field: [Order.orderAmount](Order.md)
- Parent-object field: [Transaction.transactionAmount](Transaction.md)
- Parent-object field: [CaptureNotifyRequest.captureAmount](CaptureNotifyRequest.md)
- Parent-object field: [PaymentNotifyRequest.paymentAmount](PaymentNotifyRequest.md)
- Parent-object field: [RefundNotifyRequest.refundAmount](RefundNotifyRequest.md)
- Parent-object field: [AuthenticateAuthorizeRequest.paymentAmount](AuthenticateAuthorizeRequest.md)
- Parent-object field: [AuthorizeRequest.paymentAmount](AuthorizeRequest.md)
- Parent-object field: [CaptureRequest.captureAmount](CaptureRequest.md)
- Parent-object field: [PayRequest.paymentAmount](PayRequest.md)
- Parent-object field: [AuthenticateAuthorizeResponse.paymentAmount](AuthenticateAuthorizeResponse.md)
- Parent-object field: [CaptureResponse.captureAmount](CaptureResponse.md)
- Parent-object field: [InquiryPaymentResponse.paymentAmount](InquiryPaymentResponse.md)
- Parent-object field: [PayResponse.paymentAmount](PayResponse.md)
- Parent-object field: [RefundRequest.refundAmount](RefundRequest.md)
- Parent-object field: [InquiryRefundResponse.refundAmount](InquiryRefundResponse.md)
- Parent-object field: [RefundResponse.refundAmount](RefundResponse.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/base/Amount.java`  
SHA-256: `5d77cb18907a49b7c712f94e526b9542a3c5b36e2ea896cea7b8e128e65399bd`
