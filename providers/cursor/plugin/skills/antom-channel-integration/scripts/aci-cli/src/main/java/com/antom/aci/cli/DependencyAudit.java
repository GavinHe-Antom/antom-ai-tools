/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Checks resolved Maven artifacts, not just the files copied into the generated project. */
final class DependencyAudit {
    private static final String SDK = "com.alipay.iacqintegrationhub:common-sdk";
    // dependency:list 3.6.1: group:artifact:type[:classifier]:version:scope:absolute-path.
    private static final Pattern ARTIFACT = Pattern.compile(
            "^([^\\s:]+):([^\\s:]+):([^\\s:]+):(?:([^\\s:]+):)?([^\\s:]+):"
                    + "(compile|provided|runtime|test|system):(.+)$");

    private DependencyAudit() { }

    /** Returns local dependency evidence; dependencies outside the baseline still need platform review. */
    static Map<String, Object> inspect(Path report, Path bundledJar, Path bundledPom, String sdkVersion)
            throws Exception {
        if (!Files.isRegularFile(report) || Files.size(report) > 5 * 1024 * 1024) {
            throw new GenerationException(6, "Missing or oversized resolved dependency report: " + report);
        }
        JsonNode baseline;
        try (InputStream input = DependencyAudit.class.getResourceAsStream("/platform-dependencies.json")) {
            baseline = JsonFiles.MAPPER.readTree(input);
        }
        if (!sdkVersion.equals(baseline.path("sdkVersion").asText())) {
            throw new GenerationException(3, "No dependency baseline for SDK " + sdkVersion);
        }
        List<String> runtime = new ArrayList<>();
        List<String> provided = new ArrayList<>();
        List<String> unconfirmedProvided = new ArrayList<>();
        List<Map<String, Object>> review = new ArrayList<>();
        Map<String, Object> sdk = null;
        for (String line : Files.readAllLines(report, StandardCharsets.UTF_8)) {
            String value = line.trim();
            if (value.isEmpty() || value.equals("The following files have been resolved:") || value.equals("none")) {
                continue;
            }
            // Maven on Java 9+ can append module information; it is not part of the artifact path.
            value = value.replaceFirst(" -- module .*$", "");
            Matcher artifact = ARTIFACT.matcher(value);
            if (!artifact.matches()) {
                throw new GenerationException(6, "Unrecognized resolved dependency entry: " + value);
            }
            String coordinate = artifact.group(1) + ":" + artifact.group(2);
            String version = artifact.group(5);
            String scope = artifact.group(6);
            String summary = coordinate + ":" + artifact.group(3);
            if (artifact.group(4) != null) {
                summary += ":" + artifact.group(4);
            }
            summary += ":" + version + ":" + scope;
            if (SDK.equals(coordinate)) {
                if (sdk != null || !"jar".equals(artifact.group(3)) || artifact.group(4) != null
                        || !sdkVersion.equals(version) || !"provided".equals(scope)) {
                    throw new GenerationException(3, "SDK must resolve exactly once as the locked version with provided scope");
                }
                sdk = checkSdk(Paths.get(artifact.group(7)), bundledJar, bundledPom, sdkVersion);
            }
            if ("test".equals(scope)) {
                continue;
            }
            JsonNode requiredVersion = baseline.path("provided").get(coordinate);
            if (requiredVersion != null && (!requiredVersion.asText().equals(version) || !"provided".equals(scope))) {
                throw new GenerationException(6, "Platform API dependency differs from the template baseline: " + summary);
            }
            if ("system".equals(scope)) {
                throw new GenerationException(6, "System-scoped dependencies are not portable delivery artifacts: " + summary);
            }
            if ("provided".equals(scope)) {
                provided.add(summary);
                if (!SDK.equals(coordinate) && requiredVersion == null) {
                    unconfirmedProvided.add(summary);
                    review.add(reviewEntry(summary, scope,
                            "Provided scope excludes this dependency from the adapter JAR, but the locked platform "
                                    + "baseline does not establish its host availability or compatibility"));
                }
            } else if ("compile".equals(scope) || "runtime".equals(scope)) {
                runtime.add(summary);
                review.add(reviewEntry(summary, scope,
                        "This scope creates an additional runtime dependency; delivery packaging, host availability "
                                + "and compatibility require platform review"));
            }
        }
        if (sdk == null) {
            throw new GenerationException(3, "Maven did not resolve the required common-sdk dependency");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sdk", sdk);
        result.put("providedDependencies", provided);
        result.put("providedDependenciesRequiringPlatformReview", unconfirmedProvided);
        result.put("runtimeDependenciesRequiringPlatformReview", runtime);
        result.put("dependenciesRequiringPlatformReview", review);
        if (review.isEmpty()) {
            result.put("runtimeReviewStatus", "no-additional-runtime-dependencies");
        } else {
            result.put("runtimeReviewStatus", "pending-platform-review");
        }
        result.put("boundary", "Resolved inventory and template API checks only; not vulnerability or full host compatibility certification");
        return result;
    }

    private static Map<String, Object> reviewEntry(String dependency, String scope, String reason) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("dependency", dependency);
        entry.put("scope", scope);
        entry.put("reason", reason);
        return entry;
    }

    private static Map<String, Object> checkSdk(Path resolvedJar, Path bundledJar, Path bundledPom, String version)
            throws Exception {
        Path resolvedPom = resolvedJar.resolveSibling("common-sdk-" + version + ".pom");
        if (!Files.isRegularFile(resolvedJar) || !Files.isRegularFile(resolvedPom)
                || !SdkBundle.hash(bundledJar, "SHA-256").equals(SdkBundle.hash(resolvedJar, "SHA-256"))
                || !SdkBundle.hash(bundledPom, "SHA-256").equals(SdkBundle.hash(resolvedPom, "SHA-256"))) {
            throw new GenerationException(3, "Maven-resolved SDK JAR/POM differs from the locked bundle: " + resolvedJar
                    + "; use a clean Maven repository containing the authorized SDK, do not edit the lock to bypass this check");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resolvedJar", resolvedJar.toString());
        result.put("resolvedPom", resolvedPom.toString());
        result.put("sha256", SdkBundle.hash(resolvedJar, "SHA-256"));
        result.put("pomSha256", SdkBundle.hash(resolvedPom, "SHA-256"));
        return result;
    }
}
