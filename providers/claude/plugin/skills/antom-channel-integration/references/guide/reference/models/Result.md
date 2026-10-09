# Result

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.base.Result`

Unified result object for all API responses.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ----------------- | ---------------------- | -------- | ---------------------------------------------------------------------------------------------------------------------------- |
| `resultStatus` | `String` | Result | Result status. Valid values: S (success), F (failure), U (unknown). |
| `resultCode` | `String` | Result | Result code indicating the specific outcome. |
| `resultMessage` | `String` | Result | Human-readable result message. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [Transaction.transactionResult](Transaction.md)
- Parent-object field: [BaseResponse.result](BaseResponse.md)
- Parent-object field: [CaptureNotifyRequest.result](CaptureNotifyRequest.md)
- Parent-object field: [PaymentNotifyRequest.result](PaymentNotifyRequest.md)
- Parent-object field: [RefundNotifyRequest.result](RefundNotifyRequest.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/base/Result.java`  
SHA-256: `2d84341caadd1f06d35814a7be75b84e09e6a19901fa5ddd9279371a67cccfb4`
