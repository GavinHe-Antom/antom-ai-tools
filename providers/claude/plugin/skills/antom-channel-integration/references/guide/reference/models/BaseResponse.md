# BaseResponse

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseResponse`

Base class for standard SPI responses, carrying the operation outcome through Result. Concrete responses may also contain business status in paymentResultInfo/refundResult. Do not confuse HTTP success, operation success and final transaction status.

Base class for all channel SPI responses. Carries unified result information via the composed Result field.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ---------- | -------------------------------- | -------------- | ------------------------------------------ |
| `result` | `Result` → [Result](Result.md) | BaseResponse | Unified result object. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Subclass: [AcsUrlCallbackResponse](AcsUrlCallbackResponse.md)
- Subclass: [NotifyResponse](NotifyResponse.md)
- Subclass: [AuthenticateAuthorizeResponse](AuthenticateAuthorizeResponse.md)
- Subclass: [AuthorizeResponse](AuthorizeResponse.md)
- Subclass: [CancelResponse](CancelResponse.md)
- Subclass: [CaptureResponse](CaptureResponse.md)
- Subclass: [InquiryPaymentResponse](InquiryPaymentResponse.md)
- Subclass: [PayResponse](PayResponse.md)
- Subclass: [InquiryRefundResponse](InquiryRefundResponse.md)
- Subclass: [RefundResponse](RefundResponse.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/base/BaseResponse.java`  
SHA-256: `9702e187ac202427f24eed91ccbf31b5615a54dc52467bb14291997ba1ff5d78`
