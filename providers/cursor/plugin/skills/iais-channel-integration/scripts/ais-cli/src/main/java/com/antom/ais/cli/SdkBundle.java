/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/** Validates a platform-supplied SDK without executing classes from the supplied JAR. */
final class SdkBundle {
    static final String COORDINATE_PATH = "com/alipay/iacqintegrationhub/common-sdk/1.5.2/";
    final Path jar;
    final Path pom;
    final String sha256;

    static SdkBundle load(AdapterSpec spec, Path base, List<Capability> capabilities) {
        try {
            return new SdkBundle(spec, base, capabilities);
        } catch (GenerationException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new GenerationException(3, "Cannot validate SDK JAR/consumer POM: " + failure.getMessage());
        }
    }

    SdkBundle(AdapterSpec spec, Path inputBase, List<Capability> capabilities) throws Exception {
        if (spec.sdkJar == null || spec.sdkPom == null) {
            throw new GenerationException(3, "Provide both sdkJar and standalone consumer sdkPom");
        }
        jar = inputBase.resolve(Paths.get(spec.sdkJar)).normalize();
        pom = inputBase.resolve(Paths.get(spec.sdkPom)).normalize();
        if (!Files.isRegularFile(jar) || !Files.isRegularFile(pom)) {
            throw new GenerationException(3, "SDK JAR/POM not found");
        }
        if (Files.size(jar) > 50 * 1024 * 1024 || Files.size(pom) > 1024 * 1024) {
            throw new GenerationException(3, "Unexpected SDK artifact size");
        }
        validatePom(pom);
        try (JarFile archive = new JarFile(jar.toFile())) {
            JarEntry metadata = archive.getJarEntry("META-INF/maven/com.alipay.iacqintegrationhub/common-sdk/pom.properties");
            if (metadata == null) {
                throw new GenerationException(3, "SDK JAR has no Maven coordinates");
            }
            Properties coordinates = new Properties();
            try (InputStream input = archive.getInputStream(metadata)) {
                coordinates.load(input);
            }
            if (!"1.5.2".equals(coordinates.getProperty("version"))
                    || !"common-sdk".equals(coordinates.getProperty("artifactId"))
                    || !"com.alipay.iacqintegrationhub".equals(coordinates.getProperty("groupId"))) {
                throw new GenerationException(3, "SDK JAR coordinate mismatch");
            }
            for (Capability capability : capabilities) {
                requireClass(archive, capability.getRequestType());
                requireClass(archive, capability.getResponseType());
            }
            requireClass(archive, "com.alipay.iacqintegrationhub.channel.sdk.api.security.PlatformChannelSecurityService");
            requireClass(archive, "com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService");
            requireClass(archive, "com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext");
        }
        sha256 = hash(jar, "SHA-256");
    }

    private static void requireClass(JarFile archive, String type) {
        if (archive.getJarEntry(type.replace('.', '/') + ".class") == null) {
            throw new GenerationException(3, "SDK is missing required API: " + type);
        }
    }

    static Document readXml(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder().parse(file.toFile());
    }

    private static void validatePom(Path pom) throws Exception {
        Document document = readXml(pom);
        for (String forbidden : new String[]{"parent", "build", "profiles", "repositories", "pluginRepositories"}) {
            if (document.getElementsByTagName(forbidden).getLength() > 0) {
                throw new GenerationException(3, "Supply the flattened consumer POM, without " + forbidden);
            }
        }
        requirePomValue(document, "groupId", "com.alipay.iacqintegrationhub");
        requirePomValue(document, "artifactId", "common-sdk");
        requirePomValue(document, "version", "1.5.2");
    }

    private static void requirePomValue(Document document, String tag, String expected) {
        if (document.getElementsByTagName(tag).getLength() == 0
                || !expected.equals(document.getElementsByTagName(tag).item(0).getTextContent().trim())) {
            throw new GenerationException(3, "SDK consumer POM mismatch: " + tag);
        }
    }

    void copyTo(Path output) throws IOException {
        Path directory = output.resolve("lib/repository/" + COORDINATE_PATH);
        Files.createDirectories(directory);
        copyWithChecksums(jar, directory.resolve("common-sdk-1.5.2.jar"));
        copyWithChecksums(pom, directory.resolve("common-sdk-1.5.2.pom"));
    }

    private static void copyWithChecksums(Path source, Path target) throws IOException {
        Files.copy(source, target);
        Files.write(Paths.get(target + ".sha1"), hash(target, "SHA-1").getBytes(StandardCharsets.US_ASCII));
        Files.write(Paths.get(target + ".sha256"), hash(target, "SHA-256").getBytes(StandardCharsets.US_ASCII));
    }

    static String hash(Path file, String algorithm) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, length);
                }
            }
            StringBuilder result = new StringBuilder();
            for (byte value : digest.digest()) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Required JDK digest unavailable", e);
        }
    }
}
