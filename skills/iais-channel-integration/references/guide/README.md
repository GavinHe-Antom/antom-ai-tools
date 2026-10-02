# Channel Adapter Integration Guide

This guide is for external adapter developers, platform engineers assembling channel modules, and development-assisting LLMs. Its goal is to turn an institution protocol into callable, verifiable and deliverable Java SPIs, not to create another gateway or HTTP server.

## Applicability

- SDK: `com.alipay.iacqintegrationhub:common-sdk:1.5.2`; callback contract review date 2026-10-02. Invocation context, runtimeEnv and payment-notification extendInfo remain available. Identical versions do not guarantee identical artifacts; see [maintenance](12-evidence-and-maintenance.md) for source, review scope and checksums.
- Implementation instructions default to CLI template 0.1.0. The platform supplies the authorized SDK JAR and standalone consumer POM separately; the public repository has no SDK.
- The SDK has three SPI interfaces and 12 methods. Confirm platform entry points, channel registration and adapter implementations separately.
- Vaulting and disputes are excluded. CLI source is available; check whether it is built and supplied with an SDK in the current environment. Two optional notification methods have SDK contracts only, without connected entry points; the generator excludes them. Browser ACS callbacks are platform-owned, not adapter SPIs.
- Implementation facts come from code. Integration rules are engineering requirements for delivery. Examples are not real institution protocols.
- Field tables describe Java types, inheritance, original JavaDoc and defaults, not an approved business JSON Schema. Do not infer undeclared requiredness, lengths or enum conditions.

See [payment field definitions](reference/payment-sources.md) and [refund field definitions](reference/refund-sources.md) for business meaning, population rules and formats.

## Choose a reading path

| Role/task | Recommended order |
| --- | --- |
| First-time integrator | [System purpose](01-system-and-boundaries.md) → [flows](02-flows.md) → [setup and selection](03-start-and-tailor.md) → selected SPI |
| Field mapping | [Mapping and result codes](04-mapping-and-results.md) → [SPI index](reference/spi/README.md) → [shared models](reference/models/README.md) |
| Signing, verification or encryption | [Security integration](05-security.md) → [security contracts](reference/security.md) → institution test vectors |
| Reading channel, merchant and environment | [Invocation context](reference/context.md) → [test identity simulation](../TESTING.md) |
| Addresses, sandbox or HTTP properties | [Domains, sandbox and HTTP](06-routing-and-http.md) → [HTTP contracts](reference/http.md) |
| Notifications/browser callbacks | [Notifications and callbacks](07-notifications-and-callbacks.md) → selected notification SPI |
| Platform assembly and rollout | [Platform handoff](09-platform-handoff.md) → [testing and delivery](08-testing-and-delivery.md) |
| LLM/Agent | [AI collaboration](10-ai-reading.md) → [machine contracts](reference/contracts.json) → selected topics; do not load every document at once |

## Five essential facts

Platform fields are documented by actual lifecycle stage: guaranteed fields are marked as platform-injected and always present. Fields that are inapplicable, populated later or retained by the adapter are described separately. See [platform-provided fields](reference/spi/README.md#platform-provided-fields).

1. An adapter is a plain JAR in the Spring context of a platform channel SOFABoot module, not a standalone application.
2. Adapters validate/map fields, implement institution message security and convert business results; all institution HTTP uses platform services.
3. The platform selects channel identity, egress domain/path and sandbox routes. Adapters do not choose production or sandbox servers.
4. SDK method declaration, Java override, platform registration and HTTP reachability are four separate conditions.
5. The CLI wires selected SPIs only. Anonymous mapping extensions inside each SPI method and enabled security hooks still need implementation; passing GeneratedStructureTest does not make transactions operational.

## Organization and evidence

Each topic covers one concern. Per-method contracts have separate input/output tables, while shared model pages maintain nested objects.
[Current limits](11-current-limits.md) centralizes incomplete capabilities instead of hiding them in footnotes.
[Evidence and maintenance](12-evidence-and-maintenance.md) records source baselines, update procedures and validation rules.

This directory remains self-contained when copied to an external project and does not require internal Yuque login. Do not put private institution protocols, real mappings, production keys or merchant data in public documentation.
