# ThreeDSResult

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.ThreeDSResult`

3DS authentication result object.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------------ | ---------------------- | --------------- | ------------------------------------------------------------------------------------------------- |
| `threeDSVersion` | `String` | ThreeDSResult | 3DS protocol version, such as "1.0.2", "2.1.0" or "2.2.0". |
| `eci` | `String` | ThreeDSResult | Electronic Commerce Indicator (ECI) returned by the card scheme. |
| `cavv` | `String` | ThreeDSResult | Cardholder Authentication Value. |
| `xid` | `String` | ThreeDSResult | Transaction ID assigned by the directory server (3DS 1.0). |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [PaymentResultInfo.threeDSResult](PaymentResultInfo.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/ThreeDSResult.java`  
SHA-256: `eeb2b381c2bc5a58f29ce894a961a8efd24d633b393e5720d9869bb12dd9afe3`
