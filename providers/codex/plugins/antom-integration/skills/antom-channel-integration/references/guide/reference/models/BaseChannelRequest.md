# BaseChannelRequest

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseChannelRequest`

Base class for channel-facing SPI requests carrying platform-populated identity, original payload and outbound address. Before SPI invocation, the platform injects channel identity, headers and body. Requests to channel APIs also receive domain/path; notification inputs do not receive outbound addresses. Adapters map business fields and must not derive trusted channel identity or outbound domains from inbound bodies or headers.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Requirement / stage | Business description |
| ------------------ | ----------------------- | -------------------- | --------------------------------------------------------- | ---------------------------------------------------------------------------- |
| `requestHeaders` | `Map<String, String>` | BaseChannelRequest | Injected by the platform; always present | Ingress headers collected by the platform for protocol mapping, not the final downstream headers. |
| `channelCode` | `String` | BaseChannelRequest | Injected by the platform; always present | Channel identity declared by the platform module. Adapters must not derive or override it from a payload. |
| `runtimeEnv` | `String` | BaseChannelRequest | Injected by the platform; may be absent | Platform runtime environment (source examples: test, dev, pre, gray, prod; not enum-constrained). Absence does not imply a test environment. This field does not select shadow/sandbox routes; see [Invocation context](../context.md). |
| `rawBody` | `String` | BaseChannelRequest | Injected by the platform; always present | Payload text received by the platform for parsing and signature verification, not the original byte stream. |
| `domain` | `String` | BaseChannelRequest | Channel API requests: injected by the platform and always present; notification inputs: not applicable | Platform-selected channel base address for protocol and signature computation; the platform controls final egress. |
| `path` | `String` | BaseChannelRequest | Channel API requests: injected by the platform and always present; notification inputs: not applicable | Channel path selected by the platform for the operation. It may contain placeholders resolved by the platform HTTP service. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- SPI: [NotificationService.notifyPayment](../spi/notifyPayment.md)
- SPI: [NotificationService.notifyCapture](../spi/notifyCapture.md)
- SPI: [NotificationService.notifyRefund](../spi/notifyRefund.md)
- SPI: [NotificationService.notifyOnlineBankPayment](../spi/notifyOnlineBankPayment.md)
- SPI: [NotificationService.notifyReceivePayment](../spi/notifyReceivePayment.md)
- Subclass: [AuthenticateAuthorizeRequest](AuthenticateAuthorizeRequest.md)
- Subclass: [AuthorizeRequest](AuthorizeRequest.md)
- Subclass: [CancelRequest](CancelRequest.md)
- Subclass: [CaptureRequest](CaptureRequest.md)
- Subclass: [InquiryPaymentRequest](InquiryPaymentRequest.md)
- Subclass: [PayRequest](PayRequest.md)
- Subclass: [InquiryRefundRequest](InquiryRefundRequest.md)
- Subclass: [RefundRequest](RefundRequest.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/base/BaseChannelRequest.java`  
SHA-256: `4e4210a7acbebbcb97ebdeadd1749c6824680bda6d7400b09481594db4e3c190`
