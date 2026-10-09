# ChallengeActionForm

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.action.ChallengeActionForm`

Challenge action form for 3DS verification or identity authentication.

Extends: [ActionForm](ActionForm.md).

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ----------------------- | ---------------------- | --------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `actionFormType` | `String` | ActionForm | Action form type: RedirectActionForm (redirect), ChallengeActionForm (challenge/identity verification), PaymentCodeForm (payment code). |
| `challengeRenderUrl` | `String` | ChallengeActionForm | Challenge URL for 3DS or identity verification. |
| `challengeRenderData` | `String` | ChallengeActionForm | Challenge rendering data for SDK rendering. |
| `challengeType` | `String` | ChallengeActionForm | Challenge type. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

None of the 12 SPI declarations or their nested fields directly uses this type. It is listed as a retained SDK object, not as evidence of additional integration capabilities.

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/action/ChallengeActionForm.java`  
SHA-256: `ff08b51da012469bdc9d79d89c0570a5aca833f6eb8a42720ce5b164c1fe632f`
