# Good

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.Good`

Goods information object.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ---------------------- | -------------------------------- | ------ | ----------------------------------------- |
| `referenceGoodsId` | `String` | Good | Reference goods ID. |
| `goodsName` | `String` | Good | Goods name. |
| `goodsCategory` | `String` | Good | Goods category. |
| `goodsUnitAmount` | `Amount` → [Amount](Amount.md) | Good | Goods unit amount. |
| `goodsQuantity` | `Integer` | Good | Goods quantity. |
| `goodsUrl` | `String` | Good | Goods URL. |
| `deliveryMethodType` | `String` | Good | Delivery method type. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [Order.goods](Order.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/Good.java`  
SHA-256: `88d28b6ad3c2106ae57353eb968464bcddbc30e389598835009b7d7c1ee743c3`
