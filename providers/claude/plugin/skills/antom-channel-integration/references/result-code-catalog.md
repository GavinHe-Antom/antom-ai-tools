# Complete Platform Business Result Codes by API

This catalog embeds the platform-designated Result Code Mapping table for offline reading; accessing the original source is not required.

Column mapping: the source channel return status becomes Platform standard status; the source secondary channel result code becomes Platform standard code. Adapters populate Result.resultStatus and Result.resultCode respectively.
Upstream mapping and transaction-state references are not displayed. Do not infer final financial outcomes from this catalog.

Markers: `*` means the source API cell is blank; the row is grouped by its contiguous section pending platform confirmation. `PD` is preserved exactly without an inferred meaning. All 438 rows retain their order and standard-code combinations. Identical two-column rows left after removing reference columns are not deduplicated again. Source channel identifiers are not the channelCode of a new integration.

### API index

+ [AUTHORIZE — 2D card payment](#codes-AUTHORIZE)
+ [INITPAYMENT — 3D card payment](#codes-INITPAYMENT)
+ [AUTHORIZEQUERY — Payment inquiry](#codes-AUTHORIZEQUERY)
+ [AUTHORIZENOTIFY — Payment notification](#codes-AUTHORIZENOTIFY)
+ [CAPTURE — Capture](#codes-CAPTURE)
+ [CAPTUREQUERY — Capture inquiry](#codes-CAPTUREQUERY)
+ [CAPTURENOTIFY — Capture notification](#codes-CAPTURENOTIFY)
+ [COMMONCANCEL — Cancellation](#codes-COMMONCANCEL)
+ [COMMONCANCELQUERY — Cancellation inquiry](#codes-COMMONCANCELQUERY)
+ [REFUND — Refund](#codes-REFUND)
+ [REFUNDQUERY — Refund inquiry](#codes-REFUNDQUERY)
+ [REFUNDNOTIFY — Refund notification](#codes-REFUNDNOTIFY)
+ [ACSURLCALLBACK — urlcallback](#codes-ACSURLCALLBACK)
+ [AUTHENTICATEAUTHORIZE — Card payment with separate authentication and authorization](#codes-AUTHENTICATEAUTHORIZE)

<a id="codes-AUTHORIZE"></a>

### AUTHORIZE — 2D card payment

| Platform standard status | Platform standard code |
| -------------- | ------------------------------------ |
| S | `SUCCESS` |
| F | `INVALID_AMOUNT` |
| F | `PARAM_ILLEGAL` |
| F | `CURRENCY_NOT_SUPPORT` |
| F | `CARD_NOT_SUPPORTED` |
| F | `PROCESS_FAIL` |
| F | `RISK_REJECT` |
| F | `USER_BALANCE_NOT_ENOUGH` |
| F | `INVALID_CARD_NUMBER` |
| F | `USER_PAYMENT_VERIFICATION_FAILED` |
| U | `UNKNOWN_EXCEPTION` |
| F | `DECLINED` |
| U | `SUBMITTED` |
| F | `AUTHENTICATION_FAILED` |
| U | `PENDING` |
| U | `AUTHENTICATION_REQUIRED` |
| F | `ABORTED` |
| F | `DECLINED_CVV` |
| F | `EXPIRED_CARD` |
| F | `TRANSACTION_NOT_SUPPORTED` |
| F | `RESTRICTED_CARD` |
| F | `TRANSACTION_NOT_PERMITTED` |
| F | `EXCEEDS_APPROVAL_AMOUNT_LIMIT` |
| F | `PICKUP_CARD` |
| F | `INVALID_MERCHANT` |
| F | `ACCOUNT_CLOSED` |
| F | `INVALID_CREDENTIALS` |
| F | `EXCEEDS_TRANSACTION_AMOUNT_LIMIT` |
| F | `EXCEEDS_FREQUENCY_LIMIT` |
| F | `DECLINED_EXPIRY_DATE` |
| F | `INVALID_CARDHOLDER_IDENTITY` |
| F | `SYSTEM_MALFUNCTION` |
| F | `DUPLICATE_REQUEST` |
| F | `ISSUER_SWITCH_INOPERATIVE` |
| F | `RISK_REJECT` |
| F | `EXPIRED_TOKEN` |
| F | `INVALID_TRANSACTION` |
| F | `INVALID_AUTHCODE` |
| F | `INVALID_SIGNATURE` |
| F | `INVALID_FORMAT` |
| F | `OVER_TPS_LIMIT` |
| F | `INVALID_REQUEST` |
| F | `LOST_CARD` |
| F | `STOLEN_CARD` |
| F | `BLOCKED` |
| F | `DECLINED_AVS` |
| F | `TIMEOUT` |
| F | `DUPLICATE_TRANSACTION` |
| F | `INVALID_TOKEN` |
| F | `TRANSACTION_CANCELLED` |
| F | `DO_NOT_HONOR` |
| F | `INVALID_EMAIL` |
| F | `INVALID_MOBILE_NUMBER` |
| F | `EXCEEDS_DAILY_AMOUNT_LIMIT` |

<a id="codes-INITPAYMENT"></a>

### INITPAYMENT — 3D card payment

| Platform standard status | Platform standard code |
| -------------- | -------------------------------------- |
| S | `SUCCESS` |
| F | `AUTHENTICATION_REQUIRED` |
| F | `INVALID_AMOUNT` |
| F | `PARAM_ILLEGAL` |
| F | `CURRENCY_NOT_SUPPORT` |
| F | `CARD_NOT_SUPPORTED` |
| F | `PROCESS_FAIL` |
| F | `RISK_REJECT` |
| F | `USER_BALANCE_NOT_ENOUGH` |
| F | `INVALID_CARD_NUMBER` |
| F | `USER_PAYMENT_VERIFICATION_FAILED` |
| U | `PAYMENT_IN_PROCESS` |
| U | `UNKNOWN_EXCEPTION` |
| U | `SUBMITTED` |
| U | `PENDING` |
| F | `INVALID_MERCHANT` |
| F | `EXCEEDS_TRANSACTION_AMOUNT_LIMIT` |
| F | `DUPLICATE_REQUEST` |
| F | `DECLINED` |
| F | `INVALID_SIGNATURE` |
| F | `INVALID_PAYMENT_CHANNEL` |
| F | `INVALID_REQUEST` |
| F | `INVALID_MERCHANT_STATUS` |
| F | `EXCEEDS_APPROVAL_AMOUNT_LIMIT` |
| F | `EXCEEDS_WITHDRAWAL_FREQUENCY_LIMIT` |
| F | `INVALID_PAYER` |
| F | `AUTHENTICATION_FAILED` |
| F | `INVALID_TRANSACTION` |
| F | `SYSTEM_MALFUNCTION` |
| F | `BLOCKED` |
| F | `DECLINED_AVS` |
| F | `DECLINED_CVV` |
| F | `INVALID_PIN` |
| F | `EXCEEDS_FREQUENCY_LIMIT` |
| F | `EXPIRED_CARD` |
| F | `TRANSACTION_NOT_PERMITTED` |
| F | `TRANSACTION_NOT_SUPPORTED` |
| F | `TIMEOUT` |
| F | `RESTRICTED_CARD` |
| F | `INVALID_FORMAT` |
| F | `PICKUP_CARD` |
| F | `3D_SECURE_NOT_ENROLLED` |
| F | `RISK_REJECT` |
| F | `DUPLICATE_TRANSACTION` |
| F | `ISSUER_SWITCH_INOPERATIVE` |
| F | `INVALID_STATUS_OF_CARD` |
| F | `INVALID_CREDENTIALS` |
| F | `DECLINED_EXPIRY_DATE` |
| F | `INVALID_EMAIL` |
| F | `INVALID_MOBILE_NUMBER` |
| F | `DO_NOT_HONOR` |
| F | `INVALID_TOKEN` |
| F | `EXPIRED_TOKEN` |
| F | `TRANSACTION_EXPIRED` |

<a id="codes-AUTHORIZEQUERY"></a>

### AUTHORIZEQUERY — Payment inquiry

| Platform standard status | Platform standard code |
| -------------- | ------------------------------------ |
| S | `APPROVED` |
| F | `INVALID_AMOUNT` |
| F | `PARAM_ILLEGAL` |
| F | `CURRENCY_NOT_SUPPORT` |
| F | `CARD_NOT_SUPPORTED` |
| F | `PROCESS_FAIL` |
| F | `RISK_REJECT` |
| F | `USER_BALANCE_NOT_ENOUGH` |
| F | `INVALID_CARD_NUMBER` |
| F | `USER_PAYMENT_VERIFICATION_FAILED` |
| U | `UNKNOWN_EXCEPTION` |
| U | `PENDING` |
| F | `ABORTED` |
| U | `AUTHENTICATION_REQUIRED` |
| U | `INVALID_MERCHANT` |
| F | `PICKUP_CARD` |
| F | `DO_NOT_HONOR` |
| U | `SUBMITTED` |
| F | `EXCEEDS_TRANSACTION_AMOUNT_LIMIT` |
| F | `INVALID_ISSUER` |
| U | `INVALID_FORMAT` |
| F | `ISSUER_SWITCH_INOPERATIVE` |
| F | `EXPIRED_CARD` |
| F | `RESTRICTED_CARD` |
| F | `ACCOUNT_CLOSED` |
| U | `TRANSACTION_NOT_SUPPORTED` |
| U | `SYSTEM_MALFUNCTION` |
| F | `DECLINED` |
| F | `TRANSACTION_NOT_PERMITTED` |
| U | `TIMEOUT` |
| F | `DECLINED_EXPIRY_DATE` |
| F | `DECLINED_CVV` |
| U | `DUPLICATE_REQUEST` |
| F | `INVALID_PAYER` |
| U | `INVALID_TRANSACTION` |
| F | `AUTHENTICATION_FAILED` |
| U | `INVALID_CURRENCY` |
| F | `ISSUER_BANK_REFUSE_PAYMENT` |
| U | `TRANSACTION_EXPIRED` |
| F | `RISK_REJECT` |
| U | `EXPIRED_TOKEN` |
| U | `INVALID_AUTHCODE` |
| U | `INVALID_SIGNATURE` |
| U | `TRANSACTION_NOT_FOUND` |
| F | `PAYER_AMOUNT_EXCEED_LIMIT` |
| U | `OVER_TPS_LIMIT` |
| U | `INVALID_REQUEST` |
| F | `LOST_CARD` |
| F | `STOLEN_CARD` |
| U | `BLOCKED` |
| F | `DECLINED_AVS` |
| F | `EXCEEDS_FREQUENCY_LIMIT` |
| F | `INVALID_STATUS_OF_CARD` |
| F | `TRANSACTION_CANCELLED` |
| U | `INVALID_TOKEN` |
| U | `INVALID_CREDENTIALS` |
| U | `INVALID_EMAIL` |
| U | `INVALID_CARD_NUMBER` |
| U | `INVALID_MOBILE_NUMBER` |

<a id="codes-AUTHORIZENOTIFY"></a>

### AUTHORIZENOTIFY — Payment notification

| Platform standard status | Platform standard code |
| -------------- | ------------------------------------ |
| S | `SUCCESS` |
| F | `INVALID_AMOUNT` |
| F | `PARAM_ILLEGAL` |
| F | `CURRENCY_NOT_SUPPORT` |
| F | `CARD_NOT_SUPPORTED` |
| F | `PROCESS_FAIL` |
| F | `RISK_REJECT` |
| F | `USER_BALANCE_NOT_ENOUGH` |
| F | `INVALID_CARD_NUMBER` |
| F | `USER_PAYMENT_VERIFICATION_FAILED` |
| U | `PENDING` |
| U | `AUTHENTICATION_REQUIRED` |
| U | `UNKNOWN` |
| F | `INVALID_MERCHANT` |
| F | `PICKUP_CARD` |
| F | `DO_NOT_HONOR` |
| F | `EXCEEDS_TRANSACTION_AMOUNT_LIMIT` |
| F | `INVALID_ISSUER` |
| F | `INVALID_FORMAT` |
| F | `ISSUER_SWITCH_INOPERATIVE` |
| F | `EXPIRED_CARD` |
| F | `RESTRICTED_CARD` |
| F | `ACCOUNT_CLOSED` |
| F | `TRANSACTION_NOT_SUPPORTED` |
| F | `DECLINED` |
| F | `TRANSACTION_NOT_PERMITTED` |
| F | `DECLINED_EXPIRY_DATE` |
| F | `DECLINED_CVV` |
| F | `INVALID_PAYER` |
| F | `ISSUER_BANK_REFUSE_PAYMENT` |
| F | `RISK_REJECT` |
| F | `TRANSACTION_CANCELLED` |
| F | `DECLINED_AVS` |

<a id="codes-CAPTURE"></a>

### CAPTURE — Capture

| Platform standard status | Platform standard code |
| -------------- | ------------------------------------ |
| S | `SUCCESS` |
| F | `INVALID_AMOUNT` |
| F | `PARAM_ILLEGAL` |
| F | `CURRENCY_NOT_SUPPORT` |
| F | `PROCESS_FAIL` |
| F | `RISK_REJECT` |
| F | `USER_BALANCE_NOT_ENOUGH` |
| U | `UNKNOWN_EXCEPTION` |
| F | `INVALID_REQUEST` |
| F | `INVALID_MERCHANT` |
| F | `ORDER_STATUS_INVALID` |
| F | `INVALID_SIGNATURE` |
| F | `EXCEEDS_TRANSACTION_AMOUNT_LIMIT` |
| F | `DECLINED` |
| F | `DUPLICATE_REQUEST` |
| F | `GENERAL_ERROR` |
| F | `TRANSACTION_NOT_PERMITTED` |
| F | `INVALID_CREDENTIALS` |

<a id="codes-CAPTUREQUERY"></a>

### CAPTUREQUERY — Capture inquiry

| Platform standard status | Platform standard code |
| -------------- | ------------------------------------ |
| S | `SUCCESS` |
| F | `INVALID_AMOUNT` |
| F | `PARAM_ILLEGAL` |
| F | `CURRENCY_NOT_SUPPORT` |
| F | `PROCESS_FAIL` |
| F | `RISK_REJECT` |
| F | `USER_BALANCE_NOT_ENOUGH` |
| U | `UNKNOWN_EXCEPTION` |
| F | `DECLINED` |
| U | `INVALID_REQUEST` |
| U | `INVALID_TRANSACTION` |
| U | `SYSTEM_MALFUNCTION` |
| U | `BLOCKED` |
| F | `DECLINED_AVS` |
| F | `DECLINED_CVV` |
| F | `INVALID_PIN` |
| F | `DUPLICATE_TRANSACTION` |
| F | `EXCEEDS_FREQUENCY_LIMIT` |
| F | `EXPIRED_CARD` |
| F | `TRANSACTION_NOT_PERMITTED` |
| F | `TRANSACTION_NOT_SUPPORTED` |
| U | `TIMEOUT` |
| U | `TRANSACTION_NOT_FOUND` |
| F | `INVALID_CARD_NUMBER` |
| F | `RESTRICTED_CARD` |
| F | `DECLINED_EXPIRY_DATE` |
| U | `INVALID_TOKEN` |
| U | `INVALID_MERCHANT` |
| F | `TRANSACTION_EXPIRED` |
| U | `EXPIRED_TOKEN` |
| F | `TRANSACTION_CANCELLED` |
| F | `AUTHENTICATION_FAILED` |
| F | `INVALID_STATUS_OF_CARD` |
| F | `ISSUER_SWITCH_INOPERATIVE` |
| F | `RISK_REJECT` |
| U | `DUPLICATE_REQUEST` |
| U | `INVALID_CREDENTIALS` |
| F | `EXCEEDS_TRANSACTION_AMOUNT_LIMIT` |
| F | `INVALID_MOBILE_NUMBER` |
| U | `INVALID_SIGNATURE` |

<a id="codes-CAPTURENOTIFY"></a>

### CAPTURENOTIFY — Capture notification

| Platform standard status | Platform standard code |
| -------------- | ------------------------------------ |
| S | `SUCCESS` |
| F | `INVALID_AMOUNT` |
| F | `CURRENCY_NOT_SUPPORT` |
| F | `PROCESS_FAIL` |
| F | `RISK_REJECT` |
| F | `USER_BALANCE_NOT_ENOUGH` |
| F | `INVALID_PIN` |
| F | `EXPIRED_CARD` |
| F | `DECLINED` |
| F | `INVALID_PAYER` |
| F | `DECLINED_CVV` |
| F | `INVALID_MERCHANT` |
| F | `INVALID_TRANSACTION` |
| F | `SYSTEM_MALFUNCTION` |
| F | `PICKUP_CARD` |
| F | `INVALID_ISSUER` |
| F | `EXCEEDS_PIN_TRIES` |
| F | `RESTRICTED_CARD` |
| F | `TRANSACTION_NOT_PERMITTED` |
| F | `TRANSACTION_NOT_SUPPORTED` |
| F | `DECLINED_AVS` |
| F | `EXCEEDS_FREQUENCY_LIMIT` |
| F | `ISSUER_SWITCH_INOPERATIVE` |
| F | `EXCEEDS_TRANSACTION_AMOUNT_LIMIT` |
| F | `DUPLICATE_TRANSACTION` |
| F | `INVALID_STATUS_OF_CARD` |
| F | `AUTHENTICATION_FAILED` |
| F | `DECLINED_EXPIRY_DATE` |
| F | `TRANSACTION_NOT_FOUND` |
| F | `TRANSACTION_EXPIRED` |
| F | `TRANSACTION_CANCELLED` |

<a id="codes-COMMONCANCEL"></a>

### COMMONCANCEL — Cancellation

| Platform standard status | Platform standard code |
| -------------- | ----------------------------- |
| S | `SUCCESS` |
| F | `PARAM_ILLEGAL` |
| F | `RISK_REJECT` |
| F | `PROCESS_FAIL` |
| U | `UNKNOWN_EXCEPTION` |
| F | `INVALID_REQUEST` |
| F | `INVALID_MERCHANT` |
| F | `ORDER_STATUS_INVALID` |
| F | `INVALID_SIGNATURE` |
| F | `DECLINED` |
| U | `DUPLICATE_REQUEST` |
| F | `INVALID_AMOUNT` |
| F | `TRANSACTION_NOT_PERMITTED` |
| F | `INVALID_CURRENCY` |
| F | `SYSTEM_MALFUNCTION` |
| F | `TRANSACTION_NOT_FOUND` |
| F | `TRANSACTION_NOT_SUPPORTED` |
| U | `TIMEOUT` |
| F | `INVALID_FORMAT` |
| F | `EXCEEDS_REFUNDABLE_AMOUNT` |
| F | `NO_REFUNDABLE_PAYMENT` |
| F | `EXCEEDS_REFUNDABLE_PERIOD` |

<a id="codes-COMMONCANCELQUERY"></a>

### COMMONCANCELQUERY — Cancellation inquiry

| Platform standard status | Platform standard code |
| -------------- | ----------------------------- |
| S | `SUCCESS` |
| F | `PARAM_ILLEGAL` |
| F | `RISK_REJECT` |
| F | `PROCESS_FAIL` |
| U | `UNKNOWN_EXCEPTION` |
| F | `DECLINED` |
| U | `INVALID_REQUEST` |
| F | `INVALID_TRANSACTION` |
| U | `SYSTEM_MALFUNCTION` |
| F | `TRANSACTION_NOT_PERMITTED` |
| F | `TRANSACTION_NOT_SUPPORTED` |
| U | `TIMEOUT` |
| F | `PENDING` |
| U | `TRANSACTION_NOT_FOUND` |
| U | `INVALID_CREDENTIALS` |
| U | `INVALID_SIGNATURE` |
| F | `EXCEEDS_REFUNDABLE_AMOUNT` |

<a id="codes-REFUND"></a>

### REFUND — Refund

| Platform standard status | Platform standard code |
| -------------- | ----------------------------- |
| S | `SUCCESS` |
| F | `PARAM_ILLEGAL` |
| F | `PROCESS_FAIL` |
| F | `REFUND_AMOUNT_EXCEED` |
| F | `CURRENCY_NOT_SUPPORT` |
| U | `UNKNOWN_EXCEPTION` |
| F | `INVALID_MERCHANT` |
| F | `ORDER_STATUS_INVALID` |
| F | `INVALID_SIGNATURE` |
| F | `INVALID_REQUEST` |
| F | `PART_REFUND_NOT_SUPPORT` |
| U | `DUPLICATE_REQUEST` |
| F | `EXCEEDS_REFUNDABLE_AMOUNT` |
| F | `DECLINED` |
| F | `REFUND_NOT_SUPPORT` |
| U | `PENDING` |
| F | `INVALID_TRANSACTION` |
| F | `EXCEEDS_REFUNDABLE_PERIOD` |
| F | `INVALID_CREDENTIALS` |
| F | `TRANSACTION_NOT_PERMITTED` |
| F | `TRANSACTION_NOT_FOUND` |
| F | `TRANSACTION_NOT_SUPPORTED` |
| F | `INVALID_AMOUNT` |
| F | `OVER_TPS_LIMIT` |
| U | `SYSTEM_MALFUNCTION` |
| U | `TIMEOUT` |

<a id="codes-REFUNDQUERY"></a>

### REFUNDQUERY — Refund inquiry

| Platform standard status | Platform standard code |
| -------------- | ----------------------------- |
| S | `SUCCESS` |
| F | `PARAM_ILLEGAL` |
| F | `PROCESS_FAIL` |
| F | `REFUND_AMOUNT_EXCEED` |
| F | `CURRENCY_NOT_SUPPORT` |
| U | `UNKNOWN_EXCEPTION` |
| U | `INVALID_REQUEST` |
| U | `INVALID_MERCHANT` |
| U | `INVALID_SIGNATURE` |
| F | `DECLINED` |
| F | `REFUND_NOT_SUPPORT` |
| U | `PENDING` |
| U | `TRANSACTION_NOT_FOUND` |
| U | `SYSTEM_MALFUNCTION` |
| U | `INVALID_CREDENTIALS` |
| F | `TRANSACTION_NOT_PERMITTED` |
| F | `EXCEEDS_REFUNDABLE_AMOUNT` |
| F | `EXCEEDS_REFUNDABLE_PERIOD` |
| U | `OVER_TPS_LIMIT` |
| U | `TIMEOUT` |

<a id="codes-REFUNDNOTIFY"></a>

### REFUNDNOTIFY — Refund notification

| Platform standard status | Platform standard code |
| -------------- | ----------------------------- |
| S | `SUCCESS` |
| F | `PARAM_ILLEGAL` |
| F | `PROCESS_FAIL` |
| F | `REFUND_AMOUNT_EXCEED` |
| F | `CURRENCY_NOT_SUPPORT` |
| F | `TRANSACTION_NOT_PERMITTED` |
| U | `PENDING` |
| F | `DECLINED` |
| F | `EXCEEDS_REFUNDABLE_AMOUNT` |
| F | `EXCEEDS_REFUNDABLE_PERIOD` |
| F | `RISK_REJECT` |

<a id="codes-ACSURLCALLBACK"></a>

### ACSURLCALLBACK — urlcallback

| Platform standard status | Platform standard code |
| -------------- | --------------- |
| F | `AUTHORISING` |

<a id="codes-AUTHENTICATEAUTHORIZE"></a>

### AUTHENTICATEAUTHORIZE — Card payment with separate authentication and authorization

| Platform standard status | Platform standard code |
| -------------- | ------------------------------------ |
| S | `SUCCESS` |
| F | `INVALID_AMOUNT` |
| F | `PARAM_ILLEGAL` |
| F | `CURRENCY_NOT_SUPPORT` |
| F | `CARD_NOT_SUPPORTED` |
| F | `PROCESS_FAIL` |
| F | `USER_BALANCE_NOT_ENOUGH` |
| F | `USER_PAYMENT_VERIFICATION_FAILED` |
| U | `UNKNOWN_EXCEPTION` |
| F | `DECLINED` |
| U | `SUBMITTED` |
| F | `AUTHENTICATION_FAILED` |
| U | `PENDING` |
| F | `ABORTED` |
| F | `DECLINED_CVV` |
| F | `EXPIRED_CARD` |
| F | `TRANSACTION_NOT_SUPPORTED` |
| F | `RESTRICTED_CARD` |
| F | `TRANSACTION_NOT_PERMITTED` |
| F | `EXCEEDS_APPROVAL_AMOUNT_LIMIT` |
| F | `PICKUP_CARD` |
| F | `INVALID_MERCHANT` |
| F | `ACCOUNT_CLOSED` |
| F | `INVALID_CREDENTIALS` |
| F | `INVALID_CARD_NUMBER` |
| F | `EXCEEDS_TRANSACTION_AMOUNT_LIMIT` |
| F | `EXCEEDS_FREQUENCY_LIMIT` |
| F | `DECLINED_EXPIRY_DATE` |
| F | `INVALID_CARDHOLDER_IDENTITY` |
| F | `SYSTEM_MALFUNCTION` |
| F | `DUPLICATE_REQUEST` |
| F | `ISSUER_SWITCH_INOPERATIVE` |
| F | `RISK_REJECT` |
| F | `EXPIRED_TOKEN` |
| F | `INVALID_TRANSACTION` |
| F | `INVALID_AUTHCODE` |
| F | `INVALID_SIGNATURE` |
| F | `INVALID_FORMAT` |
| F | `OVER_TPS_LIMIT` |
| F | `INVALID_REQUEST` |
| F | `LOST_CARD` |
| F | `STOLEN_CARD` |
| F | `BLOCKED` |
| F | `DECLINED_AVS` |
| F | `TIMEOUT` |
| F | `DUPLICATE_TRANSACTION` |
| F | `INVALID_TOKEN` |
| F | `TRANSACTION_CANCELLED` |
| F | `DO_NOT_HONOR` |
| F | `INVALID_EMAIL` |
| F | `INVALID_MOBILE_NUMBER` |
| F | `EXCEEDS_DAILY_AMOUNT_LIMIT` |
