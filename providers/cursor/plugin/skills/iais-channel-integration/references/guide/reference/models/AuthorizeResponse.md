# AuthorizeResponse

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.payment.response.AuthorizeResponse`

**Retained type, not used by current SPI methods. Use AuthenticateAuthorizeRequest/Response for post-authentication authorization.**

Authorization response model containing only the unified Result. The authentication/authorization SPI actually returns AuthenticateAuthorizeResponse; retaining this model does not add a separate authorization API.

Extends: [BaseResponse](BaseResponse.md).

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ---------- | -------------------------------- | -------------- | ------------------------------------------ |
| `result` | `Result` → [Result](Result.md) | BaseResponse | Unified result object. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

None of the 14 SPI declarations or their nested fields directly uses this type. It is listed as a retained SDK object, not as evidence of additional integration capabilities.

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/payment/response/AuthorizeResponse.java`  
SHA-256: `5dddfad326ca04e349491dd579a99be985920e4bfba410b3b3ef31b61a9bf60c`
