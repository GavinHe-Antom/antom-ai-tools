# PaymentFactor

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.PaymentFactor`

Payment factors that affect the payment flow.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------------- | ---------------------- | --------------- | ------------------------------------------------------------------------------------ |
| `isAuthorization` | `String` | PaymentFactor | Whether this is a preauthorization. |
| `captureMode` | `String` | PaymentFactor | Capture mode: AUTOMATIC or MANUAL. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [AuthenticateAuthorizeRequest.paymentFactor](AuthenticateAuthorizeRequest.md)
- Parent-object field: [AuthorizeRequest.paymentFactor](AuthorizeRequest.md)
- Parent-object field: [PayRequest.paymentFactor](PayRequest.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/PaymentFactor.java`  
SHA-256: `d0244161a60f373c349aeec4728d9bd5bbe0fd06127c6cd2eaf48853d69231d1`
