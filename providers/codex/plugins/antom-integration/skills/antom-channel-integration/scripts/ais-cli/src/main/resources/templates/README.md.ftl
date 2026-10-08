# [=artifactId]

Channel: [=channelCode]. SDK: [=sdkVersion] (provided). Generator template: [=templateVersion].

## Start development

1. `spi/ChannelPaymentService.java`, `ChannelRefundService.java` and `ChannelNotificationService.java`: implement each selected method's anonymous extension inline. Transaction methods contain validation, request/response mapping and path placeholders; notification methods contain plaintext validation/mapping after verification/decryption. No separate `*Mapping.java` classes or `customize/api` directory are generated.
2. `customize/transport/ChannelTransportCustomization.java`: configure GET/POST, headers, query parameters and similar protocol details. Do not configure domains or connection settings, or create another HTTP client.
3. `customize/security/ChannelSecurityCustomization.java`: implement the selected canonical text and result placement. The two signature/encryption choices create `demo` platform-service examples without choosing an algorithm or declaring a completed protocol. These hooks throw until implemented. Choose platform computation or custom calculation after reviewing the institution protocol; custom calculation obtains material through platform queryKey and uses a mature library. Follow the protocol's actual operation order and stop on security failure.
4. `src/test/resources/scenarios/<method>/<id>.json`: prepare independent protocol cases before implementing the mapper. Adding a confirmed case automatically adds a test invocation. Mock platform boundaries; do not call real institutions or use production secrets.
5. `src/test/java/*DeliveryTest.java`: execute the real SPI, inline mapping and security/transport hooks. Assert exact HTTP properties, typed security inputs, result-code mapping inputs, final results and failure boundaries. Include applicable success/rejection/processing/input/security/HTTP cases. Do not generate expected results from the code under test or remove assertions to pass unfinished code.
6. `ais package --project .`: resolve dependencies, verify the actual SDK JAR/POM, run all tests and inspect the ordinary JAR. Initial delivery-test failures are expected: unfinished business behavior must not be reported as passing.

You may first run `mvn -Dtest=GeneratedStructureTest test` to check structural wiring; this is not delivery acceptance. Use Java 8 / Maven 3.6.3+.

## Test layers and independent fixtures

- `GeneratedStructureTest`: exactly one bean per selected SPI family and the exact selected SDK method signatures. Private protocol helpers and supported dependencies such as `ResultCodeService` are allowed; the test host supplies a mock result-code service.
- `TemplateRuntimeTest`: bodyless and raw-text requests, exact text passed to HTTP, response-status policy and security failure boundaries. These are framework checks, not institution acceptance.
- `*SecurityContractTest`: synthetic hooks exercise the generated security-step order, merchant/algorithm/content/key-purpose/runtime-parameter delegation, abort on security exceptions or false platform verification, and use of the processed ciphertext in HTTP. These tests do not implement institution protocols or execute real cryptographic algorithms. Fixed test sentinels are not production IVs, signatures or keys.
- `*DeliveryTest`: execute real inline mapping and security/transport hooks through SPI for every confirmed case. Empty/unconfirmed fixtures, malformed data and unfinished hooks must fail, including in expected-failure cases. Mocked computation results prove delegation, not institution algorithm compatibility. Custom computation requires independent valid/invalid signature or encryption vectors.

Security contract tests use synthetic subclass hooks and do not require the real customization to remain unfinished. Completing the real hooks must leave those wiring tests passing; delivery fixtures and protocol-specific tests establish the implemented behavior. Keep the same HTTP, identity and fail-closed security boundaries when adapting template assumptions for a confirmed protocol.

Each `src/test/resources/scenarios/<method>/` contains one JSON file per case. For example,
`success.json`, `business-rejected.json` and `invalid-signature.json`. The generated `success.json`
is explicitly unconfirmed; complete it from the protocol before setting `confirmed` to true.
That flag records a developer decision, not platform or institution approval.

| Case field | Contents |
| --- | --- |
| `id`, `category`, `confirmed` | Unique lowercase ID matching the filename; category is success, business-failure, processing, validation-failure, security-failure, http-failure, mapping-failure, identity or protocol |
| `input` | Platform-standard request; notifications include synthetic raw messages and headers. Explicit null is allowed for missing-input cases |
| `context` | Independent host identity: present, channelCode, merchantId and runtimeEnv (explicit string/null). Never derive identity from input or raw notification content |
| `http` | Expected calls (0 or 1), plus response with statusCode/headers/body, or an exception. Notifications use calls:0 |
| `expectedRequest` | When HTTP is called: final method, pathParameters, headers, queryParameters, formParameters, contentType (SDK enum name or null), attributes and exact body text/null |
| `security` | One object per selected step, containing calls, exact SDK request, result or exception. A skipped step explicitly declares calls:0; use {} when all steps are none |
| `resultCode` | Expected mapping calls with exact arguments and SDK Result/exception; [] forbids any result-code service call |
| `expectedResult` or `expectedException` | Exactly one expected outcome. Exceptions require exact type/message; unfinished UnsupportedOperationException is never a valid business outcome |

The `expectedRequest` structure is shown below. Values must come from the confirmed protocol,
and map fields must be explicit. {} and null differ. Body is exact text, not a JSON object;
null means an absent body. For byte-sensitive messages, use bodyFile naming a sibling .txt
instead of body. Dynamic-path assertions check only placeholder parameters passed to the
platform. Domains, path templates and final network encoding remain platform responsibilities.

