# Platform Standard Results and Result-Code Mapping

## Distinguish three result layers

In the platform-supplied table, the channel return status and secondary channel result code are platform-standard results, not raw external-institution codes.

| Layer | Example | Development role |
| --- | --- | --- |
| Raw institution result | Institution primary/secondary/tertiary codes and message | Extracted by adapter as ResultCodeService.mapping input |
| Platform standard result | Status S + secondary code SUCCESS | Result.resultStatus=S, Result.resultCode=SUCCESS |
| Further upstream mapping | Source table standard upstream code FLUXNET_APPROVED and transaction state SUCCESS | Consumed by upstream contracts, not the value to put in Result.resultCode |

```text
Institution raw primary/secondary codes, such as its code/subCode
    → Parameter center: raw institution code + channelCode + API scope
    → Platform Result { resultStatus, resultCode, resultMessage }
    → Upstream contract maps further to FLUXNET_* and other results
```

Configuration names can be misleading: AisResultCodeConfig.channelResultCode stores the **raw institution code or combined code**. aisResultStatus and aisResultCode store target S/F/U and standard business code respectively.
Do not put the source table's platform return status into configuration channelResultCode.

## Using the standard catalog

The [standard code catalog](result-code-catalog.md) embeds the complete business-standard combinations grouped by source API. Its source is the platform-designated Result Code Mapping table, updated 2026-08-07T07:51:50Z. The platform owner explicitly designated its channel return status/secondary code columns as platform standards.

The source review read two sheets, 494 records with standard status and 79 secondary-code names, consolidated into 438 source combinations.
The catalog displays only platform standard status and code, retaining those 438 rows in source order. Duplicate two-column entries left after removing reference columns are not deduplicated again.

**Select by API/business scenario + resultStatus + resultCode, not by reusing one English code globally.**

| Example | Source convention | Caution |
| --- | --- | --- |
| AUTHORIZE success | S / SUCCESS | Result.success() generates this combination |
| AUTHORIZEQUERY success | S / APPROVED | Result.success() always produces SUCCESS; do not reuse blindly |
| INITPAYMENT needs authentication | F / AUTHENTICATION_REQUIRED; upstream ENROLLED | Not every F means final financial failure |
| AUTHORIZE needs authentication | U / AUTHENTICATION_REQUIRED | Different from the preceding scenario |
| AUTHORIZE TIMEOUT | F / TIMEOUT | Business mapping does not mean every network timeout can return this |
| REFUNDQUERY TIMEOUT | U / TIMEOUT | Query did not determine the result; do not change it to failure |
| ACSURLCALLBACK | F / AUTHORISING; upstream ACS_VERIFIED | Does not require the adapter to create another ACS flow |

Source API names, Java methods and parameter-center api may differ. SDK authenticateAuthorize uses route key authorize, source-table name AUTHENTICATEAUTHORIZE, and possibly a full contract name in parameter-center api.
Document and confirm the relationships at delivery; do not rename automatically or merely change case.

The table primarily covers card interactions. Ask the platform to confirm combinations for non-card or uncovered scenarios; do not extend standard codes yourself.
CAPTUREQUERY and COMMONCANCELQUERY are source API names without separate identically named SDK methods. If inquiryPayment plus transactionType is required, confirm it in the capability inventory; the table alone does not establish enablement.

## Source items requiring confirmation

Retain original values; this guide does not choose between them:

| Item | Difference | Integration action |
| --- | --- | --- |
| AUTHORIZEQUERY / INVALID_CARD_NUMBER | Same API includes both F → FAIL and U → UNKNOWN | Platform confirms applicable combination and trigger |
| RISK_REJECT across APIs | F maps upstream to FLUXNET_SUSPECTED_FRAUD or FLUXNET_RISK_REJECT | Confirm upstream mapping, not just standard secondary code |
| INITPAYMENT / PAYMENT_IN_PROCESS | Upstream IPAY_RS_510950108; source transaction state blank | Do not infer state; platform confirms consumption |
| Blank source API cells | Some contiguous rows have codes without explicit API | Catalog groups under preceding API section and marks *; verify ownership before release |
| Source PD marker | No explicit definition supplied | Preserve it; do not interpret as automatic enablement or deployed status |
| Source channel column | Contains example/existing institution identifiers | Do not copy as another institution's channelCode; use assigned identity |

Accepted, awaiting authentication and processing do not always map one-to-one to S/F/U or upstream transaction states. Neither an English code name nor S/F/U alone establishes final financial outcome.

## Calling the platform mapper

```java
Result mapping(String channelCode, String channelResultCode,
               String channelResultMsg, String api);

Result mapping(String channelCode, String channelResultCode,
               String secondResultCode, String channelResultMsg, String api);

Result mapping(String channelCode, String channelResultCode,
               String secondResultCode, String thirdResultCode,
               String channelResultMsg, String api);
```

| Parameter/result | Rule |
| --- | --- |
| channelCode | Platform-provided identity, not replaced with an institution-returned value |
| channelResultCode / secondResultCode / thirdResultCode | Raw institution codes; preserve leading zeros/case; do not supply target platform codes |
| channelResultMsg | Safe institution description; do not append keys, card numbers or full payloads |
| api | Parameter-center-confirmed contract scope, not implicitly the Java method name |
| Returned Result | Configured aisResultStatus/aisResultCode; matched results retain institution message |

Multi-level codes are joined with pipes into one lookup, such as primary|secondary|tertiary; they do not produce three separately mapped Results.
Record both original levels and the actual combined value. Do not pass target S and SUCCESS as two institution-code levels.

With no matching configuration or a query result indicating failure, the platform returns U / UNKNOWN_EXCEPTION. Actual thrown query exceptions may propagate.
Matches are filtered by API scope. Avoid overlapping wildcard/specific configurations with conflicting meanings; do not assume specific entries automatically win.

The CLI generates neither a universal ChannelResultMapper nor example success codes. Implement confirmed rules in each Mapping; inject ResultCodeService with test mocks when needed.
For a query requiring APPROVED, do not call Result.success(), which always returns SUCCESS.

## Framework error codes are separate from institution business codes

| resultCode | resultStatus | Trigger/meaning | Developer action |
| --- | --- | --- | --- |
| SUCCESS | S | SDK Result.success() | Use only when this combination is required |
| UNKNOWN_EXCEPTION | U | Institution-code mapping missing or query result reports failure | Complete mapping/investigate configuration; do not fabricate success |
| SPI_NOT_REGISTERED | F | SPI implementation unavailable | Check beans, module loading and capabilities |
| SPI_METHOD_NOT_REGISTERED | F | Method outside enabled channel scope | Compare platform registration |
| CHANNEL_NOT_AVAILABLE | F | No available channel module | Platform checks module status and channelCode |
| INVALID_REQUEST | F | Controller IllegalArgumentException handled centrally | Check input; same name as a business code, different trigger |
| SYSTEM_ERROR | U or F | Facade unknown exceptions yield U; unhandled Web exceptions yield F | Handle actual result/business state; do not universally infer transaction failure |

Framework codes do not describe every institution rejection. Response.result is not HTTP status. Query fields such as refundStatus and transactions[].transactionResult must also follow their own contracts.
