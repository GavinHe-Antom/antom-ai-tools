# PaymentMethod

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.PaymentMethod`

Payment method object.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------------------- | ----------------------------------------------------------------------------- | --------------- | ----------------------------------------------------------------------------------------- |
| `paymentMethodType` | `String` | PaymentMethod | Payment method type, such as "CARD", "APPLEPAY", "GOOGLEPAY" or "ALIPAY_CN". |
| `paymentMethodMetadata` | `PaymentMethodMetadata` → [PaymentMethodMetadata](PaymentMethodMetadata.md) | PaymentMethod | Payment method metadata. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [AuthenticateAuthorizeRequest.paymentMethod](AuthenticateAuthorizeRequest.md)
- Parent-object field: [AuthorizeRequest.paymentMethod](AuthorizeRequest.md)
- Parent-object field: [PayRequest.paymentMethod](PayRequest.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/PaymentMethod.java`  
SHA-256: `ab3ae53cbc00d920d4070c902ebe34c291d9bb457838336ea561b7d3cf68d599`
