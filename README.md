# Antom AI Tools

A one-stop repository for building AI-powered products with Antom payment integration. This repository provides shared Agent Skills, provider-specific editor plugins, and an Agent Plugins 1.0-compatible portable package with an optional Antom MCP server connection.

## Available Skills

| Skill | Description |
|-------|-------------|
| **antom-integration** | Integrate Antom payment products including One-time Payments, Tokenized Payment, and Subscription Payment. |
| **antom-reconciliation-expert** | Reconciliation Report Analysis Expert — Parses local Settlement Detail report files (CSV/XLSX) for settlement amount validation, fee analysis, and reconciliation knowledge Q&A. |
| **antom-channel-integration** | Use ACI to build institution-side AIS channel adapters: understand SPI contracts, implement field mappings and security rules, generate tailored projects, and verify testing and delivery requirements. |

## Available Plugins

Provider-specific packages remain available for supported editors and agent platforms. The repository root is also an Agent Plugins 1.0-compatible portable package, allowing compatible clients to discover the shared Skills and Antom MCP configuration.

| Platform | Type | Name | Source |
|----------|------|------|--------|
| Agent Plugins 1.0-compatible clients | Portable Plugin | `antom-ai-tools` | [`plugin.json`](plugin.json) · [`mcp.json`](mcp.json) · [`skills/`](skills/) |
| Gemini CLI | Skills-only Extension | `antom-ai-tools` | [`gemini-extension.json`](gemini-extension.json) · [`skills/`](skills/) |
| Cursor | Plugin | `antom-integration` | [`providers/cursor/plugin`](providers/cursor/plugin) |
| Claude Code | Plugin | `antom-integration` | [`providers/claude/plugin`](providers/claude/plugin) |
| Codex | Plugin | `antom-integration` | [`providers/codex/plugins/antom-integration`](providers/codex/plugins/antom-integration) |
| OpenClaw | Skill | `antom-reconciliation-expert` | [`skills/antom-reconciliation-expert`](skills/antom-reconciliation-expert) |

## Gemini CLI Extension

The repository root is a Skills-only Gemini CLI extension that bundles both `antom-integration` and `antom-reconciliation-expert` from the shared [`skills/`](skills/) source of truth.

Install it directly from GitHub:

```bash
gemini extensions install https://github.com/ant-intl/antom-ai-tools
```

For local development, validate and link the working copy:

```bash
gemini extensions validate .
gemini extensions link .
```

Restart Gemini CLI after linking, then verify that both Skills are discoverable:

```text
/extensions list
/skills list
```

### P0 Scope

- `antom-integration` provides product selection, integration guidance, code generation, and troubleshooting guidance.
- `antom-reconciliation-expert` provides reconciliation knowledge and local Settlement Detail report analysis.
- The `gemini-extension.json` manifest declares neither an MCP server nor an Antom CLI dependency.
- Online bill retrieval is outside the P0 validation scope and requires its upstream prerequisites to be satisfied separately.

## Portable Agent Plugin

