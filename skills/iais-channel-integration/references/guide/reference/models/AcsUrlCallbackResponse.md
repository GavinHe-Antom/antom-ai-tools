# AcsUrlCallbackResponse

[Model index](README.md) · [Platform ACS flow](../../07-notifications-and-callbacks.md#6-acs-browser-callback)

`com.alipay.iacqintegrationhub.channel.sdk.api.gateway.response.AcsUrlCallbackResponse`

Platform-owned ACS URL callback response object; not an adapter SPI output.

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

- alipay.ais.payments.acsUrlCallback → [platform ACS flow](../../07-notifications-and-callbacks.md#6-acs-browser-callback).

Fields with the same name have method-specific requirements. See [Payment field definitions](../payment-sources.md) for API references.

## Usage

- Platform `IpayGatewayService.invokeAcsCallBackUrl` and `NotifyFacade.acsUrlCallback`; the Controller validates the redirect URL and returns HTTP 302.

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/api/gateway/response/AcsUrlCallbackResponse.java`

SHA-256: `cb2c1814df4fcc969200a24921f944498523b484660b5e6bbda4496fc87ab6c0`
