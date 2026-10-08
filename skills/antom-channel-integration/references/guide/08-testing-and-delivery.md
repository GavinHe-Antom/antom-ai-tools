# Testing, Troubleshooting and Delivery Acceptance

## 1. Compilation does not mean integration is complete

Generated SPIs are wired, but anonymous mapping extensions inside each selected SPI method and enabled security hooks still need implementation. GeneratedStructureTest checks Spring wiring and selected methods only; it does not prove field mapping, signature verification or platform registration.
Delivery tests must run real business implementations. Do not resolve initial failures by deleting or skipping tests.

## 2. Recommended test layers

| Layer | Required coverage | Does not depend on |
| --- | --- | --- |
| Field mapping unit tests | Normal/missing/boundary fields, amounts, timezones, IDs, nesting and result codes | Real network or production keys |
| Security vectors | Fixed signing input/signatures, tampering, empty signatures, encoding, dynamic paths and notification text | Production credentials |
| Template tests | Execution order, Host prohibition, non-2xx, empty bodies, no mapping after verification failure, platform exceptions | Real institutions |
| SPI tests | Correct non-null output type for every retained method; explicit unsupported behavior for unimplemented optional methods | Host network |
| Platform security conformance | Real platform security service/BCM tools with synthetic key retrieval and correct counterpart keys | Real institution credentials |
| Host integration tests | SOFA routing, bean uniqueness, registration scope, DRM, context and sandbox | Not equivalent to ordinary unit tests |
| Institution integration tests | Normal/sandbox, synchronous/asynchronous, retries/order, iPay forwarding and ACK | Stubs cannot replace acceptance |

Stub PlatformChannelHttpService, PlatformChannelSecurityService and ResultCodeService.
Assert the actual method/headers/query/form/body and path parameters, not merely a non-null response. Add independently confirmed JSON cases for verify=false, key-query errors, HTTP non-2xx and business failure; each case executes the real SPI. See the authoritative [scenario format](../TESTING.md#3-add-independent-protocol-cases), not a separate testing schema here.

## 3. Local build

From the adapter project root:

```bash
mvn -Dtest=GeneratedStructureTest test
mvn clean verify
mvn dependency:tree
jar tf target/<artifactId>-<version>.jar
```

Use Java 8. With the CLI configured, run `aci package --project . --json` for SDK, test, dependency-tree and plain-JAR checks; reports are in `target/aci-delivery/`.
common-sdk 1.5.2 comes from the `lib/repository` file Maven repository with provided scope. No install-sdk command is needed.
This file repository is not a complete offline dependency cache: Spring and test libraries still need accessible repositories or local cache. The supplied JAR/POM must match common-sdk.version; do not mix different SDK versions in one package.

## 4. Deliverable checks

- Plain adapter JAR, not executable Spring Boot fat JAR/Ark Biz; no main-application startup configuration.
- Manifest Implementation-Version and IAIS-SDK-Version match the adapter and AIS SDK versions in delivery documentation.
- Replace organization coordinates, use a unique package, and update Spring scan paths.
- Do not include copied com/alipay/iacqintegrationhub/channel/sdk classes or bundle host Spring/logging implementations.
- List coordinates/versions for added libraries and obtain host compatibility confirmation; a plain JAR does not automatically install its dependencies into the host.
- Review additional provided dependencies too: they require host-supplied libraries and can fail at runtime even when local compilation succeeds. The CLI reports both runtime and unexpected provided dependencies for platform review.
- Wire all selected SPIs, remove unselected capabilities and synchronize platform registration.
- Exclude real keys, card credentials and production payloads. Public materials must not expose local paths; use shareable synthetic test fixtures.
- Provide a capability matrix, field mappings, result-code mappings, security rules, dependency inventory, test reports and platform configuration checklist.

## 5. Common symptoms and first checks

| Symptom | Check first |
| --- | --- |
| SPI_METHOD_NOT_REGISTERED | Channel Facade registerSpiOperations, not only adapter beans |
| SPI_NOT_REGISTERED/missing service | Package scanning, loaded JAR and module-local SPI bean |
| Multiple beans of one type | Multiple adapter implementations scanned or duplicate implementations in the same module |
| Missing channel route config | channelCode, operation key and complete normal/sandbox configuration |
| Platform HTTP rejects route | Modified original context or lost thread context |
| UnsupportedOperationException | Unimplemented optional default method or incorrect invocation scope |
| Null output/empty fields | Placeholder SPI or mapping not based on real institution response |
| HTTP 200 but business failure | Institution business codes and platform mapping, not HTTP status alone |
| Non-2xx cannot be mapped | Default 2xx policy; override anonymous extension acceptResponseStatus only for confirmed business responses |
| Signature verification failure | Serialization, path substitution, encoding, key purpose and algorithm output format |
| Endless notification retries | Platform ACK format/status versus institution protocol |
| NoClassDefFoundError/NoSuchMethodError | Host versus provided SDK/library versions; SOFABoot has no per-module class-loader isolation |

Errors should retain necessary request correlation, but not keys or complete sensitive payloads. Existing logs are not a redaction guarantee; review content before adding logs.

## 6. Acceptance criteria

Every selected capability has reproducible protocol-applicable success, failure and unknown/processing scenarios. Exceptions do not become success; notifications are verified before forwarding; missing sandbox configuration does not call production; platform registration matches the JAR implementation. Mocked platform computation validates arguments/assembly, not actual cryptography; record [host conformance](../maintainer-validation.md#3-platform-host-conformance) separately.
When a downstream scenario is unavailable, record it as not applicable with a reason rather than deleting it from the test checklist.

Evidence: scaffold POM, src/test, ChannelInvocationTemplate, ChannelSpiCapabilityValidator and AbstractFacadeSupport.
