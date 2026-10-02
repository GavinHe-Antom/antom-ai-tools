# AIS Adapter CLI

Generate an independent channel adapter from selected capabilities. Supports macOS/Linux, Java 8 and Maven 3.6.3+; implemented with Picocli + FreeMarker, without Python. Java 8 is the compatibility-test baseline. Windows packaging is not supported. Maintainer end-to-end regression also requires Node.js 20+.

## Build and usage

Learning or asking the Skill questions requires no CLI, SDK or new project; see [Choose your task](../../QUICKSTART.md#choose-your-task). Work directly in an existing adapter rather than running `init` over it.

“Source root” below means this Skill's directory, containing `SKILL.md`, `QUICKSTART.md`, `references/` and `scripts/`. In Antom AI Tools this is `skills/iais-channel-integration/`; provider installations keep the same relative layout. “Adapter root” means the separately generated project. First build the CLI and read help from the source root. **This step requires no SDK and generates no adapter:**

```sh
mvn -f scripts/ais-cli/pom.xml clean verify
sh scripts/ais-cli/bin/ais --help
```

Help should list `init` and `package`. Other build dependencies require reachable Maven repositories or an existing cache. The CLI does not install itself or change PATH/Maven settings. You may add the absolute `scripts/ais-cli/bin` path to PATH yourself and invoke `ais`.

### Prepare the authorized SDK before generation

`init` and `package` require the SDK. The public CLI excludes it. Obtain the separately authorized SDK 1.5.2 and place its JAR and standalone consumer POM in the CLI's `sdk/` directory. Commands discover them automatically: **no interactive path entry or install-sdk command is needed**. The default location does not change with the working directory.

```text
scripts/ais-cli/
├── bin/ais
├── target/ais-cli.jar
└── sdk/
    ├── common-sdk-1.5.2.jar
    └── common-sdk-1.5.2.pom
```

The platform supplies these files through an authorized channel; populate `sdk/` locally. Use the platform build's common-sdk-consumer.pom under the filename above, not the source POM that inherits a platform parent. Git ignores this directory; the SDK must not enter public Git, public ZIPs or the CLI JAR. Missing or mismatched artifacts cause an explicit error, not a silent download or version substitution.

SDK 1.5.2 must include ChannelRequestContext, BaseChannelRequest.runtimeEnv and PaymentNotifyRequest.extendInfo. Earlier JARs with the same version may lack these APIs. The CLI checks the context class, generated structure tests compile the added fields, and packaging checks actual artifact digests. Follow the [SDK baseline](../../references/guide/12-evidence-and-maintenance.md) and platform delivery checksums; do not edit lock files to hide cache conflicts.

For direct executable-JAR use, put `ais-cli.jar` and the separately obtained `sdk/` side by side in your local installation, then run `java -jar /absolute/path/to/ais-cli.jar init`. From the source root, `sh scripts/ais-cli/bin/ais` sets the CLI root automatically. Preparing a local SDK does not authorize its public redistribution.

## Interactive selection

After building and preparing the SDK, run from the source root:

```sh
sh scripts/ais-cli/bin/ais init
```

Choose a new adapter output directory, such as `../my-adapter` next to the source root. Relative paths resolve from the current working directory. The target must not exist or must be empty. Generate the project before installing a project-local Skill; placing `.agents/skills/` there first makes the directory nonempty and causes rejection. Do not rerun `init` on existing adapters.

Coordinates, Java package, channelCode and output directory use text input. Card/non-card, 3DS calls, SPI scope and exactly two global security yes/no questions use arrow-key menus: **↑ / ↓ moves the highlight, Enter confirms, Ctrl+C cancels**. Security asks only whether signature handling and encryption handling are needed; it does not ask for algorithms, order, computation modes or rule references. Option numbers are neither required nor accepted.

```text
Payment type
 > card
   non-card
Up/Down: select | Enter: confirm | Ctrl+C: cancel (1/2)
```

The interactive wizard starts with pay. Card two-call 3DS adds authenticateAuthorize; non-card flows neither ask about nor generate capture. JSON requires pay only for payment-family methods, so refund-only scope is possible. Refund and inquiryRefund are separate selections from notifyRefund; selecting refund inquiry requires refund, while selecting refund notification requires notifyPayment, not refund. The generation summary must explicitly identify selected and excluded refund transactions. Only families with selected methods get a service class.

Interactive mode requires a real ANSI-capable terminal with stdin and stderr attached. Use `--config` for pipes, CI or IDE consoles without terminal support; there is no numeric-input fallback. Menus write only to stderr, so `--json` stdout remains a single result object. Cancellation returns 130, leaves no partial project and restores terminal settings.

Terminal interaction uses [JLine](https://jline.org/docs/terminal/), pinned to Java-8-compatible 3.26.3. It affects only the CLI, not the adapter or platform runtime.

Answer the two global questions once: signature handling yes/no, then encryption handling yes/no. The answers populate `securityFeatures.signature` and `securityFeatures.encryption`. Signature enables request signing and response/notification verification; encryption enables request encryption and response/notification decryption. Disabled features use no computation. Enabled features generate typed platform-call examples whose protocol hooks throw until implemented, not a working institution algorithm or canonical input.

During implementation, confirm the institution's algorithms, operation order, encoding, runtime parameters and result placement. Choose platform computation or custom adapter computation after platform `queryKey` at that stage. Supply IV, AAD and PGP options through typed request hooks rather than fixing production randomness. The CLI never accepts keys. Advanced per-method `security` JSON is available separately when detailed rules are already known; it is not an additional scaffold questionnaire.

Both computation modes obtain merchantId from `ChannelRequestContext.current().getMerchantId()`, not from protocol hooks. Adapters only read platform context; they must not rebuild identity from business content or call bind/clear in production. runtimeEnv may be null and must not independently select sandbox addresses or bypass security.

## JSON input and Agents

JSON and interactive generation are alternatives: do not run both against an already generated directory. See the complete synthetic example [examples/non-card.json](examples/non-card.json). SDK paths can be omitted, but an authorized local SDK must still be prepared.

### Preview configuration

Preview from the source root without writing an adapter. The output path denotes `my-adapter/` next to that root:

```sh
sh scripts/ais-cli/bin/ais init \
  --config scripts/ais-cli/examples/non-card.json \
  --output ../my-adapter --dry-run --json
```

### Generate a new project

Copy the example and set coordinates, channel, selected capabilities and the two security booleans from confirmed requirements. The example selects pay, refund and refund inquiry; this is illustrative, not a default for every channel. Enabled security hooks still need protocol implementation. After previewing and checking the actual configuration, generate from the source root into a nonexistent or empty directory:

```sh
sh scripts/ais-cli/bin/ais init \
  --config /absolute/path/to/confirmed-channel.json \
  --output ../my-adapter --json
```

On success, [implement, test and install the project-local Skill](../../QUICKSTART.md#implement-and-test), then complete [implementation](#where-to-implement) and [testing and packaging](#testing-and-packaging). Generation does not mean business behavior is complete; do not immediately treat the output as a deliverable.

When sdkJar/sdkPom are omitted, JSON mode reads the separately authorized files from the CLI's local `sdk/`. Automation may override both fields together with local paths relative to the configuration file. output is relative to the current working directory. Unknown fields, duplicate keys and trailing content are rejected. External templates, arbitrary hooks and shell input are unsupported. stdout contains only result JSON; prompts and diagnostics go to stderr.

| Field | Constraint |
| --- | --- |
| schemaVersion | 1 |
| groupId / packageName | At least two segments; valid lowercase Java names |
| artifactId / channelCode | Start with a letter; limited characters such as digits and hyphens |
| version / sdkVersion | Project version; templates support only SDK 1.5.2 |
| sdkJar / sdkPom | Omit both to use the CLI's local sdk/; the SDK is separately authorized. Overrides require both local files, not URLs |
| paymentType / threeDS | card/non-card; none/one/two |
| spi | pay, authenticateAuthorize, cancel, capture, inquiryPayment, refund, inquiryRefund, notifyPayment, notifyCapture, notifyRefund |
| securityFeatures | Concise scaffold form: exactly `signature` and `encryption` booleans, applied globally to selected methods |
| security | Advanced alternative to securityFeatures; exactly covers selected methods/directions with both operations, including none where applicable |
| security.*.*[].rule | Non-secret rule identifier/reference; ASCII, without control characters or code fragments |
| platform | sign/verify use algorithm; encrypt/decrypt also require cipherType, and block algorithms require parameters.mode/padding |
| adapter | Uses keyAlgorithm; implement other protocol details in customization hooks |

Do not combine `securityFeatures` and detailed `security`. The advanced form selects `none`, `platform` or `adapter` and retains its operation order, non-secret rule references and algorithm/static-parameter validation. RSA2 accepts only ECB and RSA padding; symmetric algorithms reject RSA padding; DES/DESede reject GCM. The [algorithm catalogue](src/main/resources/catalogue.json) separates signing, encryption and key lookup; do not interchange them. These implementation details are not requested by the wizard, and no algorithm is presumed suitable for every institution.

## Where to implement

| Directory | Responsibility |
| --- | --- |
| spi | Selected-method wiring and anonymous ChannelApiExtension (transactions) / ChannelNotificationExtension (notifications) containing validation, field mapping and result conversion |
| customize/security | Canonical text, platform calls and custom computation |
| customize/transport | HTTP method, headers, query and Content-Type |
| template / extension / model / support | Fixed flow, extension interfaces, messages and integration exceptions |

No `customize/api` directory or per-method Mapping helper classes are generated. For ordinary JSON protocols, edit the anonymous extension in the corresponding SPI method plus security/transport customization. Non-JSON protocols or non-2xx business responses may require explicit template changes and tests. The default template treats non-2xx as call failure rather than guessing business codes. All HTTP calls use the platform; adapters do not configure domains or connection parameters.

The SDK is copied into lib/repository with file repository + provided scope. There is no install-sdk command, system scope or SDK fat JAR. adapter-spec.json records choices; generation-lock.json records versions and JAR/POM SHA-256. Generation does not change platform capability registration or channel routing.

## Testing and packaging

Run the following commands from the **adapter root**. For the `../my-adapter` output above, first run `cd ../my-adapter` from the source root. Do not run adapter tests in the CLI source directory.

### Structure check and initial state

```sh
mvn -Dtest=GeneratedStructureTest test
```

This checks compilation, Spring wiring and the method list, proving only structural correctness. New projects have unfinished mapping and security hooks. Their `*DeliveryTest` failures during full verification are expected, not delivery checks to skip.

### Verify completed business behavior

Complete per-method mapping, security and transport customization. Add independent fixtures, platform mocks, security-parameter assertions and failure scenarios based on the protocol.

Each method's `scenarios/<method>/context.json` independently stores synthetic channelCode, merchantId and runtimeEnv. Generated tests simulate Facade binding and clear context in finally. It is neither part of input.json nor production configuration. The expected merchantId in security.json must match that test identity.

Then run from the adapter root:

```sh
mvn clean verify
```

This executes real-SPI delivery tests and should pass after business implementation is complete. Platform mocks and synthetic signatures verify delegation contracts, not algorithm equivalence or institution certification.

### Final packaging and reports

After full verification, run from the adapter root, replacing the CLI path with the actual absolute source-root path. If you configured PATH yourself, `ais package --project . --json` is equivalent:

```sh
sh /absolute/path/to/iais-channel-integration/scripts/ais-cli/bin/ais package --project . --json
```

Packaging performs: locked SDK validation → Maven clean and actual dependency resolution → resolved SDK JAR/POM hashes and provided-scope validation → Maven verify and dependency tree → unskipped per-method delivery-test and ordinary-JAR checks → SHA-256 and reports. The successful report is in **the adapter project's** `target/ais-delivery/report.json`, not the CLI's `target/`. `package` reruns verification even if an earlier standalone build passed.

`dependency-audit.json` records actual SDK paths/hashes, host provided dependencies and additional runtime dependencies. Scope/version deviations from known host API baselines reject packaging. Additional runtime dependencies are marked `pending-platform-review`; the CLI does not assume compatibility or delete them. A local Maven cache entry with the same coordinates but different contents fails explicitly; neither the cache nor lock file is automatically changed.

Maven is located via PATH or MAVEN_HOME/bin/mvn; Java uses JAVA_HOME. `--offline` is for cached dependencies. Builds are limited to 10 minutes and 50 MiB of logs, retained as .ais-build-*.log in the project. Users with mirrorOf=* must exclude bundled-sdk themselves.

Build only trusted local projects: Maven plugins can execute code, and the CLI is not an untrusted-code sandbox. Artifact checks identify embedded host classes, nested JARs, test directories and selected key-file extensions. They are **not a complete sensitive-data scan, vulnerability scan or security certification**. Local success does not replace platform assembly, institution integration or release approval.

| Exit code | Meaning |
| --- | --- |
| 0 | Generation, preview or local verification succeeded |
| 2 | Configuration or input error |
| 3 | SDK, template or lock-file mismatch |
| 4 | Nonempty target or directory conflict; no overwrite |
| 5 | Maven build/test failure or timeout |
| 6 | Test evidence or artifact check failed |
| 130 | Interactive cancellation with Ctrl+C |

## Maintainer regression

Run from `scripts/ais-cli/` to verify the CLI and templates, not to package a completed adapter:

```sh
mvn clean verify
node scripts/verify-generated.mjs /path/common-sdk-1.5.2.jar /path/common-sdk-consumer.pom
```

The regression script generates and packages through JSON configurations, retaining logs and verification.json in a temporary directory. JLine terminal-stream unit tests cover keys, menus and scrolling. Before release, also run a real-terminal arrow-key generation and Ctrl+C restoration check. No real institutions or production keys are used. Synthetic signatures mock platform delegation; they do not prove algorithm equivalence. Templates derive from the project scaffold and still require source authorization, third-party license and dependency review before publication.

## Skill distribution and licensing

The Antom AI Tools repository distributes this CLI's **source and templates** inside the `iais-channel-integration` Skill. Its existing Skill synchronization and release workflows package the shared source for supported providers. Do not assume a Skill ZIP includes a built executable JAR, SDK, binary SBOM or a separately published CLI release.

From the Skill source root, validate the bundled content:

```sh
node scripts/check-content.mjs
```

Repository maintainers run `npm run sync-skills` from the Antom AI Tools repository root to refresh the provider copies. The sync respects Git ignore rules, including this Skill's local `scripts/ais-cli/sdk/` and Maven `target/` directories. Follow the repository's existing committed-source release procedure for Skill archives; do not force-add private SDK files.

The Skill and CLI retain [Apache-2.0](../../LICENSE). Built CLI dependencies retain their [third-party notices](THIRD-PARTY-NOTICES.md). If distributing a binary separately, preserve its dependency licenses and notices, provide the resolved dependency inventory, exclude the SDK, and perform the applicable release review. Local build output does not imply publication approval or vulnerability clearance. See [installation boundaries](DISTRIBUTION.md).

## Generator implementation: Java and Python

The current generator separates input/validation in the CLI from [FreeMarker](https://freemarker.apache.org/docs/index.html) rendering in `src/main/resources/templates/`. Algorithm and host-dependency catalogues are separate JSON files. SDK contract updates usually require related template, reference and regression-test changes.

Python with [Jinja](https://jinja.palletsprojects.com/en/stable/) or [Cookiecutter](https://cookiecutter.readthedocs.io/en/stable/) can also generate Java projects, but it does not automatically improve generated-code quality. This project retains Java for the following reasons:

| Dimension | Current Java implementation | Python migration impact |
| --- | --- | --- |
| Template maintenance | Independently maintained FreeMarker files | Jinja is viable, but templates and conditions require migration |
| User environment | Adapters already need JDK/Maven; the prebuilt CLI runs directly | JDK/Maven remain required, with Python and its dependencies added |
| Interaction and configuration | Arrow keys, cancellation restoration and strict JSON validation already exist | Equivalent behavior needs implementation and validation |
| Generation and delivery | SDK validation, atomic writes, dependency audit, real tests and JAR checks already exist | A simple file-generation script does not replace these capabilities |

Reconsider migration if the audience changes to generation-only users without Java tooling, or measurable distribution costs justify it. Reuse one capability catalogue, protocol configuration and regression matrix rather than maintaining diverging generators. SDK synchronization and English translation do not change the CLI implementation language.
