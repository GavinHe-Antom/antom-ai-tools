/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
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
            throw new GenerationException(2, "aci package currently supports macOS/Linux; use a supported build environment");
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
        Path buildLog = Files.createTempFile(root, ".aci-build-", ".log");
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(10);
        Path dependencies = root.resolve("target/aci-delivery/resolved-dependencies.txt");
        runMaven(root, buildLog, offline, deadline, Arrays.asList("clean",
                "org.apache.maven.plugins:maven-dependency-plugin:3.6.1:list",
                "-DoutputAbsoluteArtifactFilename=true", "-Dmdep.outputScope=true", "-Dsort=true",
                "-DoutputEncoding=UTF-8", "-DoutputFile=" + dependencies));
        Map<String, Object> dependencyAudit = DependencyAudit.inspect(dependencies, sdk.jar, sdk.pom, spec.sdkVersion);
        runMaven(root, buildLog, offline, deadline, Arrays.asList("verify",
                "org.apache.maven.plugins:maven-dependency-plugin:3.6.1:tree",
                "-DoutputFile=target/aci-delivery/dependency-tree.txt"));
        // Recheck file contents after the build; neither Maven's cache nor the project is silently repaired.
        DependencyAudit.inspect(dependencies, sdk.jar, sdk.pom, spec.sdkVersion);
        Map<String, Set<String>> executed = checkTests(root, spec.packageName, capabilities);
        Map<String, Object> scenarioEvidence = DeliveryEvidence.inspect(root, spec.packageName, capabilities, executed);
        Path jar = root.resolve("target/" + spec.artifactId + "-" + spec.version + ".jar");
        checkJar(jar, spec, capabilities);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "verified-locally");
        result.put("artifact", jar.toString());
        result.put("sha256", SdkBundle.hash(jar, "SHA-256"));
        result.put("sdkVersion", spec.sdkVersion);
        result.put("templateVersion", ProjectGenerator.TEMPLATE_VERSION);
        result.put("methods", spec.spi);
        result.put("tests", "structure, framework security wiring and all declared real-SPI scenarios passed without skips");
        result.put("scenarioEvidence", scenarioEvidence);
        result.put("executedTests", executed);
        Map<String, Object> conformance = new LinkedHashMap<>();
        conformance.put("status", "not-executed-by-aci-package");
        conformance.put("requiredAction", "Run the platform-host adapter conformance test with synthetic keys in the target SDK runtime");
        conformance.put("boundary", "Local mock results establish parameter delegation, not production cryptographic compatibility");
        result.put("platformConformance", conformance);
        result.put("buildLog", buildLog.toString());
        result.put("dependencyAudit", dependencyAudit);
        result.put("boundary", "Not production approval, protocol certification, secret scanning or dependency vulnerability scanning");
        JsonFiles.write(root.resolve("target/aci-delivery/report.json"), result);
        JsonFiles.write(root.resolve("target/aci-delivery/dependency-audit.json"), dependencyAudit);
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

    Map<String, Set<String>> checkTests(Path root, String packageName, List<Capability> capabilities) throws Exception {
        Path reports = root.resolve("target/surefire-reports");
        if (!Files.isDirectory(reports)) {
            throw new GenerationException(6, "No Surefire test reports; skipped tests are not delivery evidence");
        }
        Map<String, Set<String>> passed = new LinkedHashMap<>();
        try (Stream<Path> paths = Files.list(reports)) {
            for (Path file : paths.filter(path -> path.getFileName().toString().endsWith(".xml")).toArray(Path[]::new)) {
                collectPassedTests(file, passed);
            }
        }
        List<String> required = new ArrayList<>();
        required.add(packageName + ".GeneratedStructureTest");
        for (Capability capability : capabilities) {
            required.add(packageName + "." + capability.getTitle() + "DeliveryTest");
            required.add(packageName + "." + capability.getTitle() + "SecurityContractTest");
        }
        for (String testClass : required) {
            if (!passed.containsKey(testClass)) {
                throw new GenerationException(6, "Missing executed, non-skipped required test class: " + testClass);
            }
        }
        return passed;
    }

    private void collectPassedTests(Path file, Map<String, Set<String>> passed) throws Exception {
        Document document = SdkBundle.readXml(file);
        Element root = document.getDocumentElement();
        List<Element> suites = new ArrayList<>();
        if ("testsuite".equals(root.getTagName())) {
            suites.add(root);
        } else if ("testsuites".equals(root.getTagName())) {
            // Aggregate counters are optional, but any reported failure or skip still blocks delivery.
            for (String field : Arrays.asList("failures", "errors", "skipped")) {
                if (root.hasAttribute(field) && testCount(root, field, file) != 0) {
                    throw new GenerationException(6, "Delivery cannot contain failed or skipped tests: " + file);
                }
            }
            NodeList children = root.getElementsByTagName("testsuite");
            for (int index = 0; index < children.getLength(); index++) {
                suites.add((Element) children.item(index));
            }
        } else {
            throw new GenerationException(6, "Unrecognized Surefire test report: " + file);
        }
        if (suites.isEmpty()) {
            throw new GenerationException(6, "No test suites in Surefire report: " + file);
        }
        for (Element suite : suites) {
            for (String field : Arrays.asList("failures", "errors", "skipped")) {
                if (testCount(suite, field, file) != 0) {
                    throw new GenerationException(6, "Delivery cannot contain failed or skipped tests: " + file);
                }
            }
            NodeList cases = suite.getElementsByTagName("testcase");
            if (testCount(suite, "tests", file) != cases.getLength()) {
                throw new GenerationException(6, "Test count does not match executed test cases: " + file);
            }
            for (int index = 0; index < cases.getLength(); index++) {
                Element testCase = (Element) cases.item(index);
                for (String outcome : Arrays.asList("failure", "error", "skipped")) {
                    if (testCase.getElementsByTagName(outcome).getLength() != 0) {
                        throw new GenerationException(6, "Delivery cannot contain failed or skipped test cases: " + file);
                    }
                }
                String className = testCase.getAttribute("classname");
                if (className.trim().isEmpty() || testCase.getAttribute("name").trim().isEmpty()) {
                    throw new GenerationException(6, "Missing executed test case identity: " + file);
                }
                passed.computeIfAbsent(className, name -> new LinkedHashSet<>()).add(testCase.getAttribute("name"));
            }
        }
    }

    private int testCount(Element suite, String field, Path file) {
        try {
            int count = Integer.parseInt(suite.getAttribute(field));
            if (count >= 0) {
                return count;
            }
        } catch (NumberFormatException invalid) {
            // Missing or malformed counters are not successful test evidence.
        }
        throw new GenerationException(6, "Invalid Surefire " + field + " count: " + file);
    }

    void checkJar(Path artifact, AdapterSpec spec, List<Capability> capabilities) throws Exception {
        if (!Files.isRegularFile(artifact)) {
            throw new GenerationException(6, "Expected ordinary JAR not found: " + artifact);
        }
        String packagePath = spec.packageName.replace('.', '/') + "/";
        try (JarFile jar = new JarFile(artifact.toFile())) {
            if (jar.getManifest() == null || !spec.sdkVersion.equals(
                    jar.getManifest().getMainAttributes().getValue("IAIS-SDK-Version"))) {
                throw new GenerationException(6, "Missing or mismatched AIS SDK version manifest (IAIS-SDK-Version)");
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
