# PaymentMethodMetadata

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.PaymentMethodMetadata`

**Object containing sensitive fields.** The cardNo comment describes encrypted or tokenized data, but its String type does not prove the actual input is encrypted. This DTO performs no encryption; the gateway integration contract determines the actual format. Do not serialize the complete object to logs.

Payment method metadata containing card-payment fields; it performs no encryption, decryption or masking. It contains sensitive data such as card number, name and CVV. Do not log it through toString or full JSON serialization.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ----------------------- | ----------------------------------- | ----------------------- | --------------------------------------------------------------- |
| `cardNo` | `String` | PaymentMethodMetadata | Card number (encrypted or tokenized). |
| `cardholderName` | `String` | PaymentMethodMetadata | Cardholder name. |
| `expiryYear` | `String` | PaymentMethodMetadata | Card expiry year. |
| `expiryMonth` | `String` | PaymentMethodMetadata | Card expiry month (two digits). |
| `billingAddress` | `Address` → [Address](Address.md) | PaymentMethodMetadata | Billing address. |
| `is3DSAuthentication` | `Boolean` | PaymentMethodMetadata | Whether 3DS authentication is required. |
| `cvv` | `String` | PaymentMethodMetadata | Security code (CVV/CVC). |
| `extendInfoMap` | `Map<String, String>` | PaymentMethodMetadata | Extension information. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [PaymentMethod.paymentMethodMetadata](PaymentMethod.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/PaymentMethodMetadata.java`  
SHA-256: `135049e5e59974f9026abfbb716a0b07a117289d746321d18a3fc2220e4b032c`
