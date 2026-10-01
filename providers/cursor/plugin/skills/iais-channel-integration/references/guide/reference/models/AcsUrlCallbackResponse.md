# AcsUrlCallbackResponse

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.callback.response.AcsUrlCallbackResponse`

ACS URL callback response object.

Extends: [BaseResponse](BaseResponse.md).

## Fields (including inherited fields)

The table includes inherited fields and describes requirements, formats and business meaning. Business requiredness does not imply automatic SDK validation. Confirm unspecified constraints before integration testing.

| Field | Java type / nested definition | Source | Requirement / format | Business description |
| ------------------------------------ | -------------------------------- | ------------------------ | ---------------------- | ---------------------------------------------------------------------- |
| `result` | `Result` → [Result](Result.md) | BaseResponse | Requiredness and length unspecified | Unified result object. |
| `businessId` | `String` | AcsUrlCallbackResponse | Requiredness and length unspecified | Business ID. |
| `iopengwSystemInnerRedirectionUrl` | `String` | AcsUrlCallbackResponse | Requiredness and length unspecified | Internal redirect URL of the iopengw system. |

Java types, inheritance and explicit initializers come from source; the table describes business meaning. An absent initializer does not imply a business default. Apply requiredness according to the method and scenario. String fields do not perform automatic enum validation. Construct full paths from the parent object, such as paymentAmount.currency; address List elements using an actual array index or applicable wildcard rule.

## Protocol reference

- alipay.ais.payments.acsUrlCallback → [acsUrlCallback output](../spi/acsUrlCallback.md).
- alipay.ais.payments.onlinebank.urlCallback → [onlineBankUrlCallback output](../spi/onlineBankUrlCallback.md).

Fields with the same name have method-specific requirements. See [Payment field definitions](../payment-sources.md) for API references.

## Usage

- SPI: [AcsUrlCallbackService.acsUrlCallback](../spi/acsUrlCallback.md)
- SPI: [AcsUrlCallbackService.onlineBankUrlCallback](../spi/onlineBankUrlCallback.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/callback/response/AcsUrlCallbackResponse.java`  
SHA-256: `82f55e60e7d10bf7a41c7d0778c5c97a9a18f496945d092772149fb22bd981a5`
