# NotifyResponse

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.notify.response.NotifyResponse`

Unified notification response object.

Extends: [BaseResponse](BaseResponse.md).

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ---------------- | -------------------------------- | ---------------- | ------------------------------------------ |
| `result` | `Result` → [Result](Result.md) | BaseResponse | Unified result object. |
| `responseText` | `String` | NotifyResponse | Response text. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Notification acknowledgment

responseText is the acknowledgment text parsed by the platform from the actual iPay response; it is not guaranteed to be `success`. It belongs to NotifyResponse, not the standard notification request returned by NotificationService. The platform Controller's final response must match the institution's ACK requirements.

API references: payment, capture and refund notifications.
