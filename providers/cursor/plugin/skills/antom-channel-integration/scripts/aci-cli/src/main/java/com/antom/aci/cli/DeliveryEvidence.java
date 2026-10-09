/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Matches declared protocol cases to their executed Surefire identities, without claiming certification. */
final class DeliveryEvidence {
    private DeliveryEvidence() {
    }

    /** Reads immutable build evidence after a successful clean Maven verification. */
    static Map<String, Object> inspect(Path root, String packageName, List<Capability> capabilities,
                                       Map<String, Set<String>> executed) throws Exception {
        Map<String, Object> evidence = new LinkedHashMap<>();
        for (Capability capability : capabilities) {
            String method = capability.getMethod();
            Path directory = root.resolve("src/test/resources/scenarios").resolve(method);
            List<Path> fixtures = scenarioFiles(directory);
            String testClass = packageName + "." + capability.getTitle() + "DeliveryTest";
            Set<String> testNames = executed.getOrDefault(testClass, Collections.emptySet());
            List<Map<String, Object>> cases = new ArrayList<>();
            Set<String> ids = new LinkedHashSet<>();
            Set<String> categories = new LinkedHashSet<>();
            boolean returningCase = false;
            for (Path file : fixtures) {
                JsonNode scenario = JsonFiles.MAPPER.readTree(file.toFile());
                String id = requiredText(scenario, "id", file);
                if (!id.matches("[a-z][a-z0-9-]{0,63}") || !file.getFileName().toString().equals(id + ".json")
                        || !ids.add(id)) {
                    throw new GenerationException(6, "Scenario ID must be unique and match its file name: " + file);
                }
                if (!scenario.path("confirmed").isBoolean() || !scenario.path("confirmed").booleanValue()) {
                    throw new GenerationException(6, "Unconfirmed protocol scenario is not delivery evidence: " + file);
                }
                String category = requiredText(scenario, "category", file);
                boolean normalCategory = "success".equals(category) || "processing".equals(category)
                        || "protocol".equals(category);
                if (normalCategory) {
                    if (!scenario.path("expectedResult").isObject() || scenario.has("expectedException")) {
                        throw new GenerationException(6, "A returning protocol category needs expectedResult, not expectedException: " + file);
                    }
                    returningCase = true;
                }
                String testName = "selectedSpiMatchesConfirmedProtocol case=" + id;
                if (!testNames.contains(testName)) {
                    throw new GenerationException(6, "Declared scenario did not execute: " + testClass + " case=" + id
                            + "; retain the generated Surefire display-name reporter");
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", id);
                item.put("category", category);
                item.put("status", "passed");
                item.put("fixtureSha256", SdkBundle.hash(file, "SHA-256"));
                item.put("rawBodyFileHashes", rawBodyFileHashes(scenario, file.getParent()));
                item.put("testClass", testClass);
                item.put("testName", testName);
                cases.add(item);
                categories.add(category);
            }
            if (!returningCase) {
                throw new GenerationException(6, "At least one confirmed returning success/processing/protocol case is required for "
                        + method + "; exception-only tests cannot establish SPI mappings");
            }
            Map<String, Object> coverage = new LinkedHashMap<>();
            coverage.put("cases", cases);
            coverage.put("categoriesCovered", categories);
            coverage.put("securityBoundary", "Typed arguments and configured outcomes; algorithm proof requires independent vectors or platform-host conformance");
            coverage.put("reviewRequired", "Confirm success, rejection, processing and applicable security/HTTP/input failure cases against the institution protocol; document non-applicable cases");
            evidence.put(method, coverage);
        }
        return evidence;
    }

    private static Map<String, String> rawBodyFileHashes(JsonNode scenario, Path directory) throws Exception {
        Map<String, String> hashes = new LinkedHashMap<>();
        addBodyFileHash(scenario.path("expectedRequest"), directory, hashes);
        addBodyFileHash(scenario.path("http").path("response"), directory, hashes);
        return hashes;
    }

    private static void addBodyFileHash(JsonNode message, Path directory, Map<String, String> hashes) throws Exception {
        if (!message.has("bodyFile")) {
            return;
        }
        String name = requiredText(message, "bodyFile", directory);
        if (!name.matches("[a-zA-Z0-9][a-zA-Z0-9._-]*\\.txt")
                || !Files.isRegularFile(directory.resolve(name), LinkOption.NOFOLLOW_LINKS)) {
            throw new GenerationException(6, "Raw body fixture must be a regular sibling .txt file: " + name);
        }
        hashes.put(name, SdkBundle.hash(directory.resolve(name), "SHA-256"));
    }

    private static List<Path> scenarioFiles(Path directory) throws Exception {
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new GenerationException(6, "Missing real-SPI scenario directory: " + directory);
        }
        List<Path> fixtures;
        try (Stream<Path> paths = Files.list(directory)) {
            fixtures = paths.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted().collect(Collectors.toList());
        }
        if (fixtures.isEmpty()) {
            throw new GenerationException(6, "No real-SPI scenarios: " + directory);
        }
        for (Path fixture : fixtures) {
            if (!Files.isRegularFile(fixture, LinkOption.NOFOLLOW_LINKS)) {
                throw new GenerationException(6, "Scenario must be a regular file, not a symbolic link: " + fixture);
            }
        }
        return fixtures;
    }

    private static String requiredText(JsonNode object, String field, Path file) {
        if (object == null || !object.isObject() || !object.path(field).isTextual()
                || object.path(field).textValue().trim().isEmpty()) {
            throw new GenerationException(6, "Missing scenario " + field + ": " + file);
        }
        return object.path(field).textValue();
    }
}
