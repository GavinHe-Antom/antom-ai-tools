# AIS Channel Adapter Delivery Files and Templates

This section recommends consistent file names and content for offline collaboration. It is not a format already validated automatically by the CLI. Markdown, CSV or Excel may be used by agreement with the platform, but required information must not be omitted. Explain why an item is not applicable instead of leaving an ambiguous blank.

## Delivery package

```text
delivery/
├── 00-integration-overview.md
├── 01-spi-inventory.csv
├── 02-field-mapping.xlsx
├── 03-security-design.md
├── 04-key-material-manifest.csv
├── 05-result-code-mapping.xlsx
├── 06-endpoints-and-notifications.xlsx
├── 07-test-and-integration-report.md
├── 08-dependencies-and-release.md
├── artifacts/
│   └── <adapter-artifactId>-<version>.jar
├── tests/
│   └── Synthetic inputs, expected requests, responses, notifications and reproducible cases
└── reports/
    └── Unit test reports and dependency tree
```

**Do not put original key material in this directory.** The key manifest records identifiers, purpose, format and handoff status only. Transfer actual symmetric/private keys separately through the platform-designated secure channel, or let the platform generate and manage them. Ordinary packages must not contain real card data or CVV.

| File | When supplied | Responsibility and purpose |
| --- | --- | --- |
| 00 Integration overview | Before development; update at delivery | Developer defines scope; platform confirms identity/environment |
| 01 SPI inventory | Before development; finalize before integration testing | Platform assembly and registration, verified method by method |
| 02 Field mappings | Before coding; maintain with implementation | Cover requests, responses and notifications |
| 03 Security design | Confirm before security coding | Choose platform or custom computation for every direction |
| 04 Key material manifest | Before environment integration | Both parties prepare and bind materials without documenting secret values |
| 05 Result-code mappings | Before mapping implementation; configure before integration | Institution raw codes to platform-standard combinations |
| 06 Endpoints and notifications | Before integration | Platform configures normal/sandbox routes, public entry points and ACK |
| 07 Test/integration report | After local tests, before delivery | Passed/failed/not-run results and reproduction steps |
| 08 Dependencies and release notes | With every artifact delivery | Coordinates, SDK, dependencies, changes and checksums |
| JAR and test materials | At delivery | Plain JAR with reproducible synthetic scenarios |

## 00-integration-overview.md: integration overview

| Item | Required content |
| --- | --- |
| Institution/product and payment method | Institution name, card/non-card, currencies/regions |
| channelCode | Platform-assigned value |
| 3DS interaction | One call / two calls / not applicable |
| Adapter / SDK | Maven coordinates, version, SDK version and package |
| Business scope | Selected payment, query, cancel, capture, refund and notification methods |
| Merchant relationship | Platform merchantId to institution Client-Id/merchant-account mapping |
| Environments | Integration environment, normal/sandbox differences and enablement scope |
| Protocol evidence | Document name, version, sections/offline attachment name |
| Contacts | Development, testing, platform configuration and institution integration owners |
| Known limits | Unavailable capabilities, unsupported payloads/algorithms/interaction modes |
| Open questions | Issue, affected methods, owner and confirmation status |

## 01-spi-inventory.csv: implemented interfaces and methods

Use one row per method, including unimplemented optional methods, to prevent incorrect platform registration.

| Column | Description |
| --- | --- |
| Fully qualified SPI interface | For example com.alipay.iacqintegrationhub.channel.sdk.spi.payment.PaymentService |
| Method | Exact name such as pay or authenticateAuthorize, not vague "payment supported" |
| Input/output types | Match the SPI reference |
| Implemented / reason not implemented | Yes, no or to confirm |
| Implementation class/source path | Actual class and method the platform can locate |
| Bean scan scope | Actual package and Spring XML |
| Platform operation/route key | For example AUTHORIZE / authorize, confirmed by platform |
| Standard code catalog API | For example INITPAYMENT, listed independently |
| Parameter-center api | Exact contract identifier; do not infer from other names |
| Institution API/method | Business name, GET/POST, etc.; actual domains in endpoint inventory |
| Security design ID | Specific scenario in file 03 |
| Mapping/result-code sheet | Traceable rules for this method |
| Unit/integration status | Test name, report location and passed/failed/not-run |
| Platform enablement confirmation | Owner and confirmation record |

