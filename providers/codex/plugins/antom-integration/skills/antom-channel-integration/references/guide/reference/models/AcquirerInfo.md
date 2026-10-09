# AcquirerInfo

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.AcquirerInfo`

Acquirer information object.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------------------- | ---------------------- | -------------- | --------------------------------------------------------------- |
| `acquirerName` | `String` | AcquirerInfo | Acquirer name. |
| `referenceRequestId` | `String` | AcquirerInfo | Reference request ID assigned by APO. |
| `acquirerMerchantId` | `String` | AcquirerInfo | Merchant ID assigned by the acquirer. |
| `acquirerResultCode` | `String` | AcquirerInfo | Acquirer result code. |
| `acquirerResultMessage` | `String` | AcquirerInfo | Acquirer result message. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

None of the 12 SPI declarations or their nested fields directly uses this type. It is listed as a retained SDK object, not as evidence of additional integration capabilities.

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/AcquirerInfo.java`  
SHA-256: `3117aa552a5e1a8cacc37eca302c24ab6b1bcb5864d08cc702b671449a85d9ab`
