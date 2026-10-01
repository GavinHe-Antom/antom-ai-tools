# Env

[Model index](README.md) · [SPI index](../spi/README.md)

`com.alipay.iacqintegrationhub.channel.sdk.model.base.Env`

Device and network environment in which the order is placed.

## Fields (including inherited fields)

| Field | Java type / nested definition | Source | Description (source comments) |
| ------------------ | ----------------------------------------------- | ------ | ------------------------------------------------------------------------------------------------ |
| `terminalType` | `String` | Env | Terminal type. Valid values: APP, WEB, WAP. |
| `osType` | `String` | Env | Device operating-system type, such as "IOS" or "ANDROID". |
| `clientIp` | `String` | Env | Client IP address. |
| `colorDepth` | `Integer` | Env | Color depth. |
| `screenHeight` | `Integer` | Env | Screen height. |
| `screenWidth` | `Integer` | Env | Screen width. |
| `timeZoneOffset` | `Integer` | Env | Time-zone offset. |
| `deviceLanguage` | `String` | Env | Device language. |
| `deviceId` | `String` | Env | Device ID. |
| `browserInfo` | `BrowserInfo` → [BrowserInfo](BrowserInfo.md) | Env | Browser information. |

Java types, field comments and explicit initializers come from source. An absent initializer does not imply a business default. This page does not infer requiredness, length, enum binding or JSON validation rules. A String field does not automatically restrict values even if a related enum exists. Construct full field paths from the parent object, such as paymentAmount.currency; address List elements using the actual array index or applicable wildcard rule.

## Usage

- Parent-object field: [AuthorizeRequest.env](AuthorizeRequest.md)
- Parent-object field: [PayRequest.env](PayRequest.md)

## Source

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/model/base/Env.java`  
SHA-256: `eee192697018ca420982ab6b3a2c312c9a4bc4795f912e03cc5b5950b58f12e5`
