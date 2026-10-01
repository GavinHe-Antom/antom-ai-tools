/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Runs a fixed Maven verification workflow on a trusted local project and checks its delivery artifact. */
final class AdapterPackager {
    Map<String, Object> pack(Path project, boolean offline) throws Exception {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows")) {
            throw new GenerationException(2, "ais package currently supports macOS/Linux; use a supported build environment");
        }
        Path root = project.toRealPath();
        AdapterSpec spec = JsonFiles.read(root.resolve("adapter-spec.json"));
        List<Capability> capabilities = SpecValidator.validate(spec);
        SdkBundle sdk = SdkBundle.load(spec, root, capabilities);
        JsonNode lock = JsonFiles.MAPPER.readTree(root.resolve("generation-lock.json").toFile());
        if (!sdk.sha256.equals(lock.path("sdkSha256").asText())
                || !SdkBundle.hash(sdk.pom, "SHA-256").equals(lock.path("sdkPomSha256").asText())) {
            throw new GenerationException(3, "Bundled SDK differs from generation-lock.json");
        }
        Path buildLog = Files.createTempFile(root, ".ais-build-", ".log");
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(10);
        Path dependencies = root.resolve("target/ais-delivery/resolved-dependencies.txt");
        runMaven(root, buildLog, offline, deadline, Arrays.asList("clean",
                "org.apache.maven.plugins:maven-dependency-plugin:3.6.1:list",
                "-DoutputAbsoluteArtifactFilename=true", "-Dmdep.outputScope=true", "-Dsort=true",
                "-DoutputEncoding=UTF-8", "-DoutputFile=" + dependencies));
        Map<String, Object> dependencyAudit = DependencyAudit.inspect(dependencies, sdk.jar, sdk.pom, spec.sdkVersion);
        runMaven(root, buildLog, offline, deadline, Arrays.asList("verify",
                "org.apache.maven.plugins:maven-dependency-plugin:3.6.1:tree",
                "-DoutputFile=target/ais-delivery/dependency-tree.txt"));
        // Recheck file contents after the build; neither Maven's cache nor the project is silently repaired.
        DependencyAudit.inspect(dependencies, sdk.jar, sdk.pom, spec.sdkVersion);
        checkTests(root, capabilities);
        Path jar = root.resolve("target/" + spec.artifactId + "-" + spec.version + ".jar");
        checkJar(jar, spec, capabilities);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "verified-locally");
        result.put("artifact", jar.toString());
        result.put("sha256", SdkBundle.hash(jar, "SHA-256"));
        result.put("sdkVersion", spec.sdkVersion);
        result.put("templateVersion", ProjectGenerator.TEMPLATE_VERSION);
        result.put("methods", spec.spi);
        result.put("tests", "all selected delivery tests passed without skips");
        result.put("buildLog", buildLog.toString());
        result.put("dependencyAudit", dependencyAudit);
        result.put("boundary", "Not production approval, protocol certification, secret scanning or dependency vulnerability scanning");
        JsonFiles.write(root.resolve("target/ais-delivery/report.json"), result);
        JsonFiles.write(root.resolve("target/ais-delivery/dependency-audit.json"), dependencyAudit);
        return result;
    }

    private void runMaven(Path root, Path log, boolean offline, long deadline, List<String> goals) throws Exception {
        String executable = "mvn";
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome != null && !mavenHome.trim().isEmpty()) {
            executable = Paths.get(mavenHome, "bin", "mvn").toString();
        }
        List<String> command = new ArrayList<>(Arrays.asList(executable, "-B", "--no-transfer-progress",
                "-DskipTests=false", "-Dmaven.test.skip=false", "-Dmdep.skip=false", "-Dstyle.color=never"));
        command.addAll(goals);
        if (offline) {
            command.add(1, "-o");
        }
        // This is not a shell command. The caller explicitly chose to build this trusted local project.
        Process process;
        try {
            process = new ProcessBuilder(command).directory(root.toFile())
                    .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile())).start();
        } catch (IOException failure) {
            throw new GenerationException(5, "Cannot start Maven; check PATH/MAVEN_HOME: " + failure.getMessage());
        }
        try {
            while (!process.waitFor(1, TimeUnit.SECONDS)) {
                if (System.nanoTime() > deadline || Files.size(log) > 50 * 1024 * 1024) {
                    process.destroyForcibly();
                    throw new GenerationException(5, "Maven exceeded time/output limit; see " + log);
                }
            }
        } catch (InterruptedException interrupted) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw interrupted;
        }
        if (process.exitValue() != 0) {
            throw new GenerationException(5, "Maven build/tests failed; see " + log);
        }
    }

    private void checkTests(Path root, List<Capability> capabilities) throws Exception {
        Path reports = root.resolve("target/surefire-reports");
        if (!Files.isDirectory(reports)) {
            throw new GenerationException(6, "No Surefire test reports; skipped tests are not delivery evidence");
        }
        List<String> passed = new ArrayList<>();
        try (Stream<Path> paths = Files.list(reports)) {
            for (Path file : paths.filter(path -> path.getFileName().toString().endsWith(".xml")).toArray(Path[]::new)) {
                Document document = SdkBundle.readXml(file);
                Element suite = document.getDocumentElement();
                for (String field : Arrays.asList("failures", "errors", "skipped")) {
                    if (!"0".equals(suite.getAttribute(field))) {
                        throw new GenerationException(6, "Delivery cannot contain failed or skipped tests: " + file);
                    }
                }
                NodeList cases = suite.getElementsByTagName("testcase");
                if (cases.getLength() > 0) {
                    passed.add(suite.getAttribute("name"));
                }
            }
        }
        for (Capability capability : capabilities) {
            if (passed.stream().noneMatch(name -> name.endsWith("." + capability.getTitle() + "DeliveryTest"))) {
                throw new GenerationException(6, "Missing executed delivery test for " + capability.getMethod());
            }
        }
    }

    private void checkJar(Path artifact, AdapterSpec spec, List<Capability> capabilities) throws Exception {
        if (!Files.isRegularFile(artifact)) {
            throw new GenerationException(6, "Expected ordinary JAR not found: " + artifact);
        }
        String packagePath = spec.packageName.replace('.', '/') + "/";
        try (JarFile jar = new JarFile(artifact.toFile())) {
            if (jar.getManifest() == null || !spec.sdkVersion.equals(
                    jar.getManifest().getMainAttributes().getValue("IAIS-SDK-Version"))) {
                throw new GenerationException(6, "Missing or mismatched IAIS-SDK-Version manifest");
            }
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if ((name.endsWith(".class") && !name.startsWith(packagePath)) || name.endsWith(".jar")
                        || name.endsWith(".pem") || name.endsWith(".key") || name.startsWith("BOOT-INF/")
                        || name.startsWith("scenarios/") || name.endsWith("DeliveryTest.class")) {
                    throw new GenerationException(6, "Unexpected runtime/test/credential artifact entry: " + name);
                }
            }
            for (Capability capability : capabilities) {
                String service = packagePath + "spi/" + ProjectGenerator.serviceName(capability.getFamily()) + ".class";
                if (jar.getEntry(service) == null) {
                    throw new GenerationException(6, "Missing selected SPI implementation: " + service);
                }
            }
        }
    }
}
