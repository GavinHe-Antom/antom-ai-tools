# Project Creation and CLI Boundaries

## Available workflow

This Skill's `scripts/ais-cli/` contains Java CLI source, versioned templates and tests. Resolve that directory relative to this Skill's `SKILL.md`, not to the adapter project. Build there with `mvn clean verify`, then run `sh bin/ais --help`.
The baseline is Java 8 and Maven 3.6.3+. macOS/Linux are supported; Windows is unverified. Installing the Skill includes CLI source, but does not build it, install Java/Maven, or supply the SDK. See the [quickstart](../QUICKSTART.md).
Examples using `ais` assume the CLI bin directory is on PATH. Otherwise use its actual launcher or `java -jar <cli-path>/ais-cli.jar`; do not assume an identically named command is installed.
Without the CLI, use a platform-supplied project, check [scaffold differences](baseline-scaffold.md), and test/package with Maven. Do not invent installer packages or download URLs.

## When the user provides a CLI

1. Confirm the tool's source, inspect --help, version and command help; the name ais alone does not establish identity.
2. Verify template/SDK compatibility, then collect coordinates, unique package, card/non-card, 3DS, SPI and notification scope.
3. Confirm signing, verification, encryption, decryption and computation modes by method and direction. Missing protocol rules cannot be replaced with default success.
4. Prefer supported noninteractive input and structured output. Inspect a preview file list when available; do not overwrite existing projects.
5. Generate in the designated directory, then implement real mappings/security rules. A compiling skeleton is not a finished integration.
6. Run real tests before packaging. Report missing SDK, required implementations or security rules instead of skipping tests and declaring delivery readiness.

## Commands

Template 0.1.0 supports SDK 1.5.2. Neither the public repository nor distribution contains the SDK. Obtain the authorized JAR and standalone consumer POM from the platform and place them in the CLI's local sdk/ directory; they are read automatically without prompting for paths.
Humans can run `ais init` in a real terminal, use arrow keys to select, Enter to confirm and Ctrl+C to cancel. Only free-text coordinates, package, channel and protocol descriptions require typing. Piped numeric selections are unsupported.
Agents should use `ais init --config adapter-spec.json --output ./my-adapter --dry-run --json`, inspect the plan, then remove --dry-run. Do not simulate numbered line-by-line input. The source distribution includes examples/non-card.json; do not assume a binary ZIP includes it.

JSON omits sdkJar/sdkPom by default. If overrides are necessary, supply both; relative paths resolve from the configuration-file directory. Output paths resolve from the working directory.
When the SDK is absent, explain the required version/location and ask the user to obtain it through the authorized platform channel. Do not download untrusted JARs, invent dependencies or add the SDK to the public CLI package.
Every selected method declares security directions: request sign/encrypt and response/notification verify/decrypt. Order them according to protocol; none also requires evidence. platform selects a signing/cipher algorithm; adapter selects keyAlgorithm, with queryKey purpose derived from the operation.
Populate IV/AAD/PGP in typed request hooks. Do not fix production randomness or enter real keys.

After generation, implement business logic in customize/api, customize/security and customize/transport. `GeneratedStructureTest` validates structure only; `*SecurityContractTest` uses synthetic hooks to verify template security contracts. Initial `*DeliveryTest` is expected to fail because real business logic is unfinished.
For a trusted project, `ais package --project ./my-adapter --json` runs all tests, dependency-tree and plain-JAR checks. Reports appear in target/ais-delivery; the command neither uploads nor configures the platform. Maven plugins execute code, so package is not a malicious-code sandbox.

The SDK uses a file repository with provided scope; no install-sdk command is required. Correct version mismatches, nonempty output directories and missing policy rules instead of bypassing tests. Local CLI success is not proof of protocol correctness, vulnerability clearance or rollout approval.

The authorized SDK 1.5.2 must contain ChannelRequestContext, BaseChannelRequest.runtimeEnv and PaymentNotifyRequest.extendInfo. Version equality alone is insufficient: compare platform-supplied JAR/POM checksums. The CLI rejects a missing context class. Do not edit generation-lock.json to conceal Maven cache mismatches.

Generated security code reads merchantId from ChannelRequestContext. Protocol hooks supply input, signatures, parameters and result placement, not another merchant identity. Tests use independent context.json fixtures to simulate platform identity; follow [testing](TESTING.md) and always clear context in finally.
