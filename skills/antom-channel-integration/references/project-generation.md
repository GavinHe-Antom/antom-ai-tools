# Project Creation and CLI Boundaries

## Available workflow

This Skill's `scripts/ais-cli/` contains Java CLI source, versioned templates and tests. Resolve that directory relative to this Skill's `SKILL.md`, not to the adapter project. Build there with `mvn clean verify`, then run `sh bin/ais --help`.
The baseline is Java 8 and Maven 3.6.3+. macOS/Linux are supported; Windows is unverified. Installing the Skill includes CLI source, but does not build it, install Java/Maven, or supply the SDK. See the [quickstart](../QUICKSTART.md).
Examples using `ais` assume the CLI bin directory is on PATH. Otherwise use its actual launcher or `java -jar <cli-path>/ais-cli.jar`; do not assume an identically named command is installed.
Without the CLI, use a platform-supplied project, check [scaffold differences](baseline-scaffold.md), and test/package with Maven. Do not invent installer packages or download URLs.

## Confirm the generation inputs

Use this intake for a request to **create a new adapter**, not for knowledge questions or an existing project's implementation. Read the user's request and supplied specification first, retain the known values, and ask only about missing or conflicting ones. Use the user's language and offer understandable choices rather than requiring knowledge of Java interface names.

Do not create an adapter directory, write its generation configuration, copy a demo, or run `init` (even `--dry-run`) while these choices are unresolved. Read-only inspection of the CLI, SDK and existing workspace is fine. Ask, wait for the reply, and then continue; an unanswered or preselected option is not a confirmed choice.

### 1. Obtain the institution code and payment scenario

Ask for a missing company/institution code through a separate free-text-only input labeled **Company/institution code** in the user's language. Display no example institution names/codes, suggested answers, placeholder, prefilled value or naming hints. If the question tool supports an `options` field, omit it for this question. Do not combine it with payment type or namespace questions. In a text-only conversation, ask just "Company/institution code?" rather than listing examples.

Collect payment type separately using the choices **card** and **non-card**. A display name is optional when the code is already supplied. Do not ask for an additional alias or acceptance of the code solely to derive project identifiers. If only a display name is supplied and it cannot produce a usable technical code, ask for that code. Preserve an existing platform-assigned channelCode or organization namespace when provided.

Once the payment type is known, ask about only the applicable scenarios:

| Topic | Choices to present | Generation consequence |
| --- | --- | --- |
| Card 3DS | No 3DS / authentication and authorization in one call / separate calls | Use `threeDS=none/one/two`; only `two` selects `authenticateAuthorize`, which is required for that choice |
| Payment transactions | Payment, payment inquiry, cancellation; card also offers capture | Translate confirmed choices to `pay`, `inquiryPayment`, `cancel`, `capture`; a selected payment-family method requires `pay` |
| Refund transactions | Refund; optionally refund inquiry | Translate to `refund`, `inquiryRefund`; selecting refund inquiry requires the abstract `refund` method |
| Non-card notifications | Payment notification / refund notification / no notifications | No capture option; selecting any notification requires `notifyPayment` |
| Card notifications | Payment notification / capture notification / refund notification / no notifications | Translate to `notifyPayment`, `notifyCapture`, `notifyRefund`; selecting any notification requires `notifyPayment` |

Non-card uses `threeDS=none`; do not ask its user to choose a 3DS mode or capture. Do not infer 3DS from the institution name, SDK classes or another channel's implementation. Ask about unmentioned optional transactions and notifications rather than enabling all or interpreting silence as "not needed". If the user already explicitly limits the scope (for example, "pay only, no notifications"), do not ask again about each excluded capability.

Explain interface dependencies before adding a method the user did not request. For example, a refund-notification-only request conflicts with the required `notifyPayment` method: ask whether payment notification can be included instead of quietly adding a fake implementation. `notifyRefund` does not by itself require the `refund` transaction, and `notifyCapture` does not require the `capture` transaction. Do not invent those dependencies.

The interactive wizard always includes `pay`; JSON mode can express refund-only or notification-only scopes. For Agent workflows, translate the confirmed scope to JSON and follow the actual validator rather than copying the wizard's default selections. Vaulting, dispute and unconnected entry-point capabilities are not available in this generator. Do not generate `receivePaymentNotify` or `onlineBankPaymentNotify` implementations (the SDK names are `notifyReceivePayment` and `notifyOnlineBankPayment`). Do not generate `acsUrlCallback` or `onlineBankUrlCallback`, a callback service or their operation constants, even if a local SDK snapshot still declares them. SDK declarations alone do not expand the generator's supported scope.

### 2. Derive and use project identifiers

