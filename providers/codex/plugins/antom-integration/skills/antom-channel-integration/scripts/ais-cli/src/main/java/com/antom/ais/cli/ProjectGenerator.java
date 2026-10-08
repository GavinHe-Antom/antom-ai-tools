/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Renders trusted bundled templates in a staging directory, then publishes a complete project. */
final class ProjectGenerator {
    static final String TEMPLATE_VERSION = "0.1.0";
    private final Configuration templates;

    ProjectGenerator() {
        templates = new Configuration(Configuration.VERSION_2_3_34);
        templates.setClassForTemplateLoading(ProjectGenerator.class, "/templates");
        templates.setDefaultEncoding("UTF-8");
        templates.setInterpolationSyntax(Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX);
        templates.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        templates.setAPIBuiltinEnabled(false);
        templates.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        templates.setLogTemplateExceptions(false);
    }

    Map<String, Object> generate(AdapterSpec spec, Path base, Path output, boolean dryRun) throws Exception {
        List<Capability> capabilities = SpecValidator.validate(spec);
        SdkBundle sdk = SdkBundle.load(spec, base, capabilities);
        Path destination = canonicalDestination(output);
        checkDestination(destination);
        checkSiblingIdentities(spec, destination, null);
        Map<String, String> files = files(spec, capabilities);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "generated");
        if (dryRun) {
            result.put("status", "planned");
        }
        result.put("output", destination.toString());
        result.put("templateVersion", TEMPLATE_VERSION);
        result.put("sdkVersion", spec.sdkVersion);
        result.put("sdkSha256", sdk.sha256);
        result.put("methods", spec.spi);
        List<String> fileList = new ArrayList<>(files.keySet());
        fileList.add("adapter-spec.json");
        fileList.add("generation-lock.json");
        for (String extension : Arrays.asList("jar", "pom")) {
            String artifact = "lib/repository/" + SdkBundle.COORDINATE_PATH + "common-sdk-1.5.2." + extension;
            fileList.add(artifact);
            fileList.add(artifact + ".sha1");
            fileList.add(artifact + ".sha256");
        }
        result.put("files", fileList);
        if (dryRun) {
            return result;
        }
        Files.createDirectories(destination.getParent());
        Path stage = Files.createTempDirectory(destination.getParent(), ".ais-stage-");
        try {
            Map<String, Object> model = model(spec, capabilities);
            for (Map.Entry<String, String> file : files.entrySet()) {
                Map<String, Object> fileModel = new LinkedHashMap<>(model);
                setFileContext(file.getKey(), capabilities, fileModel);
                render(stage.resolve(file.getKey()), file.getValue(), fileModel);
            }
            sdk.copyTo(stage);
            AdapterSpec saved = JsonFiles.MAPPER.convertValue(spec, AdapterSpec.class);
            saved.sdkJar = "lib/repository/" + SdkBundle.COORDINATE_PATH + "common-sdk-1.5.2.jar";
            saved.sdkPom = "lib/repository/" + SdkBundle.COORDINATE_PATH + "common-sdk-1.5.2.pom";
            JsonFiles.write(stage.resolve("adapter-spec.json"), saved);
            Map<String, Object> lock = new LinkedHashMap<>();
            lock.put("templateVersion", TEMPLATE_VERSION);
            lock.put("sdkVersion", spec.sdkVersion);
            lock.put("sdkSha256", sdk.sha256);
            lock.put("sdkPomSha256", SdkBundle.hash(sdk.pom, "SHA-256"));
            JsonFiles.write(stage.resolve("generation-lock.json"), lock);
            checkDestination(destination);
            checkSiblingIdentities(spec, destination, stage);
            if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
                // Delete only an empty destination; a concurrent writer causes DirectoryNotEmptyException.
                Files.delete(destination);
            }
            // Same-filesystem rename; without REPLACE_EXISTING a competing destination is not overwritten.
            Files.move(stage, destination);
            return result;
        } catch (Exception failure) {
            try {
                deleteStage(stage);
            } catch (Exception cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    /** Only real manifests in direct sibling projects establish a local identity collision. */
    private void checkSiblingIdentities(AdapterSpec spec, Path destination, Path stage) throws IOException {
        Path parent = destination.getParent();
        if (!Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        List<Path> siblings;
        try (Stream<Path> entries = Files.list(parent)) {
            siblings = entries.filter(path -> !path.equals(destination) && !path.equals(stage))
                    .filter(path -> Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))
                    .sorted().collect(java.util.stream.Collectors.toList());
        }
        for (Path sibling : siblings) {
            Path manifest = sibling.resolve("adapter-spec.json");
            if (!Files.isRegularFile(manifest, LinkOption.NOFOLLOW_LINKS) || Files.size(manifest) > 1024 * 1024) {
                continue;
            }
            JsonNode existing;
            try {
                existing = JsonFiles.MAPPER.readTree(manifest.toFile());
            } catch (IOException malformed) {
                // An unreadable or malformed unrelated file is not evidence of an identity collision.
                continue;
            }
            if (existing == null || !existing.isObject()) {
                continue;
            }
            String conflict = null;
            if (sameText(existing, "channelCode", spec.channelCode)) {
                conflict = "channelCode " + spec.channelCode;
            } else if (sameText(existing, "packageName", spec.packageName)) {
                conflict = "Java package " + spec.packageName;
            } else if (sameText(existing, "groupId", spec.groupId)
                    && sameText(existing, "artifactId", spec.artifactId)
                    && sameText(existing, "version", spec.version)) {
                conflict = "Maven coordinates " + spec.groupId + ":" + spec.artifactId + ":" + spec.version;
            }
            if (conflict != null) {
                throw new GenerationException(4, "Project identity collision: " + conflict + " already appears in "
                        + manifest + "; choose different identifiers or a different workspace destination");
            }
        }
    }

    private static boolean sameText(JsonNode manifest, String field, String expected) {
        JsonNode value = manifest.get(field);
        return value != null && value.isTextual() && expected.equals(value.textValue());
    }

    private void checkDestination(Path destination) throws Exception {
        for (Path path = destination; path != null; path = path.getParent()) {
            if (Files.isSymbolicLink(path)) {
                throw new GenerationException(4, "Destination cannot traverse symbolic links: " + path);
            }
        }
        if (!Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        if (!Files.isDirectory(destination)) {
            throw new GenerationException(4, "Destination is not an empty directory");
        }
        try (Stream<Path> entries = Files.list(destination)) {
            if (entries.findAny().isPresent()) {
                throw new GenerationException(4, "Destination is not empty; existing files will not be overwritten");
            }
        }
    }

    private Path canonicalDestination(Path output) throws Exception {
        Path absolute = output.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(absolute)) {
            throw new GenerationException(4, "Destination itself cannot be a symbolic link");
        }
        Path existing = absolute.getParent();
        while (existing != null && !Files.exists(existing)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            throw new GenerationException(4, "Destination parent cannot be resolved");
        }
        // Resolve platform aliases such as macOS /var -> /private/var once before checking/writing.
        return existing.toRealPath().resolve(existing.relativize(absolute));
    }

    private Map<String, String> files(AdapterSpec spec, List<Capability> capabilities) throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        String javaRoot = "src/main/java/" + spec.packageName.replace('.', '/') + "/";
        try (BufferedReader input = new BufferedReader(new InputStreamReader(
                ProjectGenerator.class.getResourceAsStream("/templates/static-files.txt"), StandardCharsets.UTF_8))) {
            String name;
            while ((name = input.readLine()) != null) {
                if (!name.isEmpty()) {
                    files.put(javaRoot + name, "base/" + name + ".ftl");
                }
            }
        }
        files.put("pom.xml", "pom.xml.ftl");
        files.put(".gitignore", "gitignore.ftl");
        files.put("README.md", "README.md.ftl");
        files.put("DELIVERY.md", "DELIVERY.md.ftl");
        files.put("src/main/resources/META-INF/spring/" + spec.artifactId + ".xml", "spring.xml.ftl");
        files.put(javaRoot + "customize/security/ChannelSecurityCustomization.java", "security.java.ftl");
        files.put(javaRoot + "customize/transport/ChannelTransportCustomization.java", "transport.java.ftl");
        for (String family : Arrays.asList("payment", "refund", "notify")) {
            if (capabilities.stream().anyMatch(capability -> family.equals(capability.getFamily()))) {
                files.put(javaRoot + "spi/" + serviceName(family) + ".java", "spi.java.ftl");
            }
        }
        for (Capability capability : capabilities) {
            files.put("src/test/java/" + spec.packageName.replace('.', '/') + "/" + capability.getTitle()
                    + "DeliveryTest.java", "delivery-test.java.ftl");
            files.put("src/test/java/" + spec.packageName.replace('.', '/') + "/" + capability.getTitle()
                    + "SecurityContractTest.java", "security-test.java.ftl");
            files.put("src/test/resources/scenarios/" + capability.getMethod() + "/success.json", "scenario.json.ftl");
        }
        files.put("src/test/java/" + spec.packageName.replace('.', '/') + "/DeliveryScenario.java", "scenario.java.ftl");
        files.put("src/test/java/" + spec.packageName.replace('.', '/') + "/TemplateRuntimeTest.java", "template-runtime-test.java.ftl");
        files.put("src/test/java/" + spec.packageName.replace('.', '/') + "/GeneratedStructureTest.java", "structure-test.java.ftl");
        return files;
    }

    private Map<String, Object> model(AdapterSpec spec, List<Capability> capabilities) {
        Map<String, Object> model = JsonFiles.MAPPER.convertValue(spec, new TypeReference<Map<String, Object>>() { });
        model.put("capabilities", capabilities);
        model.put("templateVersion", TEMPLATE_VERSION);
        boolean active = false;
        for (Map<String, List<AdapterSpec.SecurityStep>> directions : spec.security.values()) {
            for (List<AdapterSpec.SecurityStep> steps : directions.values()) {
                for (AdapterSpec.SecurityStep step : steps) {
                    active |= !"none".equals(step.implementation);
                }
            }
        }
        model.put("activeSecurity", active);
        return model;
    }

    private void setFileContext(String path, List<Capability> capabilities, Map<String, Object> model) {
        for (Capability capability : capabilities) {
            if (path.endsWith("/" + capability.getTitle() + "DeliveryTest.java")
                    || path.endsWith("/" + capability.getTitle() + "SecurityContractTest.java")
                    || path.endsWith("/scenarios/" + capability.getMethod() + "/success.json")) {
                model.put("capability", capability);
            }
        }
        for (String family : Arrays.asList("payment", "refund", "notify")) {
            if (path.endsWith("/" + serviceName(family) + ".java")) {
                model.put("family", family);
                model.put("serviceClass", serviceName(family));
                model.put("serviceInterface", serviceName(family).substring("Channel".length()));
            }
        }
    }

    static String serviceName(String family) {
        if ("payment".equals(family)) { return "ChannelPaymentService"; }
        if ("refund".equals(family)) { return "ChannelRefundService"; }
        return "ChannelNotificationService";
    }

    private void render(Path file, String template, Map<String, Object> model) throws Exception {
        Files.createDirectories(file.getParent());
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            templates.getTemplate(template).process(model, writer);
        }
    }

    private static void deleteStage(Path stage) throws Exception {
        if (!Files.exists(stage)) { return; }
        try (Stream<Path> paths = Files.walk(stage)) {
            Path[] ordered = paths.sorted(Comparator.reverseOrder()).toArray(Path[]::new);
            for (Path path : ordered) {
                Files.delete(path);
            }
        }
    }
}
