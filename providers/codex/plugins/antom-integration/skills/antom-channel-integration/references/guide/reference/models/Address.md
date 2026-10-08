# Address

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.base.Address`

Reusable business address fields for billing and other address mappings; no country/region format validation is performed.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------ | ---------------------- | --------- | ------------------------------------------------------------------------------------------------- |
| `region` | `String` | Address | Two-letter country/region code; refer to ISO 3166. |
| `state` | `String` | Address | State/County/Province. |
| `city` | `String` | Address | City/District/Suburb/Town/Village. |
| `address1` | `String` | Address | Address line 1 (street address/PO box/company name). |
| `address2` | `String` | Address | Address line 2 (apartment/suite/unit/building). |
| `zipCode` | `String` | Address | ZIP or postal code. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [PaymentMethodMetadata.billingAddress](PaymentMethodMetadata.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/base/Address.java`  
SHA-256: `dfe8411bbb9fc5f3ef5ba0ab2866020ebc666b07d5baa0e0ea080659c1d39520`