The repository root follows the [Agent Plugins 1.0 specification](https://agent-plugins.org/):

- [`plugin.json`](plugin.json) identifies the portable plugin.
- [`skills/`](skills/) remains the source of truth for the bundled Agent Skills.
- [`mcp.json`](mcp.json) declares the optional Antom MCP server connection.

Installation, permissions, and authentication are managed by each compatible client. Follow the instructions provided by your client to load this repository root as an Agent Plugin.

### Antom MCP Server

The portable plugin declares the hosted Antom MCP server:

- Endpoint: `https://mcp.antom.com`
- Transport: Streamable HTTP
- Configuration: [`mcp.json`](mcp.json)
- Documentation: [Antom MCP documentation](https://docs.antom.com/ac/ref_zh-cn/mcp)

The MCP server complements the existing Skills and does not replace them. If the MCP connection is unavailable, compatible clients can continue loading the bundled Skills.

> **Access:** The Antom MCP server is currently available through a controlled allowlist rollout. See the [Antom MCP documentation](https://docs.antom.com/ac/ref_zh-cn/mcp) for access application instructions. After approval, authentication is handled through the client-managed OAuth flow; do not add credentials to `mcp.json`.

## antom-integration

### What Problem Does It Solve

Antom encapsulates core payment capabilities into standardized Skill components that are fully readable by AI. Developers can quickly integrate payments in Vibe Coding mode without dealing with complex underlying logic — simply describe your needs in natural language.

This Skill helps AI quickly load the Antom knowledge base, answer Antom integration questions, and generate integration code, significantly reducing the learning curve and development effort for payment integration.

### Install

#### Cursor

Submit / install via the Cursor marketplace, or for local development:

```bash
cp -R providers/cursor/plugin ~/.cursor/plugins/local/antom-integration
# then restart Cursor (>= 2.6)
```

Marketplace entry: [`.cursor-plugin/marketplace.json`](.cursor-plugin/marketplace.json)

#### Claude Code

```text
/plugin marketplace add ant-intl/antom-ai-tools
/plugin install antom-integration@antom
```

For local development, point the marketplace at the working copy:

```text
/plugin marketplace add /absolute/path/to/antom-ai-tools
```

Validate locally:

```bash
claude plugin validate .
```

Marketplace entry: [`.claude-plugin/marketplace.json`](.claude-plugin/marketplace.json)

#### Codex

Marketplace entry: [`.codex-plugin/marketplace.json`](.codex-plugin/marketplace.json) (legacy importer reads `.agents/plugins/marketplace.json`).

Plugin manifest: [`providers/codex/plugins/antom-integration/.codex-plugin/plugin.json`](providers/codex/plugins/antom-integration/.codex-plugin/plugin.json)

### Test Assets

When you ask AI for a test card or to test a payment, it pulls the latest sandbox test data straight from the official Antom docs (never hardcoded):

| Source | Use |
|--------|-----|
| [`docs.antom.com/ac/ref/card.md`](https://docs.antom.com/ac/ref/card.md) | Sandbox test cards (fetched live): success / failure / 3DS / risk-control / invalid-input scenarios |
| [`docs.antom.com/ac/ref/wallet.md`](https://docs.antom.com/ac/ref/wallet.md) · [`testwallet.md`](https://docs.antom.com/ac/ref/testwallet.md) | How to test wallet / BNPL / online-banking (APM) payments: test wallet app + QR / login flow |
| [`docs.antom.com/ac/cashierpay/testcases.md`](https://docs.antom.com/ac/cashierpay/testcases.md) | Go-live validation checklist (production test cases) |

In Claude Code, the plugin also exposes slash commands:

```text
/test-cards
/test-wallet
```

### Example Prompts

```text
Help me choose the right Antom payment integration.
```

```text
Generate Antom checkout integration code.
```

```text
Review my Antom payment flow for security issues.
```

### Important Notes

1. When describing your requirements to AI, be specific about the payment scenario (e.g., One-time Payment, Redirect to CKP Payment) to avoid ambiguity and potential misinterpretation.
2. Always review the integration code generated by AI and verify the logic yourself before deployment.
3. The Skill is updated in sync with Antom product iterations. It is recommended to periodically re-run the installation command to get the latest version.

## antom-reconciliation-expert

### What Problem Does It Solve

Antom delivers Settlement Detail reports to merchants, but verifying settlement amounts, understanding fee breakdowns, and attributing discrepancies manually is tedious and error-prone — especially when dealing with hundreds of rows across multiple fee types and currencies.

This Skill turns AI into a reconciliation expert. Simply provide your local Settlement Detail report file or ask a question in natural language, and AI will parse the data, validate every settlement formula, break down all fee components, and explain reconciliation rules — significantly reducing the time and effort needed for settlement verification.

### Install

#### OpenClaw

Manually copy the skill into your OpenClaw skills directory:

```bash
cp -R skills/antom-reconciliation-expert ~/.openclaw/skills/antom-reconciliation-expert
```

Verify installation:

```bash
openclaw skills list
```

### Example Prompts

```text
Help me analyze SETTLEMENT_DETAIL_202604271985548486_20260428.xlsx
```

```text
What is interchangeFee and how is it calculated?
```

```text
Validate the settlement amounts in my report and show me any discrepancies.
```

### Important Notes

1. When providing a report file, make sure it is a **Settlement Detail** report (filename must contain both `SETTLEMENT` and `DETAIL`). Transaction Detail and Settlement Summary reports are not supported.
2. Only `.csv` and `.xlsx` file formats are accepted. Other formats (`.xls`, `.pdf`, `.txt`, etc.) will be rejected.
3. Fee amounts (such as interchangeFee, schemeFee) are displayed as-is from the report. The Skill does not perform reverse rate calculation on these fees.
4. The Skill is updated in sync with Antom product iterations. It is recommended to periodically re-run the installation command to get the latest version.

## antom-channel-integration (ACI)

### What Problem Does It Solve

Antom Channel Integration (ACI), provided as the `antom-channel-integration` Skill and CLI, helps external developers implement institution protocols against the AIS platform standard and SDK SPI contracts.

It provides task-oriented guidance for SPI selection, request/response field mapping, security processing, platform-managed HTTP and routing, standard result-code mapping, testing, and delivery. The actual AIS SDK supplied by the platform takes precedence over reference snapshots; institution-specific behavior requires confirmed protocol rules.

### Skill and CLI Resources

The shared source is [`skills/antom-channel-integration/`](skills/antom-channel-integration/). Its bundled [`ACI CLI`](skills/antom-channel-integration/scripts/aci-cli/README.md) contains Java source, FreeMarker templates, tests, AIS SDK 1.5.2 with its standalone consumer POM, and an arrow-key selection workflow:

- `aci init` generates a separate adapter project for selected card/non-card capabilities, 3DS flows, notifications, and security rules.
- `aci package` runs local build, test-evidence, dependency, and ordinary-JAR checks and produces delivery reports. It does not upload artifacts or change platform configuration.
- The Skill answers integration questions and implements user-confirmed mappings and security rules in a designated adapter project; it does not deploy the platform or modify production configuration.

Load or install the complete `skills/antom-channel-integration/` directory, including its references, scripts and SDK. Gemini CLI and portable clients discover it under the shared `skills/` directory. No Antom MCP connection is required for its offline knowledge or local CLI workflows.

### Getting Started

For guidance only, load the Skill and ask a question; neither Java nor the platform SDK is required. For generation, use Java 8 and Maven 3.6.3+ on macOS/Linux. Windows is unverified. From the repository root:

```sh
mvn -f skills/antom-channel-integration/scripts/aci-cli/pom.xml clean verify
sh skills/antom-channel-integration/scripts/aci-cli/bin/aci --help
```

AIS SDK 1.5.2 and its standalone consumer POM are included in the Skill's `scripts/aci-cli/sdk/` directory. After building, run `init` directly; no separate SDK download or setup is needed. Maven also produces `target/aci-cli-0.1.0.zip` with the executable JAR and SDK for standalone use. Use the arrow keys and Enter for interactive selection; agents and CI use the documented JSON configuration mode.

```sh
sh skills/antom-channel-integration/scripts/aci-cli/bin/aci init --output ../my-channel-adapter
```

Generation produces wiring and customization hooks, not completed institution behavior. Implement the mappings and security rules, prepare independent fixtures, and pass the real SPI tests before packaging. See the [CLI setup](skills/antom-channel-integration/scripts/aci-cli/README.md#build-and-usage), [SPI index](skills/antom-channel-integration/references/guide/reference/spi/README.md), [testing workflow](skills/antom-channel-integration/references/TESTING.md), and [delivery checklist](skills/antom-channel-integration/references/delivery.md).

### Example Prompts

```text
Use $antom-channel-integration to explain the platform/adapter responsibilities for pay and notifyPayment. Do not change files.
```

```text
Use $antom-channel-integration in my adapter directory. Implement pay, inquiryPayment and refund using the confirmed field mappings and security rules below. Add independent tests through the real SPI entry points and list any unresolved protocol details.
```

### Important Notes

1. The platform owns channel identity, merchant context, domain/path configuration, HTTP transport, key lookup, and iPay forwarding. Adapters implement institution protocol adaptation through the provided platform services.
2. Do not include real keys, production credentials or card data in public issues or commits. Distribute the bundled SDK JAR and standalone consumer POM with the Skill and CLI; exclude local build output from Skill source packages.
3. Passing local tests does not replace platform assembly validation, institution integration testing, or release approval.

## Repository Layout

```text
gemini-extension.json                     # Gemini CLI Skills-only extension manifest
plugin.json                              # Agent Plugins 1.0 manifest
mcp.json                                 # hosted Antom MCP connection
.cursor-plugin/marketplace.json
.claude-plugin/marketplace.json
.codex-plugin/marketplace.json
.agents/plugins/marketplace.json
skills/antom-integration/SKILL.md          # shared source of truth
skills/antom-reconciliation-expert/        # reconciliation expert skill + scripts
skills/antom-channel-integration/           # ACI adapter integration skill + references
  scripts/aci-cli/                        # Java CLI, templates, and regression tests
providers/
  cursor/plugin/        # Cursor adapter
  claude/plugin/        # Claude Code adapter
  codex/plugins/        # Codex adapter
LICENSE
```

Each provider package is self-contained and ships its own copy of the skill so editors can load it independently. The single source of truth is `skills/`; provider copies are mirrored from it via the sync script below.

## Sync Skills Across Providers

To prepare provider packages from the shared Skill sources, run:

```bash
npm run sync-skills
# or
node scripts/sync-skills.mjs
```

The script copies every file under `skills/<name>/` into:

- `providers/cursor/plugin/skills/<name>/`
- `providers/claude/plugin/skills/<name>/`
- `providers/codex/plugins/<name>/skills/<name>/`

Use this command when preparing provider packages. Maintain channel integration changes only in `skills/antom-channel-integration/` and this README.

On GitHub, the [`sync-skills` workflow](.github/workflows/sync-skills.yml) watches `skills/**` and `scripts/sync-skills.mjs`. When `main` receives changes there, it runs `npm run sync-skills` and opens a `chore/sync-skills` PR with the mirrored provider copies — so you only need to edit the source-of-truth and merge the auto-generated PR.

## Community & Support

- Follow the [installation and Sandbox tutorials](https://github.com/ant-intl/antom-ai-tools/discussions/categories/tutorials-best-practices).
- Ask integration questions in [Q&A](https://github.com/ant-intl/antom-ai-tools/discussions/categories/q-a).
- Suggest new Skills or use cases in [Ideas](https://github.com/ant-intl/antom-ai-tools/discussions/categories/ideas).
- Share what you built in [Show and tell](https://github.com/ant-intl/antom-ai-tools/discussions/categories/show-and-tell).
- Browse all [Antom AI Tools Discussions](https://github.com/ant-intl/antom-ai-tools/discussions).

If Antom AI Tools helps you, consider starring the repository to follow future updates.
 

## Contact

Antom Technical Service — `TechnicalService@antom.com`

## License

MIT — see [`LICENSE`](LICENSE).

The `antom-channel-integration` Skill and its ACI CLI retain their [Apache-2.0 license](skills/antom-channel-integration/scripts/aci-cli/THIRD-PARTY-NOTICES.md#component-license). CLI dependencies retain their own [third-party notices](skills/antom-channel-integration/scripts/aci-cli/THIRD-PARTY-NOTICES.md). AIS SDK 1.5.2 is included with the Skill and CLI distributions. Institution protocols and other platform libraries retain their own rights.
