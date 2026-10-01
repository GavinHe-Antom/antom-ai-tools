# From Generated Project to Callable Adapter

## 1. Prepare before starting

Confirm SDK/template versions, channel code, card or non-card integration, 3DS interaction mode, transaction/notification scope, institution protocol, field mappings, result-code rules, synthetic examples and the platform configuration owner.
Institution examples are not standard SDK protocols. Do not copy another channel's fields, success codes, headers or signatures.

The default workflow uses CLI 0.1.0; see [project generation](../project-generation.md). The platform supplies the SDK 1.5.2 JAR and standalone consumer POM separately. Prerequisites: Java 8 and Maven 3.6.3+.
For a generic scaffold supplied directly by the platform, read [scaffold differences](../baseline-scaffold.md) first; do not assume identical class structures.

## 2. Select capabilities by business scope, not class names

| Integration choice | CLI and SDK constraints | Implementation |
| --- | --- | --- |
| Card, one payment call | pay is required | Select cancel/capture/query according to institution support |
| Card, separate authentication and authorization calls | pay + authenticateAuthorize | Use the two distinct request types; do not cast PayRequest |
| Non-card | pay is required; capture/notifyCapture unavailable | Select queries, refunds and notifications as needed |
| Refunds | Implement refund when selecting RefundService | Select inquiryRefund as needed |
| Notifications | Implement notifyPayment when selecting NotificationService | May add notifyCapture/notifyRefund; refund-notification-only generation is not supported |
| Online-bank/receive-payment notifications and URL callbacks | SDK declaration does not imply a connected entry point | CLI does not generate capabilities whose entry points are not connected |

Java abstract methods are required only when implementing their interface; not every channel must offer refunds or notifications. CLI 0.1.0 additionally requires pay.
Do not override unselected optional methods; preserve the SDK's default UnsupportedOperationException instead of returning null or fabricated success.
Capability changes must update configuration, implementation, tests and platform registration together. Do not rerun init over an existing project.

## 3. Default project structure

| Location | Generated content | Integrator work |
| --- | --- | --- |
| spi/Channel*Service | Selected methods delegate to the invocation template and corresponding Mapping | Verify scope; do not recreate helper wiring |
| `customize/api/<Method>Mapping` | Independent extension per selected method, such as PayMapping and NotifyRefundMapping | Validation, field conversion, path parameters and result mapping |
| customize/security/ChannelSecurityCustomization | Protocol-selected directions/order; platform computation or key-query branches | Merchant identity, signing input, encoding, runtime parameters and result placement |
| customize/transport/ChannelTransportCustomization | JSON/POST starting point | Set institution method, headers, Query/Form and Content-Type |
| src/test/java | GeneratedStructureTest; SecurityContractTest and DeliveryTest for each method | Distinguish synthetic template validation from real SPI acceptance; add institution scenarios |
| `src/test/resources/scenarios/<method>` | input, expected-request, response, response-headers, expected-result and security JSON | Supply synthetic protocol fixtures; do not derive expectations from the implementation under test |
| adapter-spec.json / generation-lock.json | Selections, SDK/template versions and SDK JAR/POM SHA-256 | Preserve provenance; do not edit lock values to bypass validation |

Transaction SPIs use executeDynamicUrl by default. Mapping returns an empty Map when the path has no placeholders; otherwise it returns raw business values for platform encoding.
Notification SPIs use executeNotification and return a standard notification request, not an ACK or a direct iPay call. See the [implementation workflow](../implementation-workflow.md).

## 4. From local verification to platform integration

1. Complete selected Mapping and security hooks; replace initially empty fixtures.
2. Run `mvn -Dtest=GeneratedStructureTest test` to check Spring wiring; this does not prove business completeness.
3. Run `mvn clean verify` for real SPI tests, including security failures, unknown business results and exceptions.
4. For a trusted project, run `ais package --project . --json` and inspect reports, dependencies and the plain JAR.
5. Ask the platform to configure bean scanning, uniqueId, capability registration, routes, iPay identity, keys and result codes.
6. Record host validation and institution integration separately. Local success does not mean routes, keys, notifications or ACK behavior are live.
