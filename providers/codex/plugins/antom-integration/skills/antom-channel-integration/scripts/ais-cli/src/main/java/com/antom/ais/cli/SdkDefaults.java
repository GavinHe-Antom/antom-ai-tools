/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Resolves separately authorized SDK files placed beside the CLI, independently of the working directory. */
final class SdkDefaults {
    private SdkDefaults() {
    }

    /** Keep explicit paired paths; otherwise select the CLI's fixed local SDK paths. */
    static void apply(AdapterSpec spec) {
        if (spec.sdkJar != null || spec.sdkPom != null) {
            if (spec.sdkJar == null || spec.sdkPom == null) {
                throw new GenerationException(3, "Specify both sdkJar and sdkPom, or omit both to use the local default SDK paths");
            }
            return;
        }
        Path sdk = home().resolve("sdk");
        Path jar = sdk.resolve("common-sdk-1.5.2.jar");
        Path pom = sdk.resolve("common-sdk-1.5.2.pom");
        if (!Files.isRegularFile(jar) || !Files.isRegularFile(pom)) {
            throw new GenerationException(3, "Platform SDK 1.5.2 is missing from the local default paths: "
                    + jar + " and " + pom
                    + ". Obtain the authorized SDK JAR and standalone consumer POM through the platform channel "
                    + "and place them at those paths; the SDK is supplied separately from the CLI.");
        }
        spec.sdkJar = jar.toString();
        spec.sdkPom = pom.toString();
    }

    /** The launcher sets its installation directory; a standalone JAR uses its own directory. */
    private static Path home() {
        String configured = System.getProperty("ais.cli.home");
        if (configured != null && !configured.trim().isEmpty()) {
            return Paths.get(configured).toAbsolutePath().normalize();
        }
        try {
            Path location = Paths.get(SdkDefaults.class.getProtectionDomain().getCodeSource()
                    .getLocation().toURI()).toAbsolutePath();
            if (Files.isRegularFile(location)) {
                return location.getParent();
            }
            return location;
        } catch (URISyntaxException failure) {
            throw new GenerationException(3, "Cannot locate CLI distribution: " + failure.getMessage());
        }
    }
}
