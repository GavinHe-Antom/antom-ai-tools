# ActionForm

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.action.ActionForm`

Base action form describing user interaction required in the payment flow.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------------ | ---------------------- | ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `actionFormType` | `String` | ActionForm | Action form type: RedirectActionForm (redirect), ChallengeActionForm (challenge/identity verification), PaymentCodeForm (payment code). |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Subclass: [ChallengeActionForm](ChallengeActionForm.md)
- Subclass: [PaymentCodeForm](PaymentCodeForm.md)
- Subclass: [RedirectActionForm](RedirectActionForm.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/action/ActionForm.java`  
SHA-256: `c658e4bf1db08acf638092c2c58be9d8855254e60fc5b8742790a15e0440077c`