Preserve explicit user-provided identifiers. Otherwise directly derive channelCode, Maven coordinates and the Java package from the supplied institution code and use them without a second confirmation. The user does not need to approve an identifier proposal. If a Maven groupId is supplied but the Java package is not, use that groupId plus `.adapter` as the package. Respect an existing organization namespace when supplied; an inferred namespace does not establish ownership or platform registration.

Normalize ASCII letters to lowercase. Keep hyphens in the artifact token and convert underscores to hyphens; remove these separators for the channel/Java namespace token. Prefix a Java namespace token with `channel_` if it starts with a digit or is a Java keyword. Do not truncate a code or invent a different institution to conceal a validation error. Ask only if the code cannot yield valid identifiers, explicit inputs conflict, or a real collision requires another code/destination.

For example, institution code `acme` directly produces:

| Generated value | Derived value | Constraint |
| --- | --- | --- |
| channelCode | `iaischannelacme` | 2–64 characters, lowercase first letter, then lowercase letters/digits/underscore/hyphen; platform registration still required |
| Maven groupId | `com.acme.channel` | Valid lowercase Java-name segments; at least two segments, under 160 characters |
| Maven artifactId | `channel-acme-adapter` | 2–64 characters, lowercase first letter, then lowercase letters/digits/hyphen |
| Maven version | `1.0.0-SNAPSHOT` | CLI default unless explicitly supplied; 1–64 characters, starts with a digit |
| Java package | `com.acme.channel.adapter` | Valid lowercase Java-name segments; no Java keywords or hyphens |
| Output location | A separate `channel-acme-adapter/` under the user's selected workspace | Show the resolved path; it must be nonexistent or empty, not the Skill directory |

These are generation conventions, not SDK-mandated names. Report the chosen values in the generation summary without pausing for approval. Use a channel-specific code when the user supplies one; do not claim the derived channelCode is already registered. Institution codes and display names stay in the intake summary. The CLI JSON has no `companyName`, `companyCode`, `institutionCode`, `company` or `channelAlias` field; serialize only its supported keys.

Different institution codes can normalize to the same namespace. Before generation, check available project manifests for actual channelCode, package or Maven-coordinate collisions; do not hide a conflict by changing identifiers silently. The CLI checks direct sibling adapter manifests when previewing/generating. This workspace check is not a platform-wide uniqueness guarantee; registration still requires platform review. Ask about a different code or explicit identifiers only when a real conflict exists.

### 3. Ask only two security questions

Ask exactly two global yes/no questions: **Does the adapter need signature handling?** and **Does the adapter need encryption handling?** Retain answers already supplied. Serialize them as `securityFeatures: {"signature": true/false, "encryption": true/false}`; do not ask per-method security questions during scaffold creation.

Signature handling enables request-signing and response/notification-verification examples for the selected methods. Encryption handling enables request-encryption and response/notification-decryption examples. Enabled features demonstrate typed platform calls; their protocol-input, algorithm/parameter and result-placement hooks remain unfinished and fail until implemented. Disabled features generate no computation for their operations. These global choices shape the skeleton, not certify institution security behavior.

Do not request algorithms, operation order, `platform` versus `adapter`, key algorithms, cipher parameters or rule references for scaffold creation. Decide these during implementation from the institution protocol; custom computation may query keys through the platform and use a mature library. Do not request real keys, merchant credentials or production payloads. If either yes/no answer is missing, ask only for that answer and leave creation pending.

Advanced JSON configuration can separately specify the existing detailed `security` contract per method/direction instead of `securityFeatures`. Use it only when those rules are already supplied; do not turn it into the default intake or combine the two forms. The advanced form retains explicit operation order, implementation, algorithm/keyAlgorithm, static parameters and non-secret rule references.

### 4. Summarize, preview and generate

Summarize the supplied institution code, payment type/3DS mode, transaction and notification methods, derived or explicit identifiers, output path and the two security booleans for information only. Explicitly state whether refund and refund inquiry are included or excluded; when neither is selected, explain that no `ChannelRefundService` will be generated even if `notifyRefund` is selected. Compare the configuration and dry-run methods with the user's confirmed scope; a user-selected refund missing from `spi` is an input omission to correct before generation. Do not auto-enable refund transactions because a notification is selected. Do not ask whether the user accepts the names or wants you to proceed again. Ask only for unresolved scope/boolean answers, an unavailable output location or an actual conflict.

Once the required inputs and prerequisites are available, write the actual configuration, run the documented JSON dry run, inspect the methods and file list, and generate into the selected destination. SDK paths come from the CLI's local default; do not ask users to type JAR/POM locations unless diagnosing missing/mismatched artifacts. Report unfinished mapping/security hooks separately from successful skeleton generation.

