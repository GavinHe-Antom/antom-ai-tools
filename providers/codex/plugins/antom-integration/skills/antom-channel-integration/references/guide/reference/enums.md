# SDK business enums

An enum does not mean the platform supports the corresponding capability. A String field is not automatically constrained by an enum with a matching name. For example, TransactionType.CHARGEBACK does not imply a dispute
SPI. Retained enums such as GrantType do not imply vaulting support. See the [Security reference](security.md) for security enums.

## ActionFormType

Action form type enumeration.

| Name | Constructor argument | Source description |
| --- | --- | --- |
| `REDIRECT` | "REDIRECT" | Redirect |
| `CHALLENGE` | "CHALLENGE" | Challenge / identity verification |
| `PAYMENT_CODE` | "PAYMENT_CODE" | Payment code |

Source: `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/enums/ActionFormType.java`.

## CaptureMode

Capture mode enumeration.

| Name | Constructor argument | Source description |
| --- | --- | --- |
| `AUTOMATIC` | "AUTOMATIC" | Automatic capture |
| `MANUAL` | "MANUAL" | Manual capture |

Source: `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/enums/CaptureMode.java`.

## GrantType

Grant type enumeration.

| Name | Constructor argument | Source description |
| --- | --- | --- |
| `AUTHORIZATION_CODE` | "AUTHORIZATION_CODE" | Authorization code, used to obtain an access token |
| `REFRESH_TOKEN` | "REFRESH_TOKEN" | Refresh token, used to refresh an access token |

Source: `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/enums/GrantType.java`.

## NotifyType

Notification type enumeration.

| Name | Constructor argument | Source description |
| --- | --- | --- |
| `PAYMENT_RESULT` | "PAYMENT_RESULT" | Payment result notification |
| `REFUND_RESULT` | "REFUND_RESULT" | Refund result notification |
| `CAPTURE_RESULT` | "CAPTURE_RESULT" | Capture result notification |
| `PAYMENT_PENDING` | "PAYMENT_PENDING" | Payment pending notification |

Source: `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/enums/NotifyType.java`.

## PaymentMethodType

Payment method type enumeration.

| Name | Constructor argument | Source description |
| --- | --- | --- |
| `CARD` | "CARD" | Card payment |
| `APPLEPAY` | "APPLEPAY" | Apple Pay |
| `GOOGLEPAY` | "GOOGLEPAY" | Google Pay |
| `ALIPAY_CN` | "ALIPAY_CN" | Alipay China |
| `ALIPAY_HK` | "ALIPAY_HK" | Alipay Hong Kong |
| `GCASH` | "GCASH" | GCash wallet |
| `DANA` | "DANA" | DANA wallet |
| `KAKAOPAY` | "KAKAOPAY" | KakaoPay wallet |
| `TRUEMONEY` | "TRUEMONEY" | TrueMoney wallet |
| `TNG` | "TNG" | Touch 'n Go wallet |
| `ONLINEBANKING_YAPILY` | "ONLINEBANKING_YAPILY" | Online banking via Yapily |
| `BANCONTACT` | "BANCONTACT" | Bancontact |
| `IDEAL` | "IDEAL" | iDEAL |
| `SOFORT` | "SOFORT" | Sofort |
| `GIROPAY` | "GIROPAY" | Giropay |
| `EPS` | "EPS" | EPS |
| `MULTIBANCO` | "MULTIBANCO" | Multibanco |
| `BLIK` | "BLIK" | BLIK |
| `MBWAY` | "MBWAY" | MB WAY |
| `PAYPAL` | "PAYPAL" | PayPal |
| `WECHATPAY` | "WECHATPAY" | WeChat Pay |

Source: `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/enums/PaymentMethodType.java`.

## ResultStatusType

Result status type enumeration.

| Name | Constructor argument | Source description |
| --- | --- | --- |
| `S` | "S" | Success |
| `F` | "F" | Failure |
| `U` | "U" | Unknown |

Source: `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/enums/ResultStatusType.java`.

## TerminalType

Terminal type enumeration.

| Name | Constructor argument | Source description |
| --- | --- | --- |
| `APP` | "APP" | Mobile app |
| `WEB` | "WEB" | Web |
| `WAP` | "WAP" | Mobile web |

Source: `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/enums/TerminalType.java`.

## TransactionStatusType

Transaction status type enumeration.

| Name | Constructor argument | Source description |
| --- | --- | --- |
| `SUCCESS` | "SUCCESS" | Success |
| `FAIL` | "FAIL" | Failure |
| `PROCESSING` | "PROCESSING" | Processing |

Source: `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/enums/TransactionStatusType.java`.

## TransactionType

Transaction type enumeration.

| Name | Constructor argument | Source description |
| --- | --- | --- |
| `PAYMENT` | "PAYMENT" | Payment |
| `REFUND` | "REFUND" | Refund |
| `CAPTURE` | "CAPTURE" | Capture |
| `CANCEL` | "CANCEL" | Cancel |
| `CHARGEBACK` | "CHARGEBACK" | Chargeback |
| `CREDIT_ADJUSTMENT` | "CREDIT_ADJUSTMENT" | Credit adjustment |
| `DEBIT_ADJUSTMENT` | "DEBIT_ADJUSTMENT" | Debit adjustment |

Source: `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/enums/TransactionType.java`.