Implemented methods must not remain return null. Unsupported optional methods must not fabricate success. Implementation and platform registration inventories must match in both directions.

## 02-field-mapping.xlsx: field mapping

Use applicable request, response and notification sheets for each SPI.

| Column | Description |
| --- | --- |
| SPI/method/direction | Standard request → institution, institution response → standard response, institution notification → standard notification |
| Full source field path | For example paymentAmount.value or institution data.payment.id |
| Full target field path | Institution or SDK path, not only a leaf name |
| Source/target types | String, integer, array, object, etc. |
| Required condition | Unconditional or precise business condition |
| Conversion | Amount units, timezone, enum, date, encoding and precision |
| Missing/null behavior | Throw, omit per protocol or use a valid default; never default to success |
| Example input/expected output | Synthetic data; card fixtures must have no real transaction value |
| Security participation | Whether included in signing/digest/encryption and processing order |
| Implementation/test location | Locate code and independent expectation |
| Confirmation status | Unconfirmed items and owners |

List each Map extension key. If security requires rawBody, do not reconstruct original text after mapping. Amount conversion must state currency and units, not simply "divide by 100".

## 03-security-design.md: security design

**Document each scenario, not merely "uses HMAC".** Request signing, response verification, notification verification, request encryption and response decryption may follow different rules.

| Item | Required information |
| --- | --- |
| Security design ID/applicable SPI | Method, direction, normal/sandbox |
| Computation mode | Platform sign/verify/encrypt/decrypt/digest, or queryKey plus custom adapter computation |
| Algorithm | Full name; exact platform enum, or all custom protocol parameters including digest/padding/curve |
| Key purpose/ownership | SIGN/VERIFY/ENCRYPT/DECRYPT; platform private key, institution public key or shared symmetric key |
| Merchant identity | Relationship of platform ChannelRequestContext merchantId to institution merchant/Client-Id |
| Key material ID | Reference file 04; do not include the value |
| Material format | Raw text/Base64/PEM/DER, key length and decoding count |
| Signing-input construction | Full field paths, ordering, case, nulls, separators and trailing newline |
| Body/path handling | Body before digest, actual encoded path, Query inclusion and request/notification differences |
| Execution order | Encrypt-then-sign or another explicit sequence; reverse response processing |
| Output format/placement | HEX/Base64, prefix, header/JSON field name |
| Time/randomness | Format/timezone, validity window, random fields and deterministic test setup |
| Failure policy | Stop sending/notification mapping, propagate exceptions; never ignore false |
| Custom implementation | Classes/methods and library coordinates/versions, or explicitly not applicable |
| Verification materials | Synthetic keys, input, expected digest/signature/ciphertext/verification result; real materials managed separately |

Provide both readable signing input and precise byte rules. Synthetic test keys do not replace confirmation that real environment materials are ready.

## 04-key-material-manifest.csv: key handoff inventory

| Material ID | Merchant/institution identity | Environment | Purpose | Material type/owner | Algorithm/length | Storage/transport format | Provider/recipient | Secure handoff record or managed reference | Platform binding/verification status |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| KEY-001, template ID | To supply | Normal/sandbox | To supply | To supply | To supply | To supply | To supply | Reference only, never key value | Pending/complete |

Material preparation rules:

- Transfer shared symmetric keys for platform management according to institution protocol; distinguish raw values from encoded storage values.
- For institution signature verification or encryption to the institution, supply its public key/required certificate, not its private key.
- Platform signing/decryption uses the platform private key, generated or securely imported under the agreed process. Institutions generally need only the corresponding public key.
- List normal and sandbox materials separately. Explicitly confirm if identical; never assume production material reuse.
- Record purpose, format, version/rotation notes and handoff evidence only. Runtime queries select current material; adapters cannot request a version.
- Never send original keys through ordinary chat, review, Git, test resources or logs. The platform names the actual transfer system/people; do not invent an upload capability.

## 05-result-code-mapping.xlsx: institution-to-platform mappings

Each row is a confirmed mapping for one API scenario. Recommended columns:

