# PaymentCodeForm

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.action.PaymentCodeForm`

Payment code form for displaying a payment code for user scanning.

Extends: [ActionForm](ActionForm.md).

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------------ | -------------------------------------------- | ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `actionFormType` | `String` | ActionForm | Action form type: RedirectActionForm (redirect), ChallengeActionForm (challenge/identity verification), PaymentCodeForm (payment code). |
| `codeDetail` | `CodeDetail` → [CodeDetail](CodeDetail.md) | PaymentCodeForm | Code details for payment code display. |
| `expireTime` | `String` | PaymentCodeForm | Payment code expiration time. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

None of the 14 SPI declarations or their nested fields directly uses this type. It is listed as a retained SDK object, not as evidence of additional integration capabilities.

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/action/PaymentCodeForm.java`  
SHA-256: `dbcc8e83a6c33eb22787c3de97370cd0535c8d66424820ccd59f706402e07669`
