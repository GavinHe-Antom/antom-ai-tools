# OrderCodeForm

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.payment.OrderCodeForm`

Order code form object.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| --------------- | ---------------------- | --------------- | ------------------------------- |
| `barcodeData` | `String` | OrderCodeForm | Barcode data. |
| `digitData` | `String` | OrderCodeForm | Digit data. |
| `qrData` | `String` | OrderCodeForm | QR code data. |
| `payCodeUrl` | `String` | OrderCodeForm | Payment code URL. |
| `expireTime` | `String` | OrderCodeForm | Expiration time. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [InquiryPaymentResponse.orderCodeForm](InquiryPaymentResponse.md)
- Parent-object field: [PayResponse.orderCodeForm](PayResponse.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/payment/OrderCodeForm.java`  
SHA-256: `8f13ae6bbf5419ac991bfcf03b42ffd060090a93cfe9593a31c625e684c9eb80`
