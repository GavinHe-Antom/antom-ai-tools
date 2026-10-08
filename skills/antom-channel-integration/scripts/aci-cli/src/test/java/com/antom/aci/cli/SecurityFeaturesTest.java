/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Compact choices create unfinished demonstrations and never infer an institution's cryptographic protocol. */
class SecurityFeaturesTest {
    @TempDir Path temporary;

    @Test
    void expandsEveryFlagCombinationAcrossPaymentRefundAndNotifications() {
        for (boolean signature : new boolean[]{false, true}) {
            for (boolean encryption : new boolean[]{false, true}) {
                AdapterSpec spec = compactSpec(signature, encryption);
                List<Capability> capabilities = SpecValidator.validate(spec);
                assertEquals(spec.spi.size(), capabilities.size());
                assertNull(spec.securityFeatures, "Normalized specs must not retain conflicting compact input");
                assertEquals(new HashSet<>(spec.spi), spec.security.keySet());
                for (Capability capability : capabilities) {
                    Map<String, List<AdapterSpec.SecurityStep>> directions = spec.security.get(capability.getMethod());
                    assertEquals(new HashSet<>(capability.getDirections()), directions.keySet());
                    for (String direction : capability.getDirections()) {
                        List<AdapterSpec.SecurityStep> steps = directions.get(direction);
                        assertEquals(2, steps.size());
                        if ("request".equals(direction)) {
                            assertStep(steps.get(0), "sign", signature);
                            assertStep(steps.get(1), "encrypt", encryption);
                        } else {
                            assertStep(steps.get(0), "verify", signature);
                            assertStep(steps.get(1), "decrypt", encryption);
                        }
                    }
                }
            }
        }
    }

    @Test
    void normalizedSpecsRemainValidAfterRepeatedValidationAndJsonRoundTrip() throws Exception {
        AdapterSpec spec = compactSpec(true, true);
        List<Capability> capabilities = SpecValidator.validate(spec);
        String normalized = JsonFiles.MAPPER.writeValueAsString(spec);
        assertEquals(capabilities, SpecValidator.validate(spec));
        assertEquals(normalized, JsonFiles.MAPPER.writeValueAsString(spec));
        AdapterSpec saved = JsonFiles.MAPPER.readValue(normalized, AdapterSpec.class);
        assertNull(saved.securityFeatures);
        assertEquals(capabilities, SpecValidator.validate(saved));
        assertEquals(normalized, JsonFiles.MAPPER.writeValueAsString(saved));
    }

    @Test
    void rejectsMissingFeatureChoicesAndConflictingDetailedDefinition() {
        AdapterSpec missingSignature = compactSpec(true, true);
        missingSignature.securityFeatures.signature = null;
        assertEquals(2, assertThrows(GenerationException.class,
                () -> SpecValidator.validate(missingSignature)).exitCode);
        AdapterSpec missingEncryption = compactSpec(true, true);
        missingEncryption.securityFeatures.encryption = null;
        assertThrows(GenerationException.class, () -> SpecValidator.validate(missingEncryption));
        AdapterSpec conflict = compactSpec(true, false);
        conflict.security.put("pay", new java.util.LinkedHashMap<>());
        GenerationException failure = assertThrows(GenerationException.class, () -> SpecValidator.validate(conflict));
        assertEquals("Use securityFeatures or detailed security, not both", failure.getMessage());
        assertNotNull(conflict.securityFeatures, "Failed validation must not discard input choices");
    }

    @Test
    void rejectsAlgorithmAndParameterDefaultsOnNormalizedDemonstrations() {
        for (String field : Arrays.asList("algorithm", "keyAlgorithm", "cipherType", "parameters")) {
            AdapterSpec spec = compactSpec(true, true);
            SpecValidator.validate(spec);
            AdapterSpec.SecurityStep step = spec.security.get("pay").get("request").get(1);
            if ("algorithm".equals(field)) {
                step.algorithm = "AES";
            } else if ("keyAlgorithm".equals(field)) {
                step.keyAlgorithm = "AES";
            } else if ("cipherType".equals(field)) {
                step.cipherType = "SYMMETRIC";
            } else {
                step.parameters.put("mode", "GCM");
            }
            assertThrows(GenerationException.class, () -> SpecValidator.validate(spec), field);
        }
    }

    @Test
    void acceptsCompactJsonButRejectsUnknownFieldsAndIncompleteChoices() throws Exception {
        AdapterSpec input = compactSpec(false, true);
        String json = JsonFiles.MAPPER.writeValueAsString(input);
        AdapterSpec parsed = JsonFiles.MAPPER.readValue(json, AdapterSpec.class);
        assertFalse(parsed.securityFeatures.signature);
        assertTrue(parsed.securityFeatures.encryption);
        SpecValidator.validate(parsed);
        assertThrows(Exception.class, () -> JsonFiles.MAPPER.readValue(
                "{\"securityFeatures\":{\"signature\":true,\"encryption\":true,\"algorithm\":\"AES\"}}", AdapterSpec.class));
        AdapterSpec incomplete = JsonFiles.MAPPER.readValue(json.replace("\"signature\":false", "\"signature\":null"), AdapterSpec.class);
        assertThrows(GenerationException.class, () -> SpecValidator.validate(incomplete));
    }

