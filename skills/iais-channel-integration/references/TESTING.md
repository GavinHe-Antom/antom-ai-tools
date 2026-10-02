# Adapter Unit Testing Guide

## 1. Test scope

This page targets CLI 0.1.0 projects using JUnit 5, Mockito 4 and JSON fixtures. Generated tests start a local Spring `AnnotationConfigApplicationContext`, scan the adapter and inject platform mocks. They do not start SOFABoot or connect to institutions, the parameter center or IBCM.
Field mapping, transport customization, security customization and response mapping must run real implementations. For other supplied projects, read [scaffold differences](baseline-scaffold.md).

SPI wiring is generated; business hooks in each SPI method's anonymous ChannelApiExtension or ChannelNotificationExtension and enabled security hooks are unfinished. Initial delivery-test failures are expected, not disposable template noise. Do not bypass real code with replacement SPIs implemented in tests.

| Test class | What it covers |
| --- | --- |
| `GeneratedStructureTest` | Spring bean wiring and selected methods, not institution protocol |
| `<Method>SecurityContractTest` | Synthetic hooks verify security order, typed inputs, failure blocking and ciphertext transport, not institution algorithms |
| `<Method>DeliveryTest` | Real SPI compared with independent standard results and outgoing payloads; e.g. PayDeliveryTest and RefundDeliveryTest |
| `Notify*DeliveryTest` | Notification SPI, standard notification comparison and no platform HTTP; e.g. NotifyRefundDeliveryTest |

Every selected method has DeliveryTest and SecurityContractTest. DeliveryTest asserts complete HTTP properties and selected security requests; still supply independent fixtures and add institution rejection, unknown, missing-field and independent algorithm-vector cases.
SecurityContractTest's synthetic hooks do not replace real security acceptance or imply institution crypto implementations are built into the template.
When an anonymous extension uses platform dependencies such as ResultCodeService, inject them into the enclosing SPI service and register matching mocks in the test Spring context.

## 2. Run tests

From the generated adapter root with Java 8:

```bash
mvn -Dtest=GeneratedStructureTest test
mvn -Dtest=PayDeliveryTest test
mvn clean verify
```

PayDeliveryTest covers required pay; run other method classes only if generated. Reports are in `target/surefire-reports/`. Nonzero Maven exit means failure; do not use -DskipTests for delivery verification.
Surefire failIfNoTests is enabled, but cannot detect missing coverage for an individual SPI.
Mockito is test-scoped, not packaged in production. Version 4.11.0 is this project's Java 8-compatible choice, not a claim that it is Mockito's current main release.

The SDK uses a local file repository and provided scope. Obtain SDK 1.5.2 JAR and standalone consumer POM separately from the platform. Other dependencies require accessible Maven repositories or cache.
If a mirror covers the file repository, exclude bundled-sdk. After initial dependency resolution, offline runs still need the same settings/repository IDs; otherwise Maven may regard existing cache entries as unavailable.

## 3. Copy a scenario

```text
src/test/resources/scenarios/
  pay/
    input.json              Standard request, deserialized into PayRequest
    context.json            Independent synthetic platform identity, not derived from input
    expected-request.json   Complete outbound properties; body is the final serialized string
    response.json           Simulated institution response
    response-headers.json   Simulated institution response headers
    expected-result.json    Independently confirmed standard output
    security.json           Typed requests and mock results for selected security steps
  refund/                   Refund input, expected body and institution response
  notifyPayment/            Generated only if selected; preserve rawBody in input.json
```

1. Copy the closest fixtures and test method, using synthetic merchants, orders and amounts.
2. Adjust standard input, institution response and independent expected payload. Do not derive expectations from the mapper under test.
3. Mock only platform methods used by the scenario. Generated tests use Mockito.mock; do not assume MockitoExtension strictness is configured.
4. Load real customization, templates and wired SPIs in the test Spring context; bind identity from context.json, execute the SPI and clear identity in finally.
5. Assert HTTP parameters, platform security inputs, standard output and call counts. Add negative cases for important failure branches.

