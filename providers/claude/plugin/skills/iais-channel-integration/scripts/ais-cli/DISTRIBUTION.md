# AIS CLI Installation and Distribution Boundaries

Supported build environment: macOS/Linux, Java 8 and Maven 3.6.3 or later. Java 8 is the compatibility test baseline. Windows is not currently a supported packaging environment.

Antom AI Tools bundles CLI source and templates in this Skill's `scripts/ais-cli/`, not a prebuilt executable. From that directory, run `mvn clean verify`, then `sh bin/ais --help`. See the [source guide](README.md) and [Skill quickstart](../../QUICKSTART.md).

If using a separately provided binary distribution:

1. Verify its published checksum. Checksums detect corruption; they are not a publisher signature.
2. Extract the distribution and run `sh bin/ais --help` using its supplied launcher.
3. Obtain SDK **1.5.2** and its standalone consumer POM from the platform maintainer through the authorized delivery channel. Put them in `sdk/common-sdk-1.5.2.jar` and `sdk/common-sdk-1.5.2.pom`, relative to the CLI installation. They are not part of the public Skill or CLI source; `init` will fail explicitly until they are supplied.
4. Run `sh bin/ais init`. Use the arrow keys and Enter for choices. For automation, use `init --config specification.json --output ./adapter --json`.
5. Complete the anonymous mapping extensions inside each selected `spi/Channel*Service` method, security/transport hooks in `customize`, and independent test fixtures. Security selection asks only whether signatures and encryption are needed; enabled platform-call examples are unfinished until the institution protocol is implemented. Then run `sh /installation/bin/ais package --project ./adapter --json`.

Packaging executes the project's Maven build. Only use it on trusted local source. SDK integrity, successful tests and a normal JAR do not imply institution protocol certification, dependency compatibility approval or production approval. Additional runtime dependencies are listed for platform review in `target/ais-delivery/dependency-audit.json`.

SDK 1.5.2 must include `ChannelRequestContext`, `BaseChannelRequest.runtimeEnv` and `PaymentNotifyRequest.extendInfo`. The same version label can occur on different builds: verify the platform delivery checksums. The CLI rejects a missing context class, and generated compilation tests cover the new fields. Do not edit the generated lock file to conceal a different SDK resolved from a local Maven cache. Generated security calls read the platform merchant identity; tests supply it separately in `context.json` and clear the thread context in `finally`.

The source build's executable lives in `target/ais-cli.jar`; use the launcher so that SDK discovery is relative to this installation, not to your working directory. For source and full guidance, consult this Skill's references and the Antom AI Tools repository.

The Skill includes its Apache-2.0 component license, CLI source and third-party notices. A separate binary publisher must also preserve the built dependency licenses and provide its resolved component inventory. The source Skill archive itself does not claim to contain a binary or CycloneDX inventory. SDK files, production credentials, local Maven settings and real institution data must not be published.
