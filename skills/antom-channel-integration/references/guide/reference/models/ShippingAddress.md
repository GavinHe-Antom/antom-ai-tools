# ShippingAddress

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.ShippingAddress`

Shipping address information object.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------ | ---------------------- | ----------------- | -------------------------------- |
| `region` | `String` | ShippingAddress | Region/Country. |
| `zipCode` | `String` | ShippingAddress | ZIP code. |
| `state` | `String` | ShippingAddress | State/Province. |
| `city` | `String` | ShippingAddress | City. |
| `address1` | `String` | ShippingAddress | Address line 1. |
| `address2` | `String` | ShippingAddress | Address line 2. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [Shipping.shippingAddress](Shipping.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/ShippingAddress.java`  
SHA-256: `24fe262e9dfe80f7d4f85c1fba23f9ec1e321c00865bf555caff80bb0b9a8404`
