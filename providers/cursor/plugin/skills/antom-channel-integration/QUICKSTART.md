# Antom Adapter Integration Quickstart

## Choose your task

- **Understand the platform:** load [SKILL.md](SKILL.md) and start with [system boundaries](references/guide/01-system-and-boundaries.md). No CLI, SDK or platform access is required for offline questions.
- **Create an adapter:** build the bundled CLI, obtain the authorized SDK, generate a separate project, and implement the confirmed protocol rules.
- **Continue an existing adapter:** load the Skill and inspect that project's actual SDK and structure. Do not run `init` over existing source.

This Skill is shared under `skills/antom-channel-integration/` in Antom AI Tools and mirrored into its provider packages. Copy or install the entire Skill directory, including references and scripts, rather than just its entry point. The Skill and CLI use English.

## Build the CLI

Java 8 and Maven 3.6.3+ are required for adapter development. macOS/Linux are supported; Windows is unverified. Commands below run from this Skill's directory, the directory containing `SKILL.md` and this quickstart:

```sh
java -version
mvn -version
mvn -f scripts/ais-cli/pom.xml clean verify
sh scripts/ais-cli/bin/ais --help
```

Build dependencies require a reachable Maven repository or an existing cache. This step builds the CLI only; it needs no platform SDK and generates no adapter. The CLI does not change PATH or Maven settings.

## Prepare the SDK

Obtain SDK **1.5.2** and its standalone consumer POM through the platform's authorized delivery channel. Place them locally as:

```text
scripts/ais-cli/sdk/common-sdk-1.5.2.jar
scripts/ais-cli/sdk/common-sdk-1.5.2.pom
```

Use the consumer POM, not the platform source POM with an internal parent. The launcher discovers these files relative to the CLI installation, not your current working directory; there is no `install-sdk` command. They must not enter public Git or release archives. Compare [platform delivery checksums and APIs](references/guide/12-evidence-and-maintenance.md): a matching version label alone does not establish compatibility.

## Generate a separate adapter

When asking an Agent to generate a project, supply the [generation inputs](references/project-generation.md#confirm-the-generation-inputs): institution code, card/non-card and applicable 3DS mode, selected transaction/notification scenarios, output directory, and whether signature handling and encryption handling are needed. These are the only two security questions for scaffold creation; algorithms, order and computation mode are implementation-stage decisions. The Skill asks about missing requirements and waits for your answers. Once the institution code is supplied, it directly derives and uses channelCode, Maven coordinates and the Java package without asking you to accept a naming proposal. Explicit identifiers are preserved. A complete specification does not require another questionnaire.

From this Skill directory:

```sh
sh scripts/ais-cli/bin/ais init --output /absolute/path/to/my-adapter
```

Use Up/Down and Enter for choices, and Ctrl+C to cancel. Select card/non-card, 3DS, SPI and notification scope, then answer the two global signature/encryption yes/no questions. Confirm refund and refund inquiry separately from refund notification; only selected transactions are generated. The output must be nonexistent or empty. Install a project-local Skill only **after** generation, since an installation directory would make the target nonempty.

For agents and CI, use JSON instead of simulating numeric terminal input. Preview the synthetic example without writing a project:

```sh
sh scripts/ais-cli/bin/ais init \
  --config scripts/ais-cli/examples/non-card.json \
  --output /absolute/path/to/my-adapter --dry-run --json
```

For real generation, provide your confirmed configuration file and omit `--dry-run`. See [CLI configuration and commands](scripts/ais-cli/README.md). The example describes a synthetic protocol, not institution requirements.

## Implement and test

Use the [integration rules template](assets/integration-rules-template.md) to provide field mappings, amount units, signing/encryption rules, results and independent synthetic scenarios. Implement transaction mapping in each SPI method's anonymous `ChannelApiExtension`, notification mapping in its anonymous `ChannelNotificationExtension`, and security/transport in `customize/security` and `customize/transport`. No `customize/api` helper classes are generated. Enabled security features contain platform-call examples with typed hooks that fail until the protocol is implemented; decide platform versus custom computation during implementation. Use the actual SDK types and the provided platform services; do not configure HTTP domains, access IBCM directly, or replace platform identity/context.

In the **adapter root**, run:

```sh
mvn -Dtest=GeneratedStructureTest test
mvn clean verify
```

The first command checks structure, not protocol correctness. Initial delivery-test failures are expected until you implement the business hooks; do not skip them. Tests should exercise the real SPI, mock platform boundaries, and use independent expected requests and security fixtures. See [testing](references/TESTING.md) and the [SPI contracts](references/guide/reference/spi/README.md).

For project-local discovery in Codex, copy this entire Skill directory into the generated adapter's `.agents/skills/antom-channel-integration/`, then refresh discovery. Other agents use their supported Skill installation locations. Do not include local build output or the SDK when copying a Skill for distribution.

## Package and hand off

After full verification, run from the **adapter root**, replacing the launcher path with your actual Skill installation:

```sh
sh /absolute/path/to/antom-channel-integration/scripts/ais-cli/bin/ais package --project . --json
```

The command reruns tests and validates the SDK, resolved dependencies, per-method evidence and ordinary JAR. Reports are written to the adapter's `target/ais-delivery/`. It does not upload or configure the platform. Maven executes project code; only build trusted projects.

Prepare the [delivery checklist](references/delivery.md), including the implemented method list, security rules, result-code mappings and test evidence. Local success does not replace host validation, institution integration testing or release approval. Do not send real keys or card credentials in prompts, public issues or ordinary delivery packages.

The Skill and bundled CLI use [Apache-2.0](LICENSE); the separately supplied SDK and institution materials retain their own rights. For questions and contributions, follow the Antom AI Tools repository's community and security policies.
