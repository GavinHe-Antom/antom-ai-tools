# [=artifactId]

Channel: [=channelCode]. SDK: [=sdkVersion] (provided). Generator template: [=templateVersion].

## Start development

1. `customize/api/*Mapping.java`: implement required-field validation, request/response mapping and path placeholders from the institution documentation.
2. `customize/transport/ChannelTransportCustomization.java`: configure GET/POST, headers, query parameters and similar protocol details. Do not configure domains or connection settings, or create another HTTP client.
3. `customize/security/ChannelSecurityCustomization.java`: implement the selected canonical text and result placement. The platform mode delegates computation to platform APIs; the adapter mode obtains material through platform queryKey and computes with a mature library. Follow the protocol's operation order and stop on security failure.
4. `src/test/resources/scenarios`: populate independent protocol fixtures in the format below. Mock platform services in tests; do not call real institutions.
5. `src/test/java/*DeliveryTest.java`: generated assertions cover all HTTP properties and selected typed security requests, exercising real Spring SPI implementations and customization hooks. Add institution-specific result-code, missing-field, boundary and independent algorithm-vector tests. Do not delete assertions to make an unimplemented protocol pass.
6. `ais package --project .`: resolve dependencies, verify the actual SDK JAR/POM, run all tests and inspect the ordinary JAR. Initial delivery-test failures are expected: unfinished business behavior must not be reported as passing.

You may first run `mvn -Dtest=GeneratedStructureTest test` to check structural wiring; this is not delivery acceptance. Use Java 8 / Maven 3.6.3+.

## Test layers and independent fixtures

- `GeneratedStructureTest`: Spring wiring and the selected SPI method list.
- `*SecurityContractTest`: synthetic hooks exercise the generated security-step order, merchant/algorithm/content/key-purpose/runtime-parameter delegation, abort on security exceptions or false platform verification, and use of the processed ciphertext in HTTP. These tests do not implement institution protocols or execute real cryptographic algorithms. Fixed test sentinels are not production IVs, signatures or keys.
- `*DeliveryTest`: execute real customization hooks through SPI and compare with independent fixtures. Empty fixtures or unfinished hooks must fail. Mocked computation results prove delegation, not institution algorithm compatibility. Custom computation especially requires independent valid/invalid signature vectors.

Each `src/test/resources/scenarios/<method>/` contains:

| File | Contents |
| --- | --- |
| `input.json` | Platform-standard request; notifications include synthetic raw messages and headers |
| `context.json` | Independent test-host identity: `channelCode`, `merchantId`, and `runtimeEnv` (string or explicit null); never derive these from `input.json` or raw notification content |
| `expected-request.json` | Non-notification methods: final HTTP method, dynamic path parameters, headers, query, form, ContentType enum name, attributes and serialized body string |
| `response.json` | Non-notification methods: exact institution response text; construct verification input according to the institution's encoding rules |
| `response-headers.json` | Non-notification methods: institution response header map, including synthetic signatures required by the protocol |
| `expected-result.json` | Independently defined platform-standard result |
| `security.json` | Complete typed requests and mocked results for the selected security calls listed below; use `{}` when all steps are none |

The complete `expected-request.json` structure is shown below. Values must come from the confirmed protocol, and map fields must be explicit. `{}` and `null` are distinct and are not normalized. `body` is the final wire JSON as a string, not a JSON object. The field is required but can explicitly be `null` for a protocol without a body. The current JSON flow serializes non-null JSONObject values; bodyless protocols require corresponding template adjustments and must pass these assertions. Dynamic-path assertions check only placeholder parameters passed to the platform; domains and path templates remain platform-managed.

```json
{
  "method": "<confirmed method>",
  "pathParameters": {},
  "headers": {},
  "queryParameters": {},
  "formParameters": {},
  "contentType": "<SDK ContentType enum name>",
  "attributes": {},
  "body": "<exact serialized JSON sent to the institution>"
}
```

`security.json` uses SDK request field names. Include the exact merchantId, algorithm, plaintext/ciphertext, signature and applicable parameters. Only independently defined successful verification requests are stubbed to return true; unmatched requests must not pass. Key-query stubs must contain test material only, never production keys.

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

Delivery tests simulate this host lifecycle with independent `context.json` values.
Security contract tests cover missing identity, hook identity overrides and same-thread
cleanup. Structure tests compile the SDK's `BaseChannelRequest.runtimeEnv` accessors/builder
and `PaymentNotifyRequest.extendInfo` accessors/builder; use the notification builder,
not a field-count-dependent all-arguments constructor. Map `extendInfo` only when the
confirmed notification protocol supplies it; the field does not carry trusted identity.

| SPI | JSON property | SDK type / meaning |
| --- | --- | --- |
<#list capabilities as cap><#list cap.directions as direction><#list security[cap.method][direction] as step><#if step.implementation != "none">
<#assign id=direction+step.operation?cap_first>
<#if step.implementation == "adapter">
| [=cap.method] | `[=id]Request` | SecurityKeyRequest: merchantId, purpose, keyAlgorithm |
| [=cap.method] | `[=id]Result` | SecurityKeyMaterial: mocked key-query result for independent tests only |
<#else>
| [=cap.method] | `[=id]Request` | Security[=step.operation?cap_first]Request: complete SDK request |
<#if step.operation != "verify">
| [=cap.method] | `[=id]Result` | Mocked computation result string |
</#if>
</#if>
</#if></#list></#list></#list>

## Selected capabilities

<#list capabilities as cap>
- `[=cap.method]` → `customize/api/[=cap.title]Mapping.java`.
</#list>

The template uses the platform's dynamic URL API. Pass an empty map when a path has no placeholders. All URLs come from the platform; use its SDK route view when signatures require the actual target. The template accepts only 2xx JSON responses by default. Adapt the template layer and tests for non-2xx business responses or non-JSON protocols; never fabricate success.

The platform owns channel-context assembly and capability registration, domain/sandbox routing, key lookup and notification forwarding. The generated manifest does not automatically modify platform configuration.

## Security steps

| SPI | Direction | Order | Operation | Implementation | Algorithm / key-query type | Rule reference |
| --- | --- | --- | --- | --- | --- | --- |
<#list capabilities as cap><#list cap.directions as direction><#list security[cap.method][direction] as step>
| [=cap.method] | [=direction] | [=step?index + 1] | [=step.operation] | [=step.implementation] | [=(step.algorithm)!((step.keyAlgorithm)!"-")] | [=step.rule] |
</#list></#list></#list>

Generation configuration must not contain real keys. The platform separately authorizes the SDK; do not redistribute it publicly with this project.
