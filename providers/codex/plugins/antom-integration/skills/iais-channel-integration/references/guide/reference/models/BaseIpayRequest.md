# BaseIpayRequest

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.base.BaseIpayRequest`

Base class for iPay gateway requests.

Adapters map channel notifications into concrete subclasses. The platform selects iPay paths by notification type and module identity, so these requests contain no channelCode/path. The gateway client overwrites domain with platform configuration before invocation.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Requirement / stage | Business description |
| ------------------ | ----------------------- | ----------------- | ------------------------------------------- | ---------------------------------------------------------------------------------- |
| `requestHeaders` | `Map<String, String>` | BaseIpayRequest | Notifications: adapter retains input values; ACS: depends on the callback scenario | Retain ingress headers during notification mapping. The platform collects ACS headers. |
| `rawBody` | `String` | BaseIpayRequest | Notifications: adapter retains input values; ACS: depends on the callback scenario | Retain the original payload during notification mapping. ACS GET callbacks do not populate this field. |
| `domain` | `String` | BaseIpayRequest | Populated by the platform later | The platform fills the base address before calling iPay over HTTP. It may be empty when the SPI returns; adapters need not set it. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Subclass: [AcsUrlCallbackRequest](AcsUrlCallbackRequest.md)
- Subclass: [CaptureNotifyRequest](CaptureNotifyRequest.md)
- Subclass: [PaymentNotifyRequest](PaymentNotifyRequest.md)
- Subclass: [RefundNotifyRequest](RefundNotifyRequest.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/base/BaseIpayRequest.java`  
SHA-256: `0de9b16dc9070b1806ef9b53ddbd1472223fb2cb5f20bafa223193a8e01218d1`