expected-request.json must include method, pathParameters, headers, queryParameters, formParameters, contentType, attributes and body.
contentType uses the SDK enum name; empty maps and null differ. Body is the exact final string, not a JSON object, and the field cannot be omitted. An explicitly null body is valid for a bodyless protocol, but the default JSON template produces a non-null body and needs corresponding workflow changes.

security.json names properties by direction and operation, such as requestSignRequest, requestSignResult and responseVerifyRequest.
Platform-computation requests use typed SDK requests; non-verify results are mock computation results. Adapter-computation entries use SecurityKeyRequest and synthetic SecurityKeyMaterial.
Use {} when every operation is none. Only an independently specified, matching verification request may return true; do not allow all verification calls globally. Consult the generated README for selected properties.

Independently populate channelCode, merchantId and runtimeEnv in context.json using synthetic values. It simulates host identity, not institution payload or production configuration.
runtimeEnv may explicitly be null; absence must not skip security. Expected security merchantId must match context.json. Generated DeliveryTest handles binding/cleanup; additional tests must also use try/finally without moving binding into production adapter code.

```java
ChannelRequestContext.bind("examplechannel", "synthetic-merchant", "dev");
try {
    PayResponse result = paymentService.pay(input);
    assertNotNull(result);
} finally {
    ChannelRequestContext.clear();
}
assertThrows(IllegalStateException.class, ChannelRequestContext::current);
```

This context does not carry real HTTP route selection or shadow Tracer state. Test identity binding does not replace platform routing, sandbox or key integration tests. Add missing-context, missing-merchant, forged business identity and exception-cleanup cases.

Generated fixture(name) reads UTF-8 text from the current method's scenarios directory without trimming or formatting. Deserialize input separately per test to prevent mutable-state contamination.
Add fixtures for rejection, tampering and empty responses. Generated empty JSON is not a confirmed protocol example. Compare signing input exactly; ordinary JSON mappings may use structural comparison.

## 4. Capture institution requests

Assert parameters at invocation time inside a Mockito Answer:

```java
when(http.executeDynamicUrl(same(input), anyMap(), any(HttpRequest.class))).thenAnswer(call -> {
    HttpRequest request = call.getArgument(2);
    assertEquals("POST", request.getMethod());
    assertEquals(expectedJson, JSON.parseObject(request.getBody()));
    return HttpResponse.builder().statusCode(200).body(channelResponse).build();
});
```

same(input) also checks that context was not replaced; assert important route fields separately. Do not retain mutable references and compare much later.
For asynchronous or multiple-request cases, copy relevant values in the Answer; a generic snapshot framework is unnecessary.

| Object | Assertions |
| --- | --- |
| context | channelCode, platform-injected domain/path and headers/raw text actually supplied by the flow |
| Dynamic URL parameters | Placeholder names and raw values such as `invoice/001 +`; no pre-encoding |
| HttpRequest | method, headers, contentType, body, queryParams, formParams, attributes |
| Standard output | Result codes/status, institution IDs, amounts and redirect information |

SDK HttpRequest has no URL field. Platform tests verify domain control and final URL construction.
Template query methods default to POST too. For institution GET calls, change real transport customization and assertions; changing a fixture alone does not implement GET.

Structural JSON comparisons must still distinguish extra fields, array ordering, numeric/string amounts, null and absence.
Generated DeliveryTest compares outbound body strings exactly. Signing input must also match character-for-character; structural equality must not conceal changed signing text.
HTTP header names are case-insensitive, but generated Map comparisons check actual keys. Fixtures should match explicitly written names, with additional tests for differently cased inbound headers.

## 5. Security mode 1: mock platform computation

Review generated DeliveryTest security assertions and add institution-specific failures:

