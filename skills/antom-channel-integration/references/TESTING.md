# Adapter Unit Testing Guide

## 1. Test scope

CLI 0.1.0 projects use JUnit 5, Mockito 4 and a local Spring `AnnotationConfigApplicationContext`. Tests execute real SPI methods, anonymous mappings, transport customization and security customization. They mock platform HTTP, security and result-code services; they do not start SOFABoot or connect to institutions, the parameter center or IBCM.

| Test layer | What it proves | What it does not prove |
| --- | --- | --- |
| `GeneratedStructureTest` | Selected methods, SDK compilation and Spring wiring | Business rules or platform registration |
| `<Method>SecurityContractTest` | Synthetic template order, typed calls, failure blocking and ciphertext transport | Real protocol hooks or cryptographic calculation |
| `<Method>DeliveryTest` | Real SPI behavior for every independently prepared case, including exact outbound requests and standard results | Mocked signature/ciphertext correctness or final network bytes |
| Adapter security-vector tests | Actual custom computation using synthetic keys and independent vectors | Real platform key lookup or institution acceptance |
| Platform host conformance | Real platform security service/BCM calculation with synthetic key retrieval | Real institution credentials, live routes or production readiness |

Every selected method has structure, security-flow and delivery coverage. Generated `success.json` is an unfinished example with `confirmed:false`; it intentionally fails until confirmed protocol expectations and real hooks are implemented. Do not delete tests, replace the real SPI with a test implementation, or mark a placeholder confirmed to make packaging pass.

For other supplied scaffolds, inspect [baseline differences](baseline-scaffold.md). Additional platform dependencies used by the real SPI must have matching test-context mocks.

## 2. Run tests

From the generated adapter root using Java 8:

```sh
mvn -Dtest=GeneratedStructureTest test
mvn -Dtest=PayDeliveryTest test
mvn clean verify
```

Run only classes generated for the selected scope. `PayDeliveryTest` runs every JSON case in `scenarios/pay/`; it is not limited to one success sample. Reports are in `target/surefire-reports/`.

Delivery requires actual passing, non-skipped structure, per-method security-flow and delivery tests. A suite name, compilation, a selective run or `-DskipTests` is not sufficient evidence. Mockito is test-scoped; version 4.11.0 is the Java 8-compatible baseline, not a claim about the current main release.

Packaging also requires at least one confirmed returning case per selected SPI: category `success`, `processing` or `protocol` with an expectedResult object, not an expectedException. Deleting all return-path cases and leaving only expected errors cannot qualify a method for delivery. This is a minimal gate, not a fixed category quota or proof of full protocol coverage.

Obtain SDK 1.5.2 and its standalone consumer POM separately. Generated projects use a local file repository and provided scope; other dependencies need accessible repositories or cache. When a Maven mirror covers all repositories, exclude `bundled-sdk`. Offline builds still need compatible settings and repository IDs.

## 3. Add independent protocol cases

```text
src/test/resources/scenarios/
  pay/
    success.json
    business-rejected.json
    processing.json
    missing-required-field.json
    outbound.txt                 Optional exact UTF-8 body fixture
  refund/
    success.json
  notifyPayment/
    success.json
    invalid-signature.json
```

The generated `DeliveryScenario` helper discovers each method's JSON files, sorts them, validates all fixtures before invocation and supplies a named JUnit parameterized case. Each file is one case. Its `id` must match its filename and use lowercase letters/digits/hyphens, starting with a letter. Add JSON cases without editing Java test code.

Prepare expectations before implementing the mapping. Use confirmed institution examples, synthetic standard inputs and independently calculated vectors; never invoke the mapper under test to generate its own expected files. Deserialize each case independently to avoid shared mutable input.

| Field | Required content |
| --- | --- |
| `id`, `category`, `confirmed` | File identity, applicable case category, and explicit confirmation of independent expectations |
| `input` | Standard SDK input object, or explicit null for a null-input case |
| `context` | `present`, `channelCode`, `merchantId`, `runtimeEnv`; the latter two may explicitly be null |
| `http` | Explicit `calls:0` or `calls:1`; a called boundary has a response or exception |
| `expectedRequest` | Required for an HTTP call; exact method, contentType, pathParameters, headers, queryParameters, formParameters, attributes and body |
| `security` | Every generated enabled step with explicit call count and matching typed request/result or exception; `{}` when no steps are enabled |
| `resultCode` | Explicit array of expected mapping calls; `[]` forbids result-code service calls |
| `expectedResult` or `expectedException` | Exactly one: complete standard output object, or exact allowed exception type and specific message |

Categories are `success`, `business-failure`, `processing`, `validation-failure`, `security-failure`, `http-failure`, `identity` and `protocol`. They describe cases, not automatic proof of coverage. Include only protocol-applicable categories and explain omissions in the delivery report.

