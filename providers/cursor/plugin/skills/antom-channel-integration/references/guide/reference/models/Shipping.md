# Shipping

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.Shipping`

Shipping information object.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------------- | ----------------------------------------------------------- | ---------- | -------------------------------------------- |
| `shippingName` | `ShippingName` → [ShippingName](ShippingName.md) | Shipping | Recipient name. |
| `shippingAddress` | `ShippingAddress` → [ShippingAddress](ShippingAddress.md) | Shipping | Recipient address. |
| `shippingPhoneNo` | `String` | Shipping | Recipient phone number. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [Order.shipping](Order.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/Shipping.java`  
SHA-256: `47ac5c2a23ba61331683c9cbe7dc2dbc56496fce01258b7b79b35e5dfd994c60`
