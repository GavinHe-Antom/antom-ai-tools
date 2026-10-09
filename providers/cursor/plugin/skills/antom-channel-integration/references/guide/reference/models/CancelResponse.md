# CancelResponse

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.spi.payment.response.CancelResponse`

Cancellation response object.

Extends: [BaseResponse](BaseResponse.md).

## Fields (including inherited fields)

The table includes inherited fields and describes requirements, formats and business meaning. Business requiredness does not imply automatic SDK validation. Confirm unspecified constraints before integration testing.

| Field | Java type / nested definition | Source | Requirement / format | Business description |
| ---------- | -------------------------------- | -------------- | ----------------- | ---------------------------------------------------------------------------------------------------------------- |
| `result` | `Result` → [Result](Result.md) | BaseResponse | Required; Result | Standard result of this business call or notification, including status, error code and message; not an HTTP status.<br>Specific error codes are determined by platform result-code mapping configuration. |

Java types, inheritance and explicit initializers come from source; the table describes business meaning. An absent initializer does not imply a business default. Apply requiredness according to the method and scenario. String fields do not perform automatic enum validation. Construct full paths from the parent object, such as paymentAmount.currency; address List elements using an actual array index or applicable wildcard rule.

## Protocol reference

- alipay.ais.payments.cancel → [cancel output](../spi/cancel.md).

Fields with the same name have method-specific requirements. See [Payment field definitions](../payment-sources.md) for API references.

## Usage

- SPI: [PaymentService.cancel](../spi/cancel.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/spi/payment/response/CancelResponse.java`  
SHA-256: `526020dcca0d954ec20347b72028d7d57f4d62092a7083fc142c88d48057ac7d`