| Column | Owner/meaning |
| --- | --- |
| channelCode | Platform-confirmed |
| SPI method | Developer supplies actual method |
| Standard code catalog API | Select from the catalog; platform confirms applicability |
| Parameter-center api | Platform confirms exact contract scope |
| HTTP status condition | For example applies only to parseable business responses; include non-2xx |
| Institution primary-code field/raw value | Institution field and original value |
| Secondary/tertiary-code fields/raw values | Supply when applicable; otherwise mark not applicable |
| Actual combined query code | Match runtime pipe-joined raw values |
| Institution message field/business meaning | Meaning, finality and whether accepted |
| Platform-standard channel return status | Target resultStatus: S/F/U |
| Platform-standard secondary result code | Target resultCode, such as SUCCESS, APPROVED, INVALID_AMOUNT |
| Upstream mapping/transaction-state reference | Reference the confirmed standard table; do not put this in Result.resultCode |
| Next processing step | Finish, await authentication/notification or query original transaction; justify retries |
| Test case/expected Result | At least one reproducible synthetic response |
| Confirmation/platform configuration record | Both parties verify before release |

This directional example explains structure only; it is not a confirmed mapping for any actual institution:

```text
Institution raw code=<institution-invalid-amount-code>
→ Target resultStatus=F, resultCode=INVALID_AMOUNT
→ Publish platform configuration using that raw code, channelCode and confirmed api
```

Rules:

1. List transaction, query and notification mappings separately; identical codes may have different API meanings.
2. Cover success, authentication required, accepted, processing, explicit failure and unknown.
3. For missing institution codes or network failures, document protocol/exception handling separately; do not invent raw codes.
4. Missing configuration must not map to success. Confirm ambiguous standard combinations before release.
5. The source table's platform secondary result code is not the institution subcode; use distinct column names.

## 06-endpoints-and-notifications.xlsx: endpoints and notifications

Institution egress sheet:

| channelCode | SPI method | Operation key | Normal domain/path | Sandbox domain/path | HTTP method/Content-Type | Path placeholders/sources | Query/Form rules | Signing-target representation | Network/timeout/idempotency requirements |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| To supply | To supply | To supply | To supply | To supply | To supply | To supply | To supply | To supply | To supply |

Notification sheet:

| Notification type | Platform-assigned receiving address | Merchant identity | Original notification example | Security rule ID | Standard correlation ID | ACK status/headers/body | Retry intervals/count | Verification-failure behavior | iPay forwarding confirmation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| To supply | Platform-confirmed | To supply | Synthetic data | To supply | To supply | To supply | To supply | To supply | Platform supplies |

Platform domain ownership does not remove the developer's obligation to provide institution endpoint information. It means the platform configures/selects addresses at runtime instead of hard-coding them in adapters.

## 07 and 08: verification and artifact notes

The test/integration report must include:

| Content | Requirement |
| --- | --- |
| Run conditions | SDK/adapter/JDK versions, command, environment and scenario ID |
| Results | Passed, failed and not run separately; skipped is not passed |
| Request verification | Compare actual platform HTTP parameters against independent expectations |
| Security verification | Computation mode, vectors, exact input, tampering and failure blocking |
| Validation layers | Local SPI cases, custom-library vectors, real platform security conformance and institution integration reported separately |
| Coverage gaps | Applicable categories not exercised, reason, owner and follow-up; a passing class name is not completeness |
| Standard results | resultStatus/resultCode, amounts, correlation IDs and notifications |
| Sandbox/routing | Correct egress and material selection, without mixing normal traffic |
| Open issues | Impact, owner, delivery-blocking status and follow-up confirmation |

For CLI projects, include `target/aci-delivery/report.json`: `executedTests` and `scenarioEvidence` identify actual testcase names, case IDs/categories and fixture hashes. Every selected method must retain at least one confirmed returning success/processing/protocol case; all expected-error cases are insufficient. Review protocol applicability instead of treating counts as certification. `platformConformance` is `not-executed-by-aci-package`; supply separate host/institution evidence when completed.

Artifact notes must include:

- JAR filename, Maven coordinates, adapter/SDK versions and source revision.
- Manifest `IAIS-SDK-Version` matches the documented AIS SDK version.
- SHA-256, build command, Java version and dependency tree.
- Additional dependency coordinates/versions/purpose and host-compatibility conclusion.
- Implementation package, Spring XML, capability inventory and configuration-list version.
- Changes, known limits and required platform assembly/configuration actions.
- Verification that duplicate SDK classes, host runtime, test classes/data, real keys and card data are absent.