### A synthetic unsigned payment case

The following illustrates the schema for a hypothetical protocol, not institution business rules. It requires matching real mapping code and no enabled security operations:

```json
{
  "id": "success",
  "category": "success",
  "confirmed": true,
  "input": {"paymentRequestId": "order-001", "paymentAmount": {"currency": "USD", "value": "1250"}},
  "context": {"present": true, "channelCode": "iaischannelexample", "merchantId": "synthetic-merchant", "runtimeEnv": null},
  "http": {"calls": 1, "response": {"statusCode": 200, "headers": {}, "body": "{\"id\":\"institution-001\",\"state\":\"accepted\"}"}},
  "expectedRequest": {"method": "POST", "contentType": "JSON", "pathParameters": {}, "headers": {}, "queryParameters": {}, "formParameters": {}, "attributes": {}, "body": "{\"reference\":\"order-001\",\"amount\":1250}"},
  "security": {},
  "resultCode": [],
  "expectedResult": {"paymentId": "institution-001", "paymentAmount": null, "paymentCreateTime": null, "normalUrl": null, "paymentResultInfo": null, "orderCodeForm": null, "extendInfo": null, "result": {"resultStatus": "S", "resultCode": "SUCCESS", "resultMessage": "Success"}}
}
```

Add another success case with a different order and amount to detect hardcoded sample values. Select expected standard codes from the confirmed mapping; this illustration does not grant success to every HTTP 200 response.

### Exact bodies and platform responses

HTTP response fixtures specify statusCode, headers and exact body text, including explicit null. Status is 100–599 or the SDK's `-1` transport-failure sentinel; -1 is not a real institution HTTP response or definitive business rejection. Request/response bodies may use `bodyFile` instead of `body` to load a sibling UTF-8 `.txt` file without trimming or reformatting. Do not specify both. Files must be ordinary local fixtures, not symlinks or paths outside the scenario directory.

Body comparison is character-for-character; `{}` and null differ. Method uses the actual protocol value; contentType is the SDK enum name or explicit null when a confirmed custom Content-Type header supplies it. Maps must explicitly be objects or null. An institution GET, text or bodyless protocol needs matching transport/request-body customization, not merely a changed fixture. SDK HttpRequest has no URL field: platform tests own final URL resolution and transmitted-byte verification.

For platform HTTP exceptions, replace `http.response` with `http.exception:{"type":"IllegalStateException","message":"synthetic timeout"}` and supply the exact expected outcome. A timeout does not establish final business rejection. Baseline JSON cases cover at most one HTTP call; use ordinary JUnit tests for multi-call protocols.

### Security and result-code expectations

Enabled step names are `requestSign`, `requestEncrypt`, `responseVerify`, `responseDecrypt`, `notificationVerify` and `notificationDecrypt`, as generated for that method. Example platform delegation:

```json
{
  "requestSign": {
    "calls": 1,
    "request": {"merchantId": "synthetic-merchant", "algorithm": "HMAC_SHA256_BASE64", "content": "independently prepared canonical text"},
    "result": "synthetic-signature"
  },
  "responseVerify": {"calls": 0}
}
```

Use typed SDK request fields. Verification results are booleans; false is a supported negative case. Adapter computation uses SecurityKeyRequest and synthetic SecurityKeyMaterial instead. A zero-call step has no result. A called step has a result or an exact exception, never both. The generated test checks matching arguments, call counts and unexpected platform-boundary interactions; it does not lock internal helper design.

Each `resultCode` entry has `calls`, `request` and `result` or `exception`. The request contains channelCode, channelResultCode, channelResultMsg and api, plus optional secondResultCode/thirdResultCode for the matching SDK overload. Its result is the expected SDK Result. Independently specify raw institution codes so a wrong field or missing mapping call fails. Do not choose parameter-center api scope by guesswork.

### Expected exceptions and identity

JSON failure cases accept `IllegalArgumentException`, `IllegalStateException`, `SecurityException` or the generated `ChannelIntegrationException` with an exact specific message. Use ordinary JUnit tests for other custom exception types. UnsupportedOperationException from unfinished hooks is never an accepted business outcome. Missing/malformed fixtures and Spring setup failures occur outside the application-exception capture and must fail the test.

Context is independent of business input. Generated tests simulate host binding, overwrite platform-owned input channel/environment fields and clear context in finally. `present:false` tests absent context; null merchantId tests missing merchant. Null runtimeEnv is legitimate and must not skip security. Add forged business identity and merchant-A/merchant-B cases; adapters must read the platform context, not bind/clear it themselves or construct IBCM subjectId.