    @Test
    void wizardAsksOnlyTwoSecurityQuestionsForEveryFlagCombination() throws Exception {
        for (boolean signature : new boolean[]{false, true}) {
            for (boolean encryption : new boolean[]{false, true}) {
                String answers = "example\r\033[B\r\r\r\r\r";
                if (signature) {
                    answers += "\033[B";
                }
                answers += "\r";
                if (encryption) {
                    answers += "\033[B";
                }
                answers += "\r";
                ByteArrayOutputStream transcript = new ByteArrayOutputStream();
                try (Terminal terminal = TerminalMenuTest.terminal(answers, transcript)) {
                    AdapterSpec spec = new InteractivePrompts(terminal).read();
                    assertEquals(Boolean.valueOf(signature), spec.securityFeatures.signature);
                    assertEquals(Boolean.valueOf(encryption), spec.securityFeatures.encryption);
                    assertTrue(spec.security.isEmpty());
                    SpecValidator.validate(spec);
                }
                String text = transcript.toString("UTF-8");
                assertTrue(text.contains("Need signing and verification?"));
                assertTrue(text.contains("Need encryption and decryption?"));
                for (String removed : Arrays.asList("operation order", "Confirmed rule reference", "SDK sign algorithm",
                        "SDK cipher algorithm", "Key algorithm", "Cipher type", "Cipher mode", "Cipher padding", "GCM tag bits")) {
                    assertFalse(text.contains(removed), removed);
                }
            }
        }
    }

    @Test
    void refundNotificationSelectionDoesNotEnableRefundTransactionOrQuery() throws Exception {
        String answers = "example\r\033[B\r\r\r\r\033[B\r\033[B\r\r\r";
        ByteArrayOutputStream transcript = new ByteArrayOutputStream();
        try (Terminal terminal = TerminalMenuTest.terminal(answers, transcript)) {
            AdapterSpec spec = new InteractivePrompts(terminal).read();
            assertEquals(Arrays.asList("pay", "notifyPayment", "notifyRefund"), spec.spi);
            assertEquals(3, SpecValidator.validate(spec).size());
            assertFalse(spec.security.containsKey("refund"));
            assertFalse(spec.security.containsKey("inquiryRefund"));
        }
        String text = transcript.toString("UTF-8");
        assertTrue(text.contains("Implement refund transaction?"));
        assertFalse(text.contains("Refund transaction/query services will not be generated"));
        assertTrue(text.contains("Selected SPI methods: pay, notifyPayment, notifyRefund"));
        assertFalse(text.contains("Implement refund inquiry?"));
    }

