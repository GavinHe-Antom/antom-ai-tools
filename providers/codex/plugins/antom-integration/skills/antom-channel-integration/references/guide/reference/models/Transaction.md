# Transaction

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.Transaction`

This object's status/result field is transactionResult (Result); there is no status field. Static success/fail/unknown methods return Result and do not automatically construct a Transaction.

Transaction object representing a subsequent action, such as capture or cancellation.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------------------ | -------------------------------- | ------------- | --------------------------------------------------------------------------------- |
| `transactionId` | `String` | Transaction | Unique transaction ID assigned by the channel. |
| `transactionRequestId` | `String` | Transaction | Unique transaction request ID assigned by the merchant. |
| `transactionAmount` | `Amount` → [Amount](Amount.md) | Transaction | Transaction amount. |
| `transactionType` | `String` | Transaction | Transaction type. |
| `transactionResult` | `Result` → [Result](Result.md) | Transaction | Transaction result. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [InquiryPaymentResponse.transactions](InquiryPaymentResponse.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/Transaction.java`  
SHA-256: `04660dff50195a6d6b61e06af598c995e5d6e1b31ea1ce30c8e9ac451885f718`
