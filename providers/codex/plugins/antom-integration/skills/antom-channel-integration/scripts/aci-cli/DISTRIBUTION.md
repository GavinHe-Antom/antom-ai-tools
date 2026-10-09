# ACI CLI Installation and Distribution Boundaries

Supported build environment: macOS/Linux, Java 8 and Maven 3.6.3 or later. Java 8 is the compatibility test baseline. Windows is not currently a supported packaging environment.

Antom AI Tools bundles CLI source, templates and AIS SDK 1.5.2 with its standalone consumer POM in this Skill's `scripts/aci-cli/`. From that directory, run `mvn clean verify`, then `sh bin/aci --help`. The SDK is included with the Skill and CLI, so `init` needs no separate SDK download or setup. See the [CLI build and usage guide](README.md#build-and-usage) and [Skill workflows](../../SKILL.md#choose-the-task-before-acting).

`mvn package` or `mvn clean verify` produces `target/aci-cli-0.1.0.zip`. It contains `aci-cli.jar`, `sdk/common-sdk-1.5.2.jar`, `sdk/common-sdk-1.5.2.pom`, `README.md`, `DISTRIBUTION.md` and `THIRD-PARTY-NOTICES.md` directly at the archive root, with no enclosing parent directory or source launcher.

To use the standalone binary distribution:

1. Verify its published checksum. Checksums detect corruption; they are not a publisher signature.
2. Extract the ZIP into a directory, keep its bundled `sdk/` beside `aci-cli.jar`, and run `java -jar aci-cli.jar --help` from that directory.
3. Run `java -jar aci-cli.jar init`. Use the arrow keys and Enter for choices. For automation, use `java -jar aci-cli.jar init --config specification.json --output ./adapter --json`.
4. Complete the anonymous mapping extensions inside each selected `spi/Channel*Service` method, security/transport hooks in `customize`, and independent test fixtures. Security selection asks only whether signatures and encryption are needed; enabled platform-call examples are unfinished until the institution protocol is implemented. Then run `java -jar /installation/aci-cli.jar package --project ./adapter --json`.

Packaging executes the project's Maven build. Only use it on trusted local source. SDK integrity, successful tests and a normal JAR do not imply institution protocol certification, dependency compatibility approval or production approval. Additional runtime dependencies are listed for platform review in `target/aci-delivery/dependency-audit.json`.

AIS SDK 1.5.2 must include `ChannelRequestContext`, `BaseChannelRequest.runtimeEnv` and `PaymentNotifyRequest.extendInfo`. The same version label can occur on different builds: verify the platform delivery checksums. The CLI rejects a missing context class, and generated compilation tests cover the new fields. Do not edit the generated lock file to conceal a different SDK resolved from a local Maven cache. Generated security calls read the platform merchant identity; tests supply it separately in `context.json` and clear the thread context in `finally`.

The source build's executable lives in `target/aci-cli.jar`; use the source launcher so that SDK discovery is relative to the source installation. The extracted ZIP's executable discovers the adjacent `sdk/` independently of your working directory. For source and full guidance, consult this Skill's references and the Antom AI Tools repository.

The Skill includes its [Apache-2.0 component license](THIRD-PARTY-NOTICES.md#component-license), CLI source, SDK and [third-party notices](THIRD-PARTY-NOTICES.md). Binary distributions also preserve the built dependency licenses and use the actual resolved component inventory. Skill source archives include the SDK JAR and standalone consumer POM, but exclude Maven build output; a generated CycloneDX inventory is not included. Production credentials, local Maven settings, real institution data and other platform-internal libraries are excluded from distributions.
