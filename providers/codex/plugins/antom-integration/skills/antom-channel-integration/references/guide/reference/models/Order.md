# Order

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.Order`

Order information object.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| -------------------- | -------------------------------------- | ------- | ----------------------------------------- |
| `referenceOrderId` | `String` | Order | Merchant-side order ID. |
| `orderDescription` | `String` | Order | Order description. |
| `orderAmount` | `Amount` → [Amount](Amount.md) | Order | Order amount. |
| `buyer` | `Buyer` → [Buyer](Buyer.md) | Order | Buyer information. |
| `merchant` | `Merchant` → [Merchant](Merchant.md) | Order | Merchant information. |
| `goods` | `List<Good>` → [Good](Good.md) | Order | Goods information list. |
| `shipping` | `Shipping` → [Shipping](Shipping.md) | Order | Shipping information. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [AuthenticateAuthorizeRequest.order](AuthenticateAuthorizeRequest.md)
- Parent-object field: [AuthorizeRequest.order](AuthorizeRequest.md)
- Parent-object field: [PayRequest.order](PayRequest.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/Order.java`  
SHA-256: `267974b096a97d3fc89b52ae348381c29e7975ccd0ce1c779484b6d31fb62b5e`