### Example questions

If only "Create an adapter for me" is known, present a free-text input with this label (translated to the user's language) and no options, example values, placeholder or default:

> Company/institution code

Ask for payment type separately with card/non-card choices; these may be separate questions in the same questionnaire. After a card answer, ask about no/one-call/two-call 3DS and the remaining transaction/notification choices. After a non-card answer, omit those card-only questions. Ask only the two signature/encryption yes/no security questions; derived identifiers do not need confirmation. Do not present a generated demo as the result of an unanswered questionnaire.

## When the user provides a CLI

1. Confirm the tool's source, inspect --help, version and command help; the name ais alone does not establish identity.
2. Verify template/SDK compatibility and follow the intake above: collect a missing institution code, card/non-card, applicable 3DS, SPI and notification scope, then wait for answers to those missing requirements. Derive project identifiers automatically.
3. Ask the two global signature/encryption yes/no questions and use `securityFeatures`. Defer detailed protocol rules and the computation mode to implementation; enabled demonstration hooks must not be presented as completed security.
4. Prefer supported noninteractive input and structured output. Inspect a preview file list when available; do not overwrite existing projects.
5. When required inputs are resolved, generate with the derived/explicit identifiers in the designated directory without another naming approval, then implement real mappings/security rules. A compiling skeleton is not a finished integration.
6. Run real tests before packaging. Report missing SDK, required implementations or security rules instead of skipping tests and declaring delivery readiness.

## Commands

Template 0.1.0 supports SDK 1.5.2. Neither the public repository nor distribution contains the SDK. Obtain the authorized JAR and standalone consumer POM from the platform and place them in the CLI's local sdk/ directory; they are read automatically without prompting for paths.
Humans can run `ais init` in a real terminal, enter the company/institution code and use arrow keys to select capabilities, Enter to confirm and Ctrl+C to cancel. Project identifiers are derived without a naming-approval question; JSON preserves explicitly supplied coordinates and namespaces. Security asks only signature/encryption yes/no. Piped numeric selections are unsupported.
Agents should use `ais init --config adapter-spec.json --output ./my-adapter --dry-run --json`, inspect the plan, then remove --dry-run. Do not simulate numbered line-by-line input. The source distribution includes examples/non-card.json; do not assume a binary ZIP includes it.

JSON omits sdkJar/sdkPom by default. If overrides are necessary, supply both; relative paths resolve from the configuration-file directory. Output paths resolve from the working directory.
When the SDK is absent, explain the required version/location and ask the user to obtain it through the authorized platform channel. Do not download untrusted JARs, invent dependencies or add the SDK to the public CLI package.
The concise `securityFeatures` form applies the two booleans to every selected method's applicable directions. Enabled examples delegate to platform security; implement the actual order, algorithm, signing input, signature placement and runtime IV/AAD/PGP in typed hooks before delivery. Do not fix production randomness or enter real keys. Advanced `security` JSON retains explicit per-direction rules when required.

After generation, implement transaction mapping in the anonymous `ChannelApiExtension` inside each SPI method and notification mapping in its anonymous `ChannelNotificationExtension`; no `customize/api` helper classes are generated. Implement security and transport in `customize/security` and `customize/transport`. `GeneratedStructureTest` validates structure only; `*SecurityContractTest` uses synthetic hooks to verify template security contracts. Initial `*DeliveryTest` is expected to fail because real business logic and enabled security hooks are unfinished.
For a trusted project, `ais package --project ./my-adapter --json` runs all tests, dependency-tree and plain-JAR checks. It requires actual passing, non-skipped structure, selected-method security and selected-method delivery reports. Additional runtime or provided dependencies require platform review because SOFABoot shares the classpath. Reports appear in target/ais-delivery; the command neither uploads nor configures the platform. Maven plugins execute code, so package is not a malicious-code sandbox.

The SDK uses a file repository with provided scope; no install-sdk command is required. Correct version mismatches, nonempty output directories and missing policy rules instead of bypassing tests. Local CLI success is not proof of protocol correctness, vulnerability clearance or rollout approval.

The authorized SDK 1.5.2 must contain ChannelRequestContext, BaseChannelRequest.runtimeEnv and PaymentNotifyRequest.extendInfo. Version equality alone is insufficient: compare platform-supplied JAR/POM checksums. The CLI rejects a missing context class. Do not edit generation-lock.json to conceal Maven cache mismatches.

Generated security code reads merchantId from ChannelRequestContext. Protocol hooks supply input, signatures, parameters and result placement, not another merchant identity. Each independent scenario contains a context object to simulate platform identity; follow [testing](TESTING.md) and always clear context in finally.
