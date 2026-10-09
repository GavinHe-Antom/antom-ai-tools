# Maintainer Validation and Evidence

This page defines maintainer checks, not adapter business implementation. Follow [adapter testing](TESTING.md) when integrating an institution. Repository and generated-project checks use the bundled SDK; platform host conformance requires the actual platform environment and its internal libraries.

## 1. Repository and CLI checks

From the Antom AI Tools repository root:

```sh
npm run test:skills
node skills/antom-channel-integration/scripts/check-content.mjs
node --test skills/antom-channel-integration/scripts/check-content.test.mjs
node --test skills/antom-channel-integration/scripts/check-sdk-contracts.test.mjs
mvn --batch-mode --no-transfer-progress -f skills/antom-channel-integration/scripts/aci-cli/pom.xml clean verify
```

Use Java 8 for the CLI build. The direct Node commands check local links, English content, JSON and Skill metadata, then run SDK-checker regression tests using synthetic bytecode. Run these checks locally; they do not require changes to the repository's shared CI or package scripts. CLI unit tests validate generation rules and template rendering; they are not generated-adapter compilation evidence.

Maintain this Skill only in `skills/antom-channel-integration/`, including references, CLI source and the bundled SDK JAR and consumer POM. Repository changes for channel integration are limited to that directory and the root README. Provider packages and shared repository scripts follow the repository's separate maintenance workflow. Exclude build output and confidential local integration materials from Skill source packages.

## 2. Bundled SDK and generated-project checks

Use the included SDK JAR and standalone consumer POM and confirm their hashes. From the Skill root:

```sh
node scripts/check-sdk-contracts.mjs scripts/aci-cli/sdk/common-sdk-1.5.2.jar
```

From `scripts/aci-cli/`, after the public CLI build:

```sh
node scripts/verify-generated.mjs
node scripts/verify-distribution.mjs
```

The SDK check compares API shape and field tables. Generated-project regression defaults to the bundled SDK and compiles and exercises real generated SPIs, platform mocks, request/result assertions and packaging using synthetic protocols; an explicit JAR/POM path pair remains available for artifact diagnosis. Distribution regression verifies ZIP contents and SDK hashes, then extracts the archive and runs init using its bundled SDK. Use Java 8 for these checks. They do not establish real platform crypto equivalence, real route availability or institution acceptance.

Retain actual command, exit code, SDK/consumer-POM hashes, generated project capability list and test reports. Use shareable synthetic inputs only. The bundled SDK is part of the release; platform-internal libraries, credentials and real institution data are excluded from public CI artifacts. Content checks are not security/license clearance.

## 3. Platform host conformance

Platform-owned tests must execute the real `DefaultPlatformChannelSecurityService` and BCM computation, replace only key retrieval with synthetic material, and check independently prepared protocol results. Keep the implementation in the platform test scope rather than copying it into generated adapters.

For asymmetric operations, match the correct peer direction: platform-private-key signatures are checked with the platform public key; institution-public-key ciphertext is decrypted with the institution private key; inbound institution signatures and platform-targeted ciphertext use the corresponding opposite keys. Ordinary platform sign/verify or encrypt/decrypt calls for one merchant are not necessarily a round trip over the same key pair.

From an authorized platform source checkout using Java 8:

```sh
mvn -pl app/common/service/integration -am \
  -Dtest=ExternalAdapterSecurityConformanceTest,PlatformSecurityAdapterConformanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

The first class executes an adapter with real platform security and a fixture-controlled HTTP boundary; without external properties it uses a synthetic adapter JAR instead of skipping. The second checks calculations and asymmetric peer-key behavior. They do not establish actual network bytes or live institution integration.

To validate an adapter on the AIS platform, select `ExternalAdapterSecurityConformanceTest` and supply these properties:

```text
-Dais.adapter.jar=/absolute/path/adapter.jar
-Dais.adapter.package=com.company.channel.adapter
-Dais.adapter.fixture=/absolute/path/host-fixture.json
```

Use the platform's `app/common/service/integration/src/test/resources/conformance/synthetic-hmac-pay.json` as the documented fixture example. Supply independent expected requests/results and synthetic keys only. This host-fixture format is maintained by the platform runner and is distinct from local Mockito scenario files. SDK compatibility is checked against the host artifact and adapter manifest; do not bundle SDK classes in the adapter.

The adapter manifest records the AIS SDK version in `IAIS-SDK-Version`.

Exact deterministic vectors may be compared byte-for-byte. Randomized signatures/ciphertexts require independent verification/decryption and format checks. Use synthetic keys; real environment material readiness and institution acceptance remain separate checks. See [security testing](TESTING.md#6-security-mode-2-mock-keys-and-execute-the-real-library).

## 4. Release evidence

Record the published Skill/CLI revision, template version, SDK artifact hashes, selected methods, executed scenarios and remaining host/institution gaps. Preserve historical source provenance honestly; see [baseline maintenance](guide/12-evidence-and-maintenance.md).

Failing or skipped checks are not passing evidence. A successful local package is not an upload, a platform deployment, a security certificate or an institution integration approval.