For callback mapping, preserve the exact complete synthetic URL, including existing query parameters and `isSandbox=true`. This verifies mapping, not sandbox recognition. Do not add adapter-side sandbox branches or an isSandbox field to the test context. Host filters own recognition, conflicts and invalid-marker behavior.

## 4. Capture institution requests

Generated tests compare complete outgoing requests. For additional ordinary JUnit tests, inspect values at invocation time inside a Mockito Answer:

```java
when(http.executeDynamicUrl(same(input), anyMap(), any(HttpRequest.class))).thenAnswer(call -> {
    HttpRequest request = call.getArgument(2);
    assertEquals("POST", request.getMethod());
    assertEquals(expectedBody, request.getBody());
    return HttpResponse.builder().statusCode(200).body(channelResponse).build();
});
```

`same(input)` checks the original context object; also assert raw dynamic-path values, headers, Query/Form, contentType and attributes. Do not pre-encode placeholder values. Header names are case-insensitive at HTTP level, while fixture maps compare actual keys; explicitly test inbound casing variations when relevant.

Ordinary structural JSON comparisons must distinguish extra fields, array order, number versus string, null and absence. Complete expectedResult includes null-valued SDK fields retained by JSON.toJSON; the example above deliberately includes them instead of comparing only populated fields. Never use structural equality to conceal changed signing text or HTTP body bytes.

## 5. Security mode 1: mock platform computation

Run the real ChannelSecurityCustomization, capture exact SecuritySignRequest/VerifyRequest/EncryptRequest/DecryptRequest and assert merchantId, algorithm, content/signature/ciphertext and protocol parameters. Check final placement, prefix and order. Request security failure must prevent HTTP; failed notification verification must prevent mapping. Model both successful and failing verification, not a globally true mock.

Mock computation proves delegation and assembly only. It does not prove actual signatures or ciphertexts. Whole-JSON signing and demo header names are not universal protocols. When the institution signs or hashes the HTTP body, use its exact final serialized text; otherwise assert the confirmed field-selection and operation-order rules. Verification must use original institution text rather than unapproved reserialization.

## 6. Security mode 2: mock keys and execute the real library

Mock only platform queryKey and execute the adapter's real library code. For HMAC-SHA256, a public [RFC 4231 vector](https://www.rfc-editor.org/rfc/rfc4231.html#section-4.3) provides an independent known answer:

```java
when(platform.queryKey(eq(expectedKeyRequest)))
    .thenReturn(SecurityKeyMaterial.builder()
        .value("Jefe") // Public test-vector key, never use in production.
        .keyType(SecurityKeyType.SYMMETRIC_KEY)
        .keyAlgorithm(SecurityKeyAlgorithm.HMAC_SHA256)
        .build());
```

Assert merchantId, purpose and keyAlgorithm. Confirm material storage/encoding: this example is raw UTF-8, not an instruction to decode RSA/PEM/Base64 the same way. Test wrong material, query failure and tampering. Do not generate expected vectors with the same function being tested.

For randomized algorithms, independently verify signatures or decrypt ciphertext and assert format/content rather than demanding identical output each run. Use mature libraries and synthetic keys, not copied platform source or real credentials.

## 7. Platform security conformance

Platform-owned tests execute the real platform security service and BCM tools, replacing only key retrieval. They validate computation separately from adapter DTO assembly; see [maintainer validation](maintainer-validation.md#3-platform-host-conformance).

For asymmetric operations, use the correct counterpart key: platform-private signing versus platform-public verification, institution-public encryption versus institution-private decryption, and the reverse ownership for inbound messages. Platform sign and verify, or encrypt and decrypt, for the same merchant do not necessarily query a matching key pair. Do not use those calls as a naive round-trip test.

The platform test layer owns final URL/form encoding, actual transmitted bytes and transport behavior. Live normal/sandbox institution testing still verifies actual materials, protocols, iPay forwarding and ACK behavior.

## 8. Notifications and acceptance

Notification cases supply original rawBody/headers, run real security before mapping and assert zero platform HTTP calls. The adapter returns standard notifications, not iPay calls or ACKs. PaymentNotifyRequest.extendInfo is a String populated only under confirmed rules; assert it independently. Prefer builders/setters over SDK all-arguments constructors.

Delivery acceptance requires applicable success, rejection/processing, missing-field and exception scenarios; enabled notification security needs both valid and invalid signatures. Non-2xx handling follows the explicitly chosen protocol policy. Record unavailable scenarios with reasons rather than silently deleting them.

Production plain JARs exclude test helpers, fixtures, Mockito/JUnit and test keys. Report local adapter tests, platform conformance and institution integration separately. Tests are evidence for specific assertions, not security certification or automatic business completeness.

Keep simple ordinary JUnit escape hatches for complex cases. Do not introduce a testing DSL, reflection dispatcher, duplicated platform implementation or SOFABoot runtime into the generated adapter tests.