    @Test
    void rendersOnlySelectedDemonstrationsWithoutLiveProtocolDefaults() throws Exception {
        for (boolean signature : new boolean[]{false, true}) {
            for (boolean encryption : new boolean[]{false, true}) {
                AdapterSpec spec = compactSpec(signature, encryption);
                Path project = temporary.resolve("case-" + signature + "-" + encryption).resolve("adapter");
                inertSdk(spec);
                new ProjectGenerator().generate(spec, temporary, project, false);
                AdapterSpec saved = JsonFiles.read(project.resolve("adapter-spec.json"));
                assertNull(saved.securityFeatures);
                assertEquals(spec.spi.size(), SpecValidator.validate(saved).size());
                String code = text(project.resolve("src/main/java/com/example/adapter/customize/security/ChannelSecurityCustomization.java"));
                assertTrue(code.contains("demonstrations only, not a completed institution implementation"));
                assertTrue(code.contains("demonstration order is a skeleton"));
                assertFalse(code.contains("        request.setAlgorithm("));
                assertFalse(code.contains("        request.setCipherType("));
                assertFalse(code.contains("ChannelRequestContext.bind("));
                assertFalse(code.contains("ChannelRequestContext.clear("));
                if (signature || encryption) {
                    assertTrue(code.contains("private final PlatformChannelSecurityService platform"));
                    assertTrue(code.contains("String merchantId = ChannelRequestContext.current().getMerchantId()"));
                    assertTrue(code.contains("request.setMerchantId(merchantId)"));
                } else {
                    assertFalse(code.contains("PlatformChannelSecurityService"));
                    assertFalse(code.contains("ChannelRequestContext"));
                }
                for (String method : spec.spi) {
                    Capability capability = Capability.find(method);
                    String contract = text(project.resolve("src/test/java/com/example/adapter/"
                            + capability.getTitle() + "SecurityContractTest.java"));
                    for (String direction : capability.getDirections()) {
                        String title = capability.getTitle() + Character.toUpperCase(direction.charAt(0)) + direction.substring(1);
                        assertFalse(contract.contains(direction + "DemonstrationFailsUntilInstitutionProtocolIsImplemented"));
                        if ("request".equals(direction)) {
                            assertEquals(signature, code.contains("platform.sign(request" + title + "Sign(message))"));
                            assertEquals(encryption, code.contains("platform.encrypt(request" + title + "Encrypt(message))"));
                        } else {
                            assertEquals(signature, code.contains("if (!platform.verify(request" + title + "Verify(message, plainBody)))"));
                            assertEquals(encryption, code.contains("platform.decrypt(request" + title + "Decrypt(message, plainBody))"));
                            assertEquals(signature, contract.contains(direction + "VerifyFalseAbortsBeforeMapping"));
                        }
                        for (AdapterSpec.SecurityStep step : saved.security.get(method).get(direction)) {
                            if ("demo".equals(step.implementation)) {
                                String hook = title + Character.toUpperCase(step.operation.charAt(0)) + step.operation.substring(1);
                                assertTrue(code.contains("Complete " + hook + " demonstration with the confirmed institution protocol"));
                                assertTrue(contract.contains("Demonstration wiring must not invent an institution algorithm"));
                            }
                        }
                    }
                }
                if (signature) {
                    assertTrue(code.contains("// SecuritySignRequest request = new SecuritySignRequest();"));
                    assertTrue(code.contains("// request.setSignature(/* institution signature extracted"));
                    assertTrue(code.contains("// message.putHeader(/* institution-defined signature header name */, result);"));
                }
                if (encryption) {
                    assertTrue(code.contains("// SecurityEncryptRequest request = new SecurityEncryptRequest();"));
                    assertTrue(code.contains("// SecurityDecryptRequest request = new SecurityDecryptRequest();"));
                    assertTrue(code.contains("// parameters.setIvBase64(/* fresh protocol-defined IV"));
                    assertTrue(code.contains("// message.setBody(envelope);"));
                }
            }
        }
    }

    private static void assertStep(AdapterSpec.SecurityStep step, String operation, boolean enabled) {
        assertEquals(operation, step.operation);
        if (enabled) {
            assertEquals("demo", step.implementation);
            assertTrue(step.rule.contains("demonstration-only"));
        } else {
            assertEquals("none", step.implementation);
        }
        assertNull(step.algorithm);
        assertNull(step.keyAlgorithm);
        assertNull(step.cipherType);
        assertTrue(step.parameters.isEmpty());
    }

    private static AdapterSpec compactSpec(boolean signature, boolean encryption) {
        AdapterSpec spec = new AdapterSpec();
        spec.groupId = "com.example";
        spec.artifactId = "example-adapter";
        spec.packageName = "com.example.adapter";
        spec.channelCode = "example";
        spec.paymentType = "non-card";
        spec.spi = Arrays.asList("pay", "refund", "inquiryRefund", "notifyPayment", "notifyRefund");
        spec.securityFeatures = new AdapterSpec.SecurityFeatures();
        spec.securityFeatures.signature = signature;
        spec.securityFeatures.encryption = encryption;
        return spec;
    }

    private static String text(Path file) throws Exception {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    /** Inert inventory-only SDK avoids invoking private APIs or real cryptographic operations. */
    private void inertSdk(AdapterSpec spec) throws Exception {
        Path jar = temporary.resolve("sdk.jar");
        Set<String> entries = new HashSet<>();
        for (Capability capability : Capability.values()) {
            entries.add(capability.getRequestType());
            entries.add(capability.getResponseType());
        }
        entries.add("com.alipay.iacqintegrationhub.channel.sdk.api.security.PlatformChannelSecurityService");
        entries.add("com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService");
        entries.add("com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("META-INF/maven/com.alipay.iacqintegrationhub/common-sdk/pom.properties"));
            output.write("groupId=com.alipay.iacqintegrationhub\nartifactId=common-sdk\nversion=1.5.2\n".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            for (String entry : entries) {
                output.putNextEntry(new JarEntry(entry.replace('.', '/') + ".class"));
                output.closeEntry();
            }
        }
        Path pom = temporary.resolve("sdk.pom");
        Files.write(pom, ("<project><modelVersion>4.0.0</modelVersion><groupId>com.alipay.iacqintegrationhub</groupId>"
                + "<artifactId>common-sdk</artifactId><version>1.5.2</version></project>").getBytes(StandardCharsets.UTF_8));
        spec.sdkJar = jar.toString();
        spec.sdkPom = pom.toString();
    }
}
