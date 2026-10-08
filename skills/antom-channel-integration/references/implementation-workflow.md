# From Integration Rules to AIS Channel Adapter Code

Use this workflow when the user explicitly asks to implement, complete or fix an adapter. For knowledge questions, read the relevant reference instead.

## 1. Confirm scope; scaffold examples are not institution protocols

Read the target POM, Spring scan configuration, generated SPIs, customization layer and tests. Summarize SDK/template version, card/non-card, one-/two-call 3DS, selected methods, notification types, completed code and existing uncommitted changes. Preserve unrelated work.

For each method, align three inputs: SDK request/response types, user-provided mappings and institution protocol examples. The [rules template](../assets/integration-rules-template.md) can record differences; do not require users to adopt its format. Once confirmed information is complete, implement it without inventing additional approval stages.

If there is no project, follow the authoritative [generation intake](project-generation.md#confirm-the-generation-inputs) first. Do not generate a demo or assume capabilities while answers are missing. Algorithms, order and computation modes belong to implementation, not scaffold intake. Report missing CLI/SDK artifacts rather than inventing download URLs.
The locations below apply to CLI 0.1.0. For a platform-supplied generic scaffold, read [scaffold differences](baseline-scaffold.md); do not mix class structures.

## 2. Implement only sufficiently defined rules

| Topic | Required before implementation | If unclear |
| --- | --- | --- |
| Capabilities | Exact SPI methods, card type and 3DS mode | Do not enable everything by default |
| Fields | Source/target paths, types, conditions, defaults, amount units and timezone | Ask about specific fields; do not guess |
| Security | Algorithm, input order, encoding, prefix, key purpose, platform merchant/institution identity mapping | Do not deliver default no-op security |
| Transport | Method, Content-Type, headers, Query/Form and dynamic-value sources | Do not configure domains, timeouts or retries yourself |
| Responses | Success/failure/processing, raw institution codes and platform mapping scope | Unknown must not become success |
| Notifications | Original-text verification, event types, correlation IDs and ACK | Adapter returns standard notification only; confirm ACK gaps with platform |

Implement independent methods whose rules are complete. With missing security rules, mapping/tests may proceed, but clearly state that the method is not deliverable. Do not fabricate signatures.

## 3. Prepare independent expectations before code

For each confirmed scenario, prepare standard input, synthetic platform identity, institution response, expected outbound request and expected standard result independently. Add expected security arguments and result-code mapping arguments when applicable. Use confirmed protocol examples or independently calculated test vectors, never the mapper under test to create its own expected files.

Cover applicable success, rejection/processing and failure branches. Record unconfirmed protocol cases as gaps rather than filling them with fabricated success. Then implement the real hooks and run the [scenario tests](TESTING.md). Fixture/schema errors and unfinished hooks are test failures, not expected business exceptions.

## 4. Code locations and wiring

Paths are relative to the adapter's Java root package:

| File/directory | Implementation |
| --- | --- |
| spi/ChannelPaymentService, ChannelRefundService, ChannelNotificationService | Generated selected-method wiring; verify against capability inventory |
| Anonymous ChannelApiExtension inside each transaction SPI method | Implement validate, mapRequestBody, mapUrlParameters and mapResponse |
| Anonymous ChannelNotificationExtension inside each notification SPI method | Implement validate/map; convert verified/decrypted text into standard notifications |
| customize/transport/ChannelTransportCustomization | Operation-specific HTTP method, protocol headers, Query/Form and Content-Type |
| customize/security/ChannelSecurityCustomization | Request signing/encryption; response and notification verification/decryption |
| Response/notification mapping in each anonymous extension | Confirm success rules and standard result combinations; inject ResultCodeService into the enclosing service if needed, without assuming a universal mapper exists |
| src/test/java, src/test/resources | Real SPI cases, synthetic standard inputs, institution responses and independent expectations |
| pom.xml, Spring XML | Change coordinates, unique package, scan scope and compatible provided dependencies only when necessary |

No `customize/api` directory or per-method Mapping helper classes are generated. Each selected method constructs its anonymous extension at the template call; do not move these hooks into duplicate helper wiring or confuse them with the platform generic scaffold's factories.

Transaction SPIs invoke template.executeDynamicUrl(operation, original request, new ChannelApiExtension<...>() { ... }). mapUrlParameters returns raw placeholder values, or an empty Map when no placeholders exist.
Notification SPIs invoke executeNotification and return PaymentNotifyRequest, CaptureNotifyRequest or RefundNotifyRequest, not NotifyResponse.
Check fields/event types against the corresponding method page. A selected refund notification has an anonymous extension in notifyRefund; it does not add a refund transaction service. Compare refund/inquiryRefund selections with the confirmed scope before assuming that an absent service is a generator failure.

Keep template/extension/model stable for ordinary flows. Use `ChannelOutboundRequest.setRawBody` for exact text or explicit bodyless calls, `getSerializedBody` for signing the final payload, and the anonymous extension's `acceptResponseStatus` for confirmed non-2xx business bodies. Test the policy without introducing another HTTP client.
Confirm route support with the platform before combining institution APIs with different destinations.

## 5. Field and transport implementation

1. Validate this operation's business conditions, not another operation's or an example validate hook's requirements.
2. Construct institution fields explicitly. Platform channelCode/domain/path/requestHeaders do not automatically belong in institution bodies.
3. Use exact decimal arithmetic with currency and institution units; retain leading zeros in identifiers.
4. Pass raw dynamic-path, Query and Form values for platform encoding. Do not pre-encode and concatenate again.
5. Select institution-required headers explicitly; do not copy all inbound headers or supply Host.
6. Map institution business states. HTTP 200, request acceptance and final transaction success are different.
7. Verify/decrypt notifications before trusting, validating and mapping the body; return standard notifications for platform forwarding.

The default is mapped JSON with 2xx response acceptance. Exact text/bodyless requests and non-2xx business bodies need explicit customization. Platform GET/HEAD body omission and nonempty Form precedence over Body affect signing input; changing a header alone does not establish correct wire content.

## 6. Security implementation decisions

Read the [security APIs](guide/reference/security.md) and select existing methods that exactly match the institution protocol. The scaffold's two `securityFeatures` booleans only generate enabled platform-call examples. Complete their typed protocol hooks, confirm operation order and select platform versus custom adapter computation here; unfinished enabled hooks fail rather than fabricate security.

### Platform computation

Use separate SecuritySignRequest / SecurityVerifyRequest / SecurityEncryptRequest / SecurityDecryptRequest.
Supply merchantId, the exact algorithm and plaintext/ciphertext; cipher calls also require cipherType, and verification requires signature. Do not reuse one request type or supply IBCM subject identity/key version.

Obtain merchantId with `ChannelRequestContext.current().getMerchantId()`; generated wrappers populate it. Protocol hooks handle content, signatures, result placement and algorithm parameters only. Do not add a merchant-selection hook or change context. Runtime environment may be null and cannot replace platform sandbox routing.

Use SecurityDigestRequest for unkeyed digests. The adapter adds institution header prefixes and places signatures.
HMAC_SHA256 outputs HEX; HMAC_SHA256_BASE64 outputs Base64. Similar names are not interchangeable. A false verification result must stop processing.

### Adapter computation

Use queryKey (SecurityKeyRequest) to obtain one SecurityKeyMaterial, specifying merchantId, purpose and keyAlgorithm.
Confirm text/PEM/Base64 encoding before using host-compatible mature libraries such as Hutool/Tink. If query categories do not meet the protocol, confirm platform support rather than accessing the key system directly.
Never put keys in static state, logs, exceptions, snapshots or ordinary delivery materials. Transfer actual materials through platform-designated secure channels, not chat.

### Exact signing input

- Implement field order, separators, trailing newline, UTF-8 and null handling explicitly.
- Digest the actual final body; do not mutate signed content afterward.
- Use the protocol-required substituted Request-Target path and Query, not a placeholder-containing template.
- Verify responses/notifications against original text, not parsed and reserialized JSON.
- Request, response and notification rules can differ; do not force reuse.
- Use algorithm-native cipher formats. Populate parameters and runtime IV/AAD/PGP in generated typed hooks according to SDK requirements. Do not assume automatic IV packaging or identical encodings; do not switch algorithms merely to compile.

If rules require exact raw bytes, multi-value headers or an unavailable inbound URI, identify the boundary gap and ask the platform rather than claiming lossless preservation.

## 7. Tests must execute the real implementation

Follow the [testing guide](TESTING.md):

- Mock only platform HTTP, security and result-code services; call real SPI entry points, mappings and security customization.
- Use each scenario's independent `context` for platform identity, bind before invocation and clear in finally. Do not derive merchant identity from business input fields.
- Assert method, headers, body, Query/Form, dynamic values and original context inside the platform HTTP mock Answer; do not call real institutions.
- Prepare standard input and expected institution payload independently; do not use the mapper under test to generate expectations.
- Mock sign proves delegation/assembly only; check merchantId/algorithm/content. Independent fixed vectors validate custom computation rules; real platform computation requires [host conformance](maintainer-validation.md#3-platform-host-conformance).
- When mocking queryKey, execute the real crypto library using public vectors or synthetic keys.
- Cover security failures with no HTTP, notification verification failures with no business mapping, non-2xx, empty responses, missing fields and unknown codes.
- Use fixed clocks/IDs or small injectable dependencies for repeatability; do not introduce a generic configuration framework for testing.
- Do not remove failing cases to hide issues. When reducing capabilities, update scope assertions and explain why.

Run Maven test/package and record exit codes. Distinguish business failures from dependency/environment issues. Report unresolved blockers accurately; skipTests is not passing evidence.

## 8. Completion criteria

Selected methods contain no placeholders, outputs match contracts, security failures stop processing, and tests verify outgoing requests and standard results.
Update the method capability matrix and outstanding platform configuration. A plain JAR needs platform assembly; tests do not establish deployed beans, routes, keys or result codes.

Change only the authorized project scope. Local packaging does not authorize upload, and delivery checks are not rollout approval.