- Mock platform.sign output.
- Run real mapping and signature assembly; capture SecuritySignRequest.
- Assert merchantId, algorithm, content and the final HTTP signature header.
- Verify signing JSON exactly equals the HTTP body.
- Test signing exceptions separately; HTTP call count must be zero.

Invoke the project's real ChannelSecurityCustomization; do not replace it with a test implementation. Whole-JSON signing and example header names are not universal platform protocols.

Read the merchant with ChannelRequestContext.current().getMerchantId(). **BaseChannelRequest has no merchantId property.**
The template fills platform security requests from context; business fields and protocol hooks cannot replace that identity. Adapters do not construct IBCM subjectId.

For encryption/decryption, mock the dedicated request's result and capture cipherType, algorithm, merchantId and plaintext/ciphertext. Assert that HTTP sends ciphertext, not plaintext.
Mock computation validates delegation parameters and assembly, not IBCM calculation correctness.

## 6. Security mode 2: mock keys and execute the real library

For adapter computation, mock only queryKey and execute the actual project crypto library.
For HMAC-SHA256, use a public [RFC 4231 §4.3](https://www.rfc-editor.org/rfc/rfc4231.html#section-4.3) vector and compare a fixed result rather than generating expectations with the same function. Use independent vectors for other algorithms.

```java
when(platform.queryKey(any(SecurityKeyRequest.class)))
    .thenReturn(SecurityKeyMaterial.builder()
        .value("Jefe") // Public test-vector key, never use in production.
        .keyType(SecurityKeyType.SYMMETRIC_KEY)
        .keyAlgorithm(SecurityKeyAlgorithm.HMAC_SHA256)
        .build());
```

Assert query merchantId, purpose and keyAlgorithm. Interpret material according to the agreed storage format; this example uses raw UTF-8 text and does not imply RSA/Base64 material is read the same way.
Test query failure, wrong material type and tampered content. For nondeterministic encryption, use an independent decryption/verification path and known protocol examples rather than requiring identical ciphertext every run.

## 7. Notifications and exceptions

Notification tests supply original body and headers, verify before validation/mapping, and run this project's real security hooks. Cover verify=false, platform security exceptions and missing fields separately. Test signature-header lookup with different casing.

Notification templates neither call iPay nor create ACKs, so assert zero platform HTTP calls. The host validates iPay forwarding and ACK behavior.

PaymentNotifyRequest.extendInfo is a String extension field. Populate it only under confirmed rules and independently assert it in expected-result.json. Do not assume a universal JSON structure, requiredness or length.
Use a builder or no-argument constructor with setters, avoiding all-arguments constructors whose signatures change when fields are added.

The CLI template throws on non-2xx before response mapping; statusCode=-1 also fails. Channels requiring institution business-code parsing from non-2xx must define and test their policy separately.
An HTTP timeout does not prove the institution rejected the request. Do not return payment success or assert definitive order failure.

## 8. Acceptance checklist

- Every delivered SPI has a real entry-point test: construct the real SPI/dependencies and invoke it; do not mock the subject under test.
- Preserve generated real-SPI calls and request/result assertions. Complete fixtures; unfinished hooks and null returns are not success.
- Cover success, business rejection, unknown/processing and exceptions for selected payment/refund/query/cancel methods. Test authenticateAuthorize only for two-call 3DS; do not require capture for non-card integrations.
- Cover verification success/failure and mappings for enabled notifications. Unit tests do not expand unavailable host API scope.
- Use independent signature vectors for institution security rules. Changed amounts, absent signature headers, wrong merchants or tampered notifications should fail tests.
- Production plain JARs must exclude test classes, fixtures, Mockito, JUnit and test keys.
- Platform transport tests separately verify URL/form encoding, actual transmitted bytes and redirects; institution sandbox tests verify actual keys and protocols.

Extend the generated local Spring context rather than adding SOFABoot, a DSL, generated expectations or reflection dispatchers for testing.
Add ordinary test methods for more scenarios; use JUnit parameterized tests when only data varies within one flow.
