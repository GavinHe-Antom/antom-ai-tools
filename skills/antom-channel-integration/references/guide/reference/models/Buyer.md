# Buyer

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.Buyer`

Buyer information object.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| -------------------- | ---------------------- | ------- | ---------------------------------------------- |
| `referenceBuyerId` | `String` | Buyer | Buyer's reference user ID. |
| `buyerName` | `String` | Buyer | Buyer name. |
| `buyerPhoneNo` | `String` | Buyer | Buyer phone number. |
| `buyerEmail` | `String` | Buyer | Buyer email. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [Order.buyer](Order.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/Buyer.java`  
SHA-256: `ce61a2b3b9634f2a27d811e99317870847ea8b2de54aa1274b38ff9fcf858c3f`