```json
{
  "method": "<confirmed method>",
  "pathParameters": {},
  "headers": {},
  "queryParameters": {},
  "formParameters": {},
  "contentType": "<SDK ContentType enum name>",
  "attributes": {},
  "body": "<exact text supplied to platform HTTP>"
}
```

Security boundary requests use SDK field names, including merchantId, algorithm, content,
signature and applicable parameters. Verification results may be true or false; other boundaries
may return results or throw configured exceptions. Key-query results contain test material only.
Calls on unselected steps or extra HTTP/security/mapping calls fail the test. Multi-call protocols,
custom exceptions and specialized assertions can use ordinary JUnit tests instead of expanding
the fixture format into a workflow language.

## Actual security compatibility

For platform computation, local mocks validate input assembly and output placement only.
Run the platform-host conformance test with the actual adapter JAR, real platform security
service and synthetic key lookup to validate deployed-library compatibility. No platform
implementation source needs to be copied into this project.

For adapter computation, mock queryKey only and execute the real adapter algorithm using
independent synthetic vectors. Verify deterministic signatures against known expected values;
verify randomized signatures/ciphertexts with the correct peer public/private key instead of
requiring identical outputs. Platform signing uses the platform private key, while verification
uses the institution public key; encryption uses the institution public key and decryption uses
the platform private key. Do not assume sign/verify or encrypt/decrypt select the same key pair.

## Platform request context

All generated sign, verify, encrypt, decrypt and queryKey requests obtain `merchantId`
from `ChannelRequestContext.current().getMerchantId()`. The wrapper resolves it before
calling protocol hooks and overwrites any hook-supplied merchant. Implement payload,
signature, runtime cipher options and result placement only; do not implement merchant
selection or copy business fields into trusted identity.

Production adapter code may read `current()`, `getChannelCode()`, `getMerchantId()` and
`getRuntimeEnv()`. Only the platform host binds the context and clears it in `finally`.
Missing context or merchant fails closed; `runtimeEnv` may be null and must not default
to a test environment. Identity is synchronous and is not inherited by asynchronous work.

Delivery tests simulate this host lifecycle with each case's independent context values.
Security contract tests cover missing identity, hook identity overrides and same-thread
cleanup. Structure tests compile the SDK's `BaseChannelRequest.runtimeEnv` accessors/builder
and `PaymentNotifyRequest.extendInfo` accessors/builder; use the notification builder,
not a field-count-dependent all-arguments constructor. Map `extendInfo` only when the
confirmed notification protocol supplies it; the field does not carry trusted identity.

| SPI | Security step | SDK request and result |
| --- | --- | --- |
<#list capabilities as cap><#list cap.directions as direction><#list security[cap.method][direction] as step><#if step.implementation != "none">
<#assign id=direction+step.operation?cap_first>
<#if step.implementation == "adapter">
| [=cap.method] | `[=id]` | request: SecurityKeyRequest; result: synthetic SecurityKeyMaterial |
<#else>
| [=cap.method] | `[=id]` | request: Security[=step.operation?cap_first]Request; result: <#if step.operation == "verify">boolean<#else>string</#if> |
</#if>
</#if></#list></#list></#list>

## Selected capabilities

<#list capabilities as cap>
<#assign service="ChannelPaymentService"><#if cap.family == "refund"><#assign service="ChannelRefundService"></#if><#if cap.family == "notify"><#assign service="ChannelNotificationService"></#if>
- `[=cap.method]` → `spi/[=service].java`, inside `[=cap.method](...)`.
</#list>

Only selected methods are emitted. `refund` and `inquiryRefund` require explicit selection;
selecting `notifyRefund` does not enable either transaction SPI.

The template uses the platform's dynamic URL API. Pass an empty map when a path has no placeholders.
All URLs come from the platform; use its SDK route view when signatures require the actual target.
For no body, return null from mapRequestBody or use setRawBody(null). For exact protocol text or
raw ciphertext, use ChannelOutboundRequest.setRawBody. Use getSerializedBody for header signing
and digests, keeping the final payload unchanged afterward unless the protocol says otherwise.
Override the anonymous extension's acceptResponseStatus only when the institution defines
business responses outside 2xx. Accepted error bodies still undergo security processing before
mapping; status acceptance never means business success.

`ais package` records each declared case ID/category, its fixture hash and matching Surefire
execution. The report separates local verification from platform conformance, which the CLI
does not run. Every selected SPI must have at least one confirmed success, processing or
protocol case that returns an expected result; exception-only cases cannot establish that
mapping is implemented. Review applicable missing scenarios before delivery; no fixed test
count proves protocol completeness.

The platform owns channel-context assembly and capability registration, domain/sandbox routing, key lookup and notification forwarding. The generated manifest does not automatically modify platform configuration.

## Security steps

`demo` marks an unfinished platform-call example, not a production security policy. Its displayed order is only skeleton wiring; confirm the actual algorithm, content, parameters, encoding and order before implementation. Advanced `platform`/`adapter` configurations represent explicitly supplied rules rather than wizard defaults.

| SPI | Direction | Order | Operation | Implementation | Algorithm / key-query type | Rule reference |
| --- | --- | --- | --- | --- | --- | --- |
<#list capabilities as cap><#list cap.directions as direction><#list security[cap.method][direction] as step>
| [=cap.method] | [=direction] | [=step?index + 1] | [=step.operation] | [=step.implementation] | [=(step.algorithm)!((step.keyAlgorithm)!"-")] | [=step.rule] |
</#list></#list></#list>

Generation configuration must not contain real keys. The platform separately authorizes the SDK; do not redistribute it publicly with this project.
