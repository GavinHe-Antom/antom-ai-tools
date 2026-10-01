# Reference Baseline and Maintenance

## Current baseline

- SDK contract/artifact review date: 2026-09-29.
- Platform source baseline: d68f51c. This update synchronizes three SDK additions; it does not mean every other platform implementation description was re-audited. Existing public SPI, security, HTTP, routing and result-code APIs were compared.
- CLI template 0.1.0 pairs with common-sdk 1.5.2. Java 8 and Maven 3.6.3+ are required; macOS/Linux are supported, Windows is unverified.
- The public repository contains no SDK. The platform supplies an authorized 1.5.2 JAR and standalone consumer POM separately. Generated projects use a file repository with provided scope and record JAR/POM SHA-256.
- Machine-readable contracts contain four SPI interfaces, 14 methods and 108 types, including ChannelRequestContext, BaseChannelRequest.runtimeEnv and PaymentNotifyRequest.extendInfo. The 22 security types are unchanged.
- Field tables and machine contracts are reference snapshots, not runtime bean-validation rules. The actual delivered SDK JAR takes precedence.

Authorized SDK JAR SHA-256 used in verification: `2f3bfab41689bad8b1dda92b811ad9c72eb595d558b3d390c97ae1c318ca486c`; consumer POM: `1e8a31b05b3acee60bc916d344e11d216e659511569a59dcd5a509e5a9e6777d`. These identify the verified artifacts, not a permanent allowlist for future valid builds. The SDK itself remains outside the public repository.

Earlier artifacts also labeled 1.5.2 may lack the added APIs. The CLI first checks for ChannelRequestContext; generated compilation and structure tests verify the fields. generation-lock.json records JAR/POM hashes, and package checks Maven's actual resolved artifacts. Do not edit the lock to conceal inconsistent cached artifacts.

Adding PaymentNotifyRequest fields also changes its Lombok all-arguments constructor signature. Examples use builders or setters. Recompile and validate existing adapters; replacing a same-version JAR alone does not prove binary compatibility. Future platform deliveries should use distinct version numbers to avoid overwriting artifacts at identical coordinates.

## Content organization

SPI pages describe methods and applicable flows, model pages maintain business types, security.md covers security services, and contracts.json supports machine lookup.
Update related pages when changing types; do not duplicate the full catalog throughout the documentation.

This package covers responsibilities, APIs, mapping, security, routing, testing and delivery needed for external integration. It does not include live environment configuration or the entire internal platform.
Reading does not require an internal knowledge base. Institution rules and actual mappings come from the integration project.

## Maintenance steps

1. Compare actual SDK methods, inherited fields, default methods and enums; update type summaries and field notes.
2. Check platform entry points and capability registration; distinguish declared contracts from usable flows.
3. After template changes, check SPI wiring, security order, serialization and exception paths.
4. Validate links, JSON, model source SHA-256 and selected SPI coverage.
5. Run Agent tasks against synthetic projects and record actual changes, commands and results; structural checks do not replace behavioral validation.
6. Record SDK/template versions and actual artifact checksums in release notes.

## Public-distribution boundary

The Skill uses Apache-2.0; SDK JARs and scaffolds are supplied separately. The license does not imply publication approval for all platform fields, result codes or third-party institution materials.
Do not include real keys, production configuration or real card data. Original key material must not enter ordinary delivery packages.

CLI source, templates and local checks are implemented. Whether they have been built, supplied with an SDK and validated depends on the actual environment and evidence.
Documentation and synthetic tests do not establish successful real-institution integration or public-release approval.
