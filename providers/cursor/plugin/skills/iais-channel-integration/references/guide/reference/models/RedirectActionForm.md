# RedirectActionForm

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.action.RedirectActionForm`

Redirect action form for navigating the user to a specified page.

Extends: [ActionForm](ActionForm.md).

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------------ | ---------------------- | -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `actionFormType` | `String` | ActionForm | Action form type: RedirectActionForm (redirect), ChallengeActionForm (challenge/identity verification), PaymentCodeForm (payment code). |
| `redirectUrl` | `String` | RedirectActionForm | Redirect URL for a normal browser. |
| `appLinkUrl` | `String` | RedirectActionForm | App-link URL for mobile deep linking. |
| `schemeUrl` | `String` | RedirectActionForm | Scheme URL./ Scheme URL. |
| `method` | `String` | RedirectActionForm | HTTP method for redirect: GET or POST. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

None of the 14 SPI declarations or their nested fields directly uses this type. It is listed as a retained SDK object, not as evidence of additional integration capabilities.

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/action/RedirectActionForm.java`  
SHA-256: `a83019471c2d5be1d59484168e8e5c43aee4b95f597abb7e4320e24f257bdc65`
