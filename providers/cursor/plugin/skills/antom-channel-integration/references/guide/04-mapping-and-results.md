# Field Mapping, Business Results and Error Handling

## 1. Where mapping belongs

`spi/*Service` is the SDK entry point. Each transaction method's anonymous `ChannelApiExtension` converts between standard and institution fields; each notification method has an anonymous `ChannelNotificationExtension`. The CLI does not generate `customize/api` helper classes. `ChannelInvocationTemplate` orchestrates validation, security, platform HTTP and response conversion. Do not put institution-specific field rules in controllers, platform routing classes or SDK DTOs.

Maintain four verifiable artifacts per operation: standard-request-to-institution-request mapping, institution-response-to-standard-response mapping, success/failure/processing result-code rules, and positive/negative test fixtures.
Maintain a separate institution-notification-to-standard-notification-request mapping; do not infer it from synchronous response fields.

## 2. Document mappings before implementation

Keep channel mappings in an independent `docs/channel-mapping/` directory, one file per method.

| Standard field path | Institution field path | Direction | Condition/default | Conversion | Validation/error policy | Evidence |
| --- | --- | --- | --- | --- | --- | --- |
| paymentRequestId | To be supplied | Request | Institution protocol | Original value or explicit rule | Whether absence is rejected | Protocol version/section |
| paymentAmount.value | To be supplied | Request | Paired with currency | Minor currency units to institution units | No silent rounding | Protocol and test vectors |
| result.resultStatus | Institution status field | Response | Success/failure/processing | Agreed result-code mapping | Unknown must not become success | Platform result-code configuration |
| paymentId | Institution transaction ID | Response | Actual response | Do not substitute request ID | Contract-specific missing-value handling | Institution example |

This is a table structure, not invented institution fields or requiredness rules. If SDK JavaDoc, institution protocol and confirmed business contracts conflict, ask the platform; do not silently reconcile them into different semantics.

## 3. Outbound request implementation order

1. In `validate`, check fields actually required by the operation. Distinguish null objects, empty strings and absent values. Errors should identify the field without including complete card data.
2. In `mapRequestBody`, construct the institution payload while preserving amount, time and identifier semantics. Do not serialize the entire SDK request directly to the institution.
3. Return dynamic-path business values from `mapUrlParameters`; the platform still owns the domain and path template.
4. Use transport customization for institution-approved headers, Query, Form and HTTP method.
5. Compute security values from the exact content that will be sent. Recompute digests/signatures after changing the body.
6. After platform transport, verify/decrypt according to the protocol before mapping the readable body into a standard response.

`executeFixedUrl` does not consume dynamic-path parameters. The standard template requires a non-null JSONObject from `mapRequestBody`. For non-JSON protocols, read the [HTTP guide](06-routing-and-http.md) to determine necessary template changes.

## 4. Key field rules

| Category | SDK fact | Integration rule |
| --- | --- | --- |
| Amounts | Amount.currency and Amount.value are String; value represents minor currency units | Convert according to currency and protocol using exact decimal arithmetic, not float/double |
| Identifiers | paymentRequestId is distinct from paymentId; refundRequestId from refundId | Request identifiers and transaction identifiers are not interchangeable |
| Time | Most fields are String | Java types do not establish format or timezone; confirm the contract before conversion |
| Boolean/mode | PaymentMethodMetadata.is3DSAuthentication is Boolean; PaymentFactor.isAuthorization is String | Do not infer types from names or conflate authentication and authorization |
| Extension fields | extendInfo is String in several DTOs; PayRequest has no such field | Do not invent DTO fields; confirm the inner JSON structure if the string carries JSON |
| Merchant | Merchant under Order.merchant has no merchantId field | Security requests use ChannelRequestContext.current().getMerchantId(); do not invent business DTO getters |
| Routing fields | Platform populates BaseChannelRequest.domain/path/channelCode | Do not emit them as institution business body fields or overwrite them |
| Request headers | requestHeaders contains inbound information, not a default outbound allowlist | Select headers explicitly per institution protocol; do not forward the entire map |
| Callback URLs | Redirect and notification URLs are business mapping values, not institution API egress routes | Preserve the complete confirmed URL, including query parameters such as isSandbox; do not add sandbox recognition or route selection to the adapter |

See [model references](reference/models/README.md) for nested fields. A table entry saying that code does not declare a requirement does not mean the business may omit it.

## 5. Responses and result codes

`BaseResponse.result` is the platform-standard result. `Result` contains resultStatus, resultCode and resultMessage; S/F/U mean success, failure and unknown.
Transaction lifecycle information also appears in `InquiryRefundResponse.refundStatus`, `Transaction.transactionResult` and similar fields. HTTP 200 alone must not produce Result.success, and acceptance must not be assumed to be a final transaction state.

CLI anonymous extension hooks do not prescribe institution success codes or generate a universal ChannelResultMapper. Implement response mapping per API:

- Only confirmed institution success criteria may produce the corresponding standard success combination.
- When platform mapping is needed, inject ResultCodeService, call mapping(channelCode, code, message, apiName), and register a matching mock in tests.
- Handle null, unknown institution codes and platform query failures explicitly; do not fabricate success.
- The platform fallback for a missing mapping is unknown, not an institution success code.

Confirm distinctions between unknown, processing, declined payments and technical exceptions with the platform configuration owner. A payment declined by an institution is a payment-failure result, not the unsupported dispute service.
`ResultCodeService` also offers multi-level result-code overloads; see the [platform API reference](reference/http.md). Use them only when the institution actually provides hierarchical codes.

## 6. Non-2xx, empty responses and exceptions

Platform HTTP preserves non-2xx responses. The CLI template accepts only 2xx by default; when the confirmed protocol returns business codes in other statuses, override `acceptResponseStatus` in that SPI method's anonymous ChannelApiExtension. Accepted bodies still pass through response security before mapping; accepting HTTP status does not imply business success. Add non-2xx cases and verify actual result-code mapping rather than guessing another institution's policy.

Do not hide problems by:

- Catching all exceptions and returning success, an empty Result or null.
- Continuing business-field mapping after signature verification fails.
- Discarding the original institution error code and retaining only a generic exception message.
- Placing complete institution bodies in extendInfo and forwarding sensitive content upstream without confirmation.

`ChannelHttpResult.rawBody` is the original response text; `body` is text after security processing. Use the appropriate version for mapping and the protocol-required original content for verification.

## 7. Example validation is not standard requiredness

CLI validate hooks provide only null-request protection and unfinished implementation hooks; they do not implement business-required field checks automatically. Implement these checks from each method page and the confirmed institution contract. Do not reuse one operation's query criteria for another.
Field references distinguish source types from business rules; template examples are not universal protocols.

Evidence: CLI SPI templates and anonymous extension hooks, ChannelInvocationTemplate, SDK Result/Amount and request/response classes.
