/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Small inert files reproduce cached SDK collisions without changing the user's Maven cache. */
class DependencyAuditTest {
    @TempDir Path temporary;
    private Path bundledJar;
    private Path bundledPom;
    private Path resolvedJar;
    private Path resolvedPom;
    private Path report;

    @BeforeEach
    void fixture() throws Exception {
        bundledJar = write("bundle.jar", "authorized-sdk");
        bundledPom = write("bundle.pom", "authorized-consumer-pom");
        Path cache = Files.createDirectories(temporary.resolve("cache with spaces"));
        resolvedJar = Files.copy(bundledJar, cache.resolve("common-sdk-1.5.2.jar"));
        resolvedPom = Files.copy(bundledPom, cache.resolve("common-sdk-1.5.2.pom"));
        report = temporary.resolve("dependencies.txt");
    }

    @Test
    void recordsActualSdkAndIgnoresTestLibrariesForRuntimeReview() throws Exception {
        report(sdk("provided") + "\norg.junit.jupiter:junit-jupiter:jar:5.10.2:test:/test.jar\n"
                + "org.slf4j:slf4j-api:jar:1.7.32:provided:/slf4j.jar");
        Map<String, Object> result = inspect();
        assertEquals("no-additional-runtime-dependencies", result.get("runtimeReviewStatus"));
        assertEquals(resolvedJar.toString(), ((Map<?, ?>) result.get("sdk")).get("resolvedJar"));
    }

    @Test
    void rejectsSameCoordinateWithDifferentJar() throws Exception {
        Files.write(resolvedJar, "stale-sdk".getBytes(StandardCharsets.UTF_8));
        report(sdk("provided"));
        assertEquals(3, assertThrows(GenerationException.class, this::inspect).exitCode);
    }

    @Test
    void rejectsDifferentResolvedPomEvenWithMatchingJar() throws Exception {
        Files.write(resolvedPom, "different-transitive-dependencies".getBytes(StandardCharsets.UTF_8));
        report(sdk("provided"));
        assertEquals(3, assertThrows(GenerationException.class, this::inspect).exitCode);
    }

    @Test
    void rejectsMissingDuplicateOrWrongScopeSdk() throws Exception {
        for (String entries : new String[]{"none", sdk("compile"), sdk("test"), sdk("provided") + "\n" + sdk("provided")}) {
            report(entries);
            assertEquals(3, assertThrows(GenerationException.class, this::inspect).exitCode);
        }
    }

    @Test
    void rejectsChangedHostApiAndSystemDependencies() throws Exception {
        for (String entry : new String[]{"org.slf4j:slf4j-api:jar:2.0.0:provided:/api.jar",
                "org.springframework:spring-context:jar:5.3.29:compile:/api.jar",
                "com.example:native:jar:1.0:system:/native.jar"}) {
            report(sdk("provided") + "\n" + entry);
            assertEquals(6, assertThrows(GenerationException.class, this::inspect).exitCode);
        }
    }

    @Test
    void listsRuntimeDependenciesIncludingClassifierForManualReview() throws Exception {
        report(sdk("provided") + "\ncom.example:helper:jar:1.0:compile:/helper.jar\n"
                + "com.example:native:jar:linux:1.0:runtime:/native.jar -- module synthetic.native");
        Map<String, Object> result = inspect();
        assertEquals("pending-platform-review", result.get("runtimeReviewStatus"));
        assertEquals(2, ((List<?>) result.get("runtimeDependenciesRequiringPlatformReview")).size());
        assertTrue(((List<?>) result.get("runtimeDependenciesRequiringPlatformReview"))
                .contains("com.example:native:jar:linux:1.0:runtime"));
    }

    @Test
    void failsClosedOnMissingOrMalformedResolutionEvidence() throws Exception {
        assertEquals(6, assertThrows(GenerationException.class, this::inspect).exitCode);
        report("unexpected dependency format");
        assertEquals(6, assertThrows(GenerationException.class, this::inspect).exitCode);
    }

    private Map<String, Object> inspect() throws Exception {
        return DependencyAudit.inspect(report, bundledJar, bundledPom, "1.5.2");
    }

    private String sdk(String scope) {
        return "com.alipay.iacqintegrationhub:common-sdk:jar:1.5.2:" + scope + ":" + resolvedJar;
    }

    private void report(String entries) throws Exception {
        Files.write(report, ("\nThe following files have been resolved:\n" + entries + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private Path write(String name, String value) throws Exception {
        return Files.write(temporary.resolve(name), value.getBytes(StandardCharsets.UTF_8));
    }
}
