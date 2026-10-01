# Merchant

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.Merchant`

Merchant information object.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ----------------------- | ---------------------- | ---------- | -------------------------------------------- |
| `referenceMerchantId` | `String` | Merchant | Reference merchant ID. |
| `merchantDisplayName` | `String` | Merchant | Merchant display name. |
| `merchantMcc` | `String` | Merchant | Merchant MCC code. |
| `merchantName` | `String` | Merchant | Merchant name. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [Order.merchant](Order.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/Merchant.java`  
SHA-256: `81d8c27e4b0b767abab670d718cd726dff684d29a979a5a1d4b95e7143cae49d`
