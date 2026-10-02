/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Contract tests use a tiny inert JAR, never bundle or execute the private platform SDK. */
class GeneratorTest {
    @TempDir Path temporary;

    @Test
    void rejectsInvalidSdkPomWithoutExpandingExternalEntities() throws Exception {
        AdapterSpec spec = spec("pay");
        inertSdk(spec);
        Files.write(Paths.get(spec.sdkPom), ("<!DOCTYPE project [<!ENTITY xxe SYSTEM 'file:///not-accessed'>]>"
                + "<project><groupId>&xxe;</groupId></project>").getBytes(StandardCharsets.UTF_8));
        assertEquals(3, assertThrows(GenerationException.class, () -> new ProjectGenerator()
                .generate(spec, temporary, temporary.resolve("invalid-sdk"), false)).exitCode);
    }

    @Test
    void rejectsOldSameVersionSdkMissingContextBeforeCreatingOutput() throws Exception {
        AdapterSpec spec = spec("pay");
        inertSdk(spec, false);
        Path project = temporary.resolve("old-sdk-output");
        GenerationException failure = assertThrows(GenerationException.class,
                () -> new ProjectGenerator().generate(spec, temporary, project, false));
        assertEquals(3, failure.exitCode);
        assertEquals("SDK is missing required API: com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext",
                failure.getMessage());
        assertFalse(Files.exists(project));

        Path config = temporary.resolve("old-sdk-config.json");
        JsonFiles.write(config, spec);
        StringWriter output = new StringWriter();
        picocli.CommandLine cli = AisCli.commandLine();
        cli.setOut(new PrintWriter(output));
        cli.setErr(new PrintWriter(new StringWriter()));
        assertEquals(3, cli.execute("init", "--config", config.toString(), "--output", project.toString(), "--json"));
        assertTrue(JsonFiles.MAPPER.readTree(output.toString()).get("error").asText().contains("ChannelRequestContext"));
        assertFalse(Files.exists(project));
    }

    @Test
    void returnsStructuredJsonForCommandErrors() throws Exception {
        StringWriter output = new StringWriter();
        StringWriter errors = new StringWriter();
        picocli.CommandLine cli = AisCli.commandLine();
        cli.setOut(new PrintWriter(output));
        cli.setErr(new PrintWriter(errors));
        assertEquals(2, cli.execute("init", "--unknown", "--json"));
        assertEquals("failed", JsonFiles.MAPPER.readTree(output.toString()).get("status").asText());
    }

    @Test
    void rejectsDestinationSymlinkAndLeavesUnrelatedFilesUntouched() throws Exception {
        AdapterSpec spec = spec("pay");
        inertSdk(spec);
        Path protectedDirectory = Files.createDirectory(temporary.resolve("existing"));
        Files.write(protectedDirectory.resolve("keep.txt"), "unchanged".getBytes(StandardCharsets.UTF_8));
        Path symbolic = temporary.resolve("alias");
        Files.createSymbolicLink(symbolic, protectedDirectory);
        assertEquals(4, assertThrows(GenerationException.class,
                () -> new ProjectGenerator().generate(spec, temporary, symbolic, false)).exitCode);
        assertEquals("unchanged", text(protectedDirectory, "keep.txt"));
    }

    @Test
    void rejectsMismatchedCipherAndMissingExplicitPolicy() {
        AdapterSpec spec = spec("pay");
        AdapterSpec.SecurityStep encryption = spec.security.get("pay").get("request").get(1);
        encryption.implementation = "platform";
        encryption.algorithm = "AES";
        encryption.cipherType = "ASYMMETRIC";
        assertThrows(GenerationException.class, () -> SpecValidator.validate(spec));
        encryption.cipherType = "SYMMETRIC";
        encryption.parameters.put("mode", "GCM");
        encryption.parameters.put("padding", "PKCS5Padding");
        encryption.parameters.put("tagBitLength", 128);
        assertThrows(GenerationException.class, () -> SpecValidator.validate(spec));
        encryption.parameters.put("padding", "NoPadding");
        assertEquals(1, SpecValidator.validate(spec).size());
        encryption.parameters.put("ivBase64", "fixed-iv-is-not-generation-config");
        assertThrows(GenerationException.class, () -> SpecValidator.validate(spec));
    }

    @Test
    void acceptsNonCardWithoutUnsupportedMethods() {
        assertEquals(1, SpecValidator.validate(spec("pay")).size());
    }

    /** Unconnected notifications and removed callbacks are not configurable generation capabilities. */
    @Test
    void rejectsUnconnectedNotificationsAndRemovedCallbacks() {
        for (String method : Arrays.asList("receivePaymentNotify", "onlineBankPaymentNotify",
                "notifyReceivePayment", "notifyOnlineBankPayment", "acsUrlCallback", "onlineBankUrlCallback")) {
            GenerationException failure = assertThrows(GenerationException.class,
                    () -> SpecValidator.validate(spec("pay", "notifyPayment", method)), method);
            assertEquals(2, failure.exitCode);
            assertEquals("Unsupported or unconnected SPI method: " + method, failure.getMessage());
        }
    }

    /** Selecting every supported capability must not reintroduce unused notification or callback code. */
    @Test
    void generatedSourcesOmitUnconnectedNotificationsAndRemovedCallbacks() throws Exception {
        String[] methods = Arrays.stream(Capability.values()).map(Capability::getMethod).toArray(String[]::new);
        AdapterSpec spec = spec(methods);
        spec.paymentType = "card";
        spec.threeDS = "two";
        inertSdk(spec);
        Path project = temporary.resolve("supported-capabilities");
        new ProjectGenerator().generate(spec, temporary, project, false);

        List<String> removedNames = Arrays.asList("receivePaymentNotify", "onlineBankPaymentNotify",
                "notifyReceivePayment", "notifyOnlineBankPayment", "acsUrlCallback", "onlineBankUrlCallback",
                "RECEIVE_PAYMENT_NOTIFICATION", "ONLINE_BANK_PAYMENT_NOTIFICATION",
                "ACS_URL_CALLBACK", "ONLINE_BANK_URL_CALLBACK");
        List<Path> sources;
        try (java.util.stream.Stream<Path> paths = Files.walk(project.resolve("src"))) {
            sources = paths.filter(path -> path.toString().endsWith(".java"))
                    .collect(java.util.stream.Collectors.toList());
        }
        for (Path source : sources) {
            String content = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
            for (String name : removedNames) {
                assertFalse(content.contains(name), source + ": " + name);
            }
        }
        assertFalse(Files.exists(project.resolve("src/main/java/com/example/adapter/spi/ChannelCallbackService.java")));
        assertFalse(Files.exists(project.resolve("src/test/resources/scenarios/receivePaymentNotify")));
        assertFalse(Files.exists(project.resolve("src/test/resources/scenarios/onlineBankPaymentNotify")));
        assertFalse(Files.exists(project.resolve("src/test/resources/scenarios/acsUrlCallback")));
        assertFalse(Files.exists(project.resolve("src/test/resources/scenarios/onlineBankUrlCallback")));
    }

    @Test
    void rejectsUnsupportedCapabilitiesAndMissingAbstractMethods() {
        for (String method : Arrays.asList("capture", "notifyCapture", "notifyRefund", "inquiryRefund", "inquiryPayment", "notifyDispute")) {
            assertThrows(GenerationException.class, () -> SpecValidator.validate(spec(method)), method);
        }
    }

    @Test
    void requiresExactlyTwoCallAuthorization() {
        AdapterSpec spec = spec("pay", "authenticateAuthorize");
        spec.paymentType = "card";
        spec.threeDS = "two";
        assertEquals(2, SpecValidator.validate(spec).size());
        spec.threeDS = "one";
        assertThrows(GenerationException.class, () -> SpecValidator.validate(spec));
    }

    @Test
    void rejectsUnknownOrIncompleteSecurityChoices() {
        AdapterSpec missing = spec("pay");
        missing.security.get("pay").remove("response");
        assertThrows(GenerationException.class, () -> SpecValidator.validate(missing));
        AdapterSpec spec = spec("pay");
        AdapterSpec.SecurityStep sign = spec.security.get("pay").get("request").get(0);
        sign.implementation = "platform";
        sign.algorithm = "SHA256";
        final AdapterSpec invalid = spec;
        assertThrows(GenerationException.class, () -> SpecValidator.validate(invalid));
        sign.algorithm = "HMAC_SHA256_BASE64";
        assertEquals(1, SpecValidator.validate(spec).size());
    }

    @Test
    void strictJsonRejectsUnknownFieldsAndDuplicateKeys() throws Exception {
        Path file = temporary.resolve("input.json");
        Files.write(file, "{\"schemaVersion\":1,\"secretKey\":\"not-allowed\"}".getBytes(StandardCharsets.UTF_8));
        assertThrows(Exception.class, () -> JsonFiles.read(file));
        Files.write(file, "{\"schemaVersion\":1,\"schemaVersion\":1}".getBytes(StandardCharsets.UTF_8));
        assertThrows(Exception.class, () -> JsonFiles.read(file));
    }

    @Test
    void rejectsSourceInjectionAndMismatchedSdk() {
        AdapterSpec spec = spec("pay");
        spec.packageName = "com.example;import evil";
        assertThrows(GenerationException.class, () -> SpecValidator.validate(spec));
        spec.packageName = "com.example.adapter";
        spec.sdkVersion = "1.5.1";
        assertEquals(3, assertThrows(GenerationException.class, () -> SpecValidator.validate(spec)).exitCode);
    }

    @Test
    void nonCardInteractiveInputDoesNotAskAboutCaptureOr3ds() throws Exception {
        String answers = "\r\r\r\rexample\r\033[B\r\r\r\r\r\r\r";
        ByteArrayOutputStream transcript = new ByteArrayOutputStream();
        AdapterSpec spec = readPrompts(answers, transcript);
        assertEquals(Arrays.asList("pay"), spec.spi);
        assertFalse(transcript.toString().contains("capture?"));
        assertFalse(transcript.toString().contains("3DS calls"));
        assertTrue(transcript.toString("UTF-8").contains("Payment type: non-card"));
        assertFalse(transcript.toString("UTF-8").contains("Select number"));
        assertFalse(transcript.toString().contains("JAR path"));
        assertNull(spec.sdkJar);
        assertNull(spec.sdkPom);
        SpecValidator.validate(spec);
    }

    /** Selecting card and two-call 3DS includes the required authorization SPI. */
    @Test
    void selectsCardTwoCallWithArrowKeys() throws Exception {
        String answers = "\r\r\r\rexample\r\r\033[B\r\r\r\r\r\r\r\r";
        AdapterSpec spec = readPrompts(answers, new ByteArrayOutputStream());
        assertEquals("card", spec.paymentType);
        assertEquals("two", spec.threeDS);
        assertEquals(Arrays.asList("pay", "authenticateAuthorize"), spec.spi);
        SpecValidator.validate(spec);
    }

    /** Refund and its inquiry remain selectable without entering institution algorithms or parameters. */
    @Test
    void selectsRefundAndInquiryWithCompactSecurityChoices() throws Exception {
        String answers = "\r\r\r\rexample\r\033[B\r\r\r\033[B\r\033[B\r\r\033[B\r\033[B\r";
        ByteArrayOutputStream transcript = new ByteArrayOutputStream();
        AdapterSpec spec = readPrompts(answers, transcript);
        assertEquals(Arrays.asList("pay", "refund", "inquiryRefund"), spec.spi);
        assertTrue(spec.securityFeatures.signature);
        assertTrue(spec.securityFeatures.encryption);
        assertTrue(spec.security.isEmpty());
        assertEquals(3, SpecValidator.validate(spec).size());
        List<AdapterSpec.SecurityStep> request = spec.security.get("pay").get("request");
        assertEquals("demo", request.get(0).implementation);
        assertEquals("demo", request.get(1).implementation);
        assertNull(request.get(0).algorithm);
        assertNull(request.get(1).algorithm);
        assertTrue(transcript.toString("UTF-8").contains("Selected SPI methods: pay, refund, inquiryRefund"));
        assertFalse(transcript.toString("UTF-8").contains("SDK sign algorithm"));
    }

    /** EOF cancels initialization instead of generating a partially answered project. */
    @Test
    void rejectsIncompleteInteractiveInput() {
        assertEquals(2, assertThrows(GenerationException.class,
                () -> readPrompts("\004", new ByteArrayOutputStream())).exitCode);
    }

    /** Feed terminal events through the real prompt implementation, not a mocked selector. */
    private AdapterSpec readPrompts(String answers, ByteArrayOutputStream transcript) throws Exception {
        try (org.jline.terminal.Terminal terminal = TerminalMenuTest.terminal(answers, transcript)) {
            return new InteractivePrompts(terminal).read();
        }
    }

    /** The SDK comes from the CLI home even when configuration and output live elsewhere. */
    @Test
    void usesBundledSdkAndPersistsProjectLocalPaths() throws Exception {
        AdapterSpec spec = spec("pay");
        inertSdk(spec);
        Path sdk = Files.createDirectories(temporary.resolve("cli home/sdk"));
        Files.copy(Paths.get(spec.sdkJar), sdk.resolve("common-sdk-1.5.2.jar"));
        Files.copy(Paths.get(spec.sdkPom), sdk.resolve("common-sdk-1.5.2.pom"));
        spec.sdkJar = null;
        spec.sdkPom = null;
        String previous = System.getProperty("ais.cli.home");
        System.setProperty("ais.cli.home", sdk.getParent().toString());
        try {
            SdkDefaults.apply(spec);
            assertEquals(sdk.resolve("common-sdk-1.5.2.jar").toString(), spec.sdkJar);
            Path project = temporary.resolve("generated");
            new ProjectGenerator().generate(spec, temporary.resolve("unrelated"), project, false);
            AdapterSpec saved = JsonFiles.read(project.resolve("adapter-spec.json"));
            assertEquals("lib/repository/" + SdkBundle.COORDINATE_PATH + "common-sdk-1.5.2.jar", saved.sdkJar);
            assertEquals(spec.sdkVersion, saved.sdkVersion);
        } finally {
            restoreCliHome(previous);
        }
    }

    /** Missing distribution artifacts produce a machine-readable failure without creating output. */
    @Test
    void rejectsMissingDefaultSdkWithJsonError() throws Exception {
        Path config = temporary.resolve("config.json");
        JsonFiles.write(config, spec("pay"));
        String previous = System.getProperty("ais.cli.home");
        System.setProperty("ais.cli.home", temporary.resolve("missing-cli").toString());
        try {
            StringWriter output = new StringWriter();
            picocli.CommandLine cli = AisCli.commandLine();
            cli.setOut(new PrintWriter(output));
            cli.setErr(new PrintWriter(new StringWriter()));
            Path project = temporary.resolve("not-generated");
            assertEquals(3, cli.execute("init", "--config", config.toString(), "--output", project.toString(), "--json"));
            assertEquals(3, JsonFiles.MAPPER.readTree(output.toString()).get("exitCode").asInt());
            assertFalse(Files.exists(project));
        } finally {
            restoreCliHome(previous);
        }
    }

    /** Explicit paired paths remain supported, but partial overrides never mix SDK versions. */
    @Test
    void retainsExplicitSdkPairAndRejectsPartialOverride() {
        AdapterSpec spec = spec("pay");
        spec.sdkJar = "explicit.jar";
        assertEquals(3, assertThrows(GenerationException.class, () -> SdkDefaults.apply(spec)).exitCode);
        spec.sdkPom = "explicit.pom";
        SdkDefaults.apply(spec);
        assertEquals("explicit.jar", spec.sdkJar);
        assertEquals("explicit.pom", spec.sdkPom);
    }

    /** Restore the process setting so SDK resolution tests cannot leak into each other. */
    private static void restoreCliHome(String previous) {
        if (previous == null) {
            System.clearProperty("ais.cli.home");
        } else {
            System.setProperty("ais.cli.home", previous);
        }
    }

    @Test
    void rendersSelectedServicesAndFailsClosedSecurityOnly() throws Exception {
        AdapterSpec spec = spec("pay", "refund", "inquiryRefund", "notifyPayment", "notifyRefund");
        inertSdk(spec);
        AdapterSpec.SecurityStep sign = spec.security.get("pay").get("request").get(0);
        sign.implementation = "platform";
        sign.algorithm = "HMAC_SHA256_BASE64";
        Path project = temporary.resolve("generated");
        java.util.Map<String, Object> generated = new ProjectGenerator().generate(spec, temporary, project, false);
        assertInlineMappings(project, spec, generated);
        String service = text(project, "src/main/java/com/example/adapter/spi/ChannelPaymentService.java");
        assertTrue(service.contains("PayResponse pay("));
        assertFalse(service.contains("capture("));
        assertFalse(service.contains("authenticateAuthorize("));
        String refund = text(project, "src/main/java/com/example/adapter/spi/ChannelRefundService.java");
        assertTrue(refund.contains("RefundResponse refund(RefundRequest request)"));
        assertTrue(refund.contains("InquiryRefundResponse inquiryRefund(InquiryRefundRequest request)"));
        assertTrue(Files.exists(project.resolve("src/test/resources/scenarios/refund/input.json")));
        assertTrue(Files.exists(project.resolve("src/test/resources/scenarios/inquiryRefund/input.json")));
        assertTrue(text(project, "src/main/java/com/example/adapter/spi/ChannelNotificationService.java").contains("notifyRefund("));
        String security = text(project, "src/main/java/com/example/adapter/customize/security/ChannelSecurityCustomization.java");
        assertTrue(security.contains("platform.sign(requestPayRequestSign(message))"));
        assertTrue(security.indexOf("ChannelRequestContext.current().getMerchantId()")
                < security.indexOf("SecuritySignRequest request = buildPayRequestSign(message)"));
        assertTrue(security.contains("request.setMerchantId(merchantId)"));
        assertFalse(security.contains("ChannelRequestContext.bind("));
        assertFalse(security.contains("ChannelRequestContext.clear("));
        assertTrue(security.contains("throw new UnsupportedOperationException"));
        assertFalse(Files.exists(project.resolve("src/main/java/com/example/adapter/spi/ChannelCallbackService.java")));
        assertTrue(text(project, "pom.xml").contains("<scope>provided</scope>"));
        assertTrue(Files.exists(project.resolve("src/test/java/com/example/adapter/NotifyRefundDeliveryTest.java")));
        assertEquals(4, assertThrows(GenerationException.class,
                () -> new ProjectGenerator().generate(spec, temporary, project, false)).exitCode);
    }

    /** Every supported selected SPI owns its real typed anonymous extension, without a mapping Bean. */
    @Test
    void rendersAllSelectedMappingsInsideSpiMethods() throws Exception {
        String[] methods = Arrays.stream(Capability.values()).map(Capability::getMethod).toArray(String[]::new);
        AdapterSpec spec = spec(methods);
        spec.paymentType = "card";
        spec.threeDS = "two";
        inertSdk(spec);
        Path project = temporary.resolve("all-selected");
        java.util.Map<String, Object> generated = new ProjectGenerator().generate(spec, temporary, project, false);
        assertInlineMappings(project, spec, generated);
        String readme = text(project, "README.md");
        assertTrue(readme.contains("`refund` → `spi/ChannelRefundService.java`, inside `refund(...)`"));
        assertTrue(readme.contains("`inquiryRefund` → `spi/ChannelRefundService.java`, inside `inquiryRefund(...)`"));
        assertFalse(readme.contains("→ `customize/api/"));
    }

    /** Refund notifications do not implicitly add the separately selected transaction family. */
    @Test
    void refundNotificationSelectionDoesNotGenerateRefundTransactions() throws Exception {
        AdapterSpec spec = spec("pay", "notifyPayment", "notifyRefund");
        inertSdk(spec);
        Path project = temporary.resolve("refund-notification-only");
        java.util.Map<String, Object> generated = new ProjectGenerator().generate(spec, temporary, project, false);
        assertInlineMappings(project, spec, generated);
        assertFalse(Files.exists(project.resolve("src/main/java/com/example/adapter/spi/ChannelRefundService.java")));
        assertFalse(Files.exists(project.resolve("src/test/resources/scenarios/refund")));
        assertFalse(Files.exists(project.resolve("src/test/resources/scenarios/inquiryRefund")));
        String notification = text(project, "src/main/java/com/example/adapter/spi/ChannelNotificationService.java");
        assertTrue(notification.contains("notifyRefund(BaseChannelRequest request)"));
        assertFalse(notification.contains("notifyCapture("));
    }

    private void assertInlineMappings(Path project, AdapterSpec spec, java.util.Map<String, Object> generated)
            throws Exception {
        assertFalse(Files.exists(project.resolve("src/main/java/com/example/adapter/customize/api")));
        for (Object path : (List<?>) generated.get("files")) {
            assertFalse(path.toString().endsWith("Mapping.java"), path.toString());
            assertFalse(path.toString().contains("/customize/api/"), path.toString());
        }
        try (java.util.stream.Stream<Path> paths = Files.walk(project)) {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().endsWith("Mapping.java")));
        }
        for (String method : spec.spi) {
            Capability capability = Capability.find(method);
            String serviceName = ProjectGenerator.serviceName(capability.getFamily());
            String source = text(project, "src/main/java/com/example/adapter/spi/" + serviceName + ".java");
            assertTrue(source.contains("public " + serviceName + "(ChannelInvocationTemplate template)"));
            assertFalse(source.contains("customize.api"));
            String declaration = "public " + capability.getResponseName() + " " + method
                    + "(" + capability.getRequestName() + " request)";
            int start = source.indexOf(declaration);
            assertTrue(start >= 0, method);
            int nextMethod = source.indexOf("    /** {@inheritDoc} */", start + declaration.length());
            String methodSource = source.substring(start);
            if (nextMethod >= 0) {
                methodSource = source.substring(start, nextMethod);
            }
            if (capability.isNotification()) {
                assertTrue(methodSource.contains("new ChannelNotificationExtension<" + capability.getResponseName() + ">()"));
                assertTrue(methodSource.contains("void validate(BaseChannelRequest request, String plainBody)"));
                assertTrue(methodSource.contains(capability.getResponseName() + " map(BaseChannelRequest request, String plainBody)"));
            } else {
                assertTrue(methodSource.contains("new ChannelApiExtension<" + capability.getRequestName()
                        + ", " + capability.getResponseName() + ">()"));
                assertTrue(methodSource.contains("void validate(" + capability.getRequestName() + " request)"));
                assertTrue(methodSource.contains("JSONObject mapRequestBody(" + capability.getRequestName() + " request)"));
                assertTrue(methodSource.contains("Map<String, String> mapUrlParameters(" + capability.getRequestName() + " request)"));
                assertTrue(methodSource.contains(capability.getResponseName() + " mapResponse(" + capability.getRequestName()
                        + " request, ChannelHttpResult response)"));
            }
            assertTrue(methodSource.contains("TODO:"), method);
            assertTrue(methodSource.contains("throw new UnsupportedOperationException(\"Implement " + method), method);
        }
    }

    @Test
    void dryRunDoesNotWriteAndMissingSdkDoesNotCreateProject() throws Exception {
        AdapterSpec spec = spec("pay");
        inertSdk(spec);
        Path project = temporary.resolve("planned");
        assertEquals("planned", new ProjectGenerator().generate(spec, temporary, project, true).get("status"));
        assertFalse(Files.exists(project));
        spec.sdkJar = "absent.jar";
        assertEquals(3, assertThrows(GenerationException.class,
                () -> new ProjectGenerator().generate(spec, temporary, project, false)).exitCode);
        assertFalse(Files.exists(project));
    }

    @Test
    void cipherAndCustomSecurityRenderTypedPlatformCalls() throws Exception {
        AdapterSpec spec = spec("pay");
        inertSdk(spec);
        AdapterSpec.SecurityStep encrypt = spec.security.get("pay").get("request").get(1);
        encrypt.implementation = "platform";
        encrypt.algorithm = "AES";
        encrypt.cipherType = "SYMMETRIC";
        encrypt.parameters.put("mode", "GCM");
        encrypt.parameters.put("padding", "NoPadding");
        encrypt.parameters.put("tagBitLength", 128);
        AdapterSpec.SecurityStep verify = spec.security.get("pay").get("response").get(0);
        verify.implementation = "adapter";
        verify.keyAlgorithm = "HMAC_SHA256_BASE64";
        Path project = temporary.resolve("secure");
        new ProjectGenerator().generate(spec, temporary, project, false);
        String code = text(project, "src/main/java/com/example/adapter/customize/security/ChannelSecurityCustomization.java");
        assertTrue(code.contains("platform.encrypt(requestPayRequestEncrypt(message))"));
        assertTrue(code.contains("SecurityCipherMode.GCM"));
        assertTrue(code.contains("SecurityKeyPurpose.VERIFY"));
        assertTrue(code.contains("platform.queryKey(keyPayResponseVerify(message, plainBody))"));
        assertTrue(code.contains("String merchantId = ChannelRequestContext.current().getMerchantId()"));
        assertFalse(code.contains("merchantPayResponseVerify"));
    }

    @Test
    void generatesIndependentWireFixturesAndTypedSecurityBoundaryTests() throws Exception {
        AdapterSpec spec = spec("pay", "notifyPayment");
        inertSdk(spec);
        AdapterSpec.SecurityStep encryption = spec.security.get("pay").get("request").get(1);
        encryption.implementation = "platform";
        encryption.algorithm = "AES";
        encryption.cipherType = "SYMMETRIC";
        encryption.parameters.put("mode", "GCM");
        encryption.parameters.put("padding", "NoPadding");
        encryption.parameters.put("tagBitLength", 128);
        AdapterSpec.SecurityStep verify = spec.security.get("notifyPayment").get("notification").get(0);
        verify.implementation = "platform";
        verify.algorithm = "HMAC_SHA256_BASE64";
        Path project = temporary.resolve("delivery-evidence");
        java.util.Map<String, Object> generated = new ProjectGenerator().generate(spec, temporary, project, false);
        String delivery = text(project, "src/test/java/com/example/adapter/PayDeliveryTest.java");
        for (String field : Arrays.asList("pathParameters", "method", "contentType", "headers",
                "queryParameters", "formParameters", "attributes", "body")) {
            assertTrue(delivery.contains("\"" + field + "\""), field);
        }
        assertTrue(delivery.contains("eq(expectedPath), sent.capture()"));
        assertTrue(delivery.contains("SecurityEncryptRequest requestEncryptRequest"));
        assertTrue(delivery.contains("verify(security).encrypt(eq(requestEncryptRequest))"));
        assertTrue(delivery.contains("context.getBean(PaymentService.class).pay(input)"));
        assertTrue(delivery.contains("fixture(\"context\")"));
        assertTrue(delivery.contains("input.setChannelCode(channelCode)"));
        assertTrue(delivery.contains("input.setRuntimeEnv(contextFixture.getString(\"runtimeEnv\"))"));
        assertTrue(delivery.contains("ChannelRequestContext.bind(channelCode, merchantId, contextFixture.getString(\"runtimeEnv\"))"));
        assertTrue(delivery.contains("finally {\n                ChannelRequestContext.clear();"));
        assertTrue(delivery.contains("assertThrows(IllegalStateException.class, ChannelRequestContext::current)"));
        assertFalse(delivery.contains("input.getMerchantId()"));
        assertTrue(((List<?>) generated.get("files")).contains("src/test/resources/scenarios/pay/context.json"));
        assertEquals("{}", text(project, "src/test/resources/scenarios/pay/context.json").trim());
        assertEquals("{}", text(project, "src/test/resources/scenarios/pay/security.json").trim());
        assertTrue(Files.exists(project.resolve("src/test/resources/scenarios/pay/response-headers.json")));
        String contract = text(project, "src/test/java/com/example/adapter/PaySecurityContractTest.java");
        assertTrue(contract.contains("requestEncryptFailureAbortsBeforeHttp"));
        assertTrue(contract.contains("assertEquals(CIPHER_BODY, sent.getValue().getBody()"));
        assertTrue(contract.contains("getIvBase64()"));
        assertTrue(contract.contains("SecurityCipherPadding.NoPadding"));
        assertTrue(contract.contains("requestRejectsMissingContextBeforeHooksOrPlatformCalls"));
        assertTrue(contract.contains("requestRejectsMissingMerchantBeforeHooksOrPlatformCalls"));
        assertTrue(contract.contains("hostClearsIdentityBeforeNextInvocationOnSameThread"));
        assertTrue(contract.contains("synthetic-untrusted-hook-merchant"));
        assertFalse(contract.contains("javax.crypto"));
        assertFalse(contract.contains("java.lang.reflect"));
        String notification = text(project, "src/test/java/com/example/adapter/NotifyPaymentSecurityContractTest.java");
        assertTrue(notification.contains("notificationVerifyFalseAbortsBeforeMapping"));
        assertTrue(notification.contains("verifyNoInteractions(fixture.http, fixture.mapping)"));
        String structure = text(project, "src/test/java/com/example/adapter/GeneratedStructureTest.java");
        assertTrue(structure.contains("Arrays.asList(ChannelInvocationTemplate.class)"));
        assertTrue(structure.contains("BaseChannelRequest.builder()"));
        assertTrue(structure.contains("PaymentNotifyRequest.builder()"));
        assertTrue(structure.contains("notification.setExtendInfo("));
    }

    static AdapterSpec spec(String... methods) {
        AdapterSpec spec = new AdapterSpec();
        spec.groupId = "com.example";
        spec.artifactId = "example-adapter";
        spec.packageName = "com.example.adapter";
        spec.channelCode = "example";
        spec.paymentType = "non-card";
        spec.spi = Arrays.asList(methods);
        for (String method : methods) {
            if ("notifyDispute".equals(method)) { continue; }
            LinkedHashMap<String, List<AdapterSpec.SecurityStep>> directions = new LinkedHashMap<>();
            for (String direction : Capability.find(method).getDirections()) {
                List<String> operations = Arrays.asList("verify", "decrypt");
                if ("request".equals(direction)) { operations = Arrays.asList("sign", "encrypt"); }
                List<AdapterSpec.SecurityStep> steps = new ArrayList<>();
                for (String operation : operations) {
                    AdapterSpec.SecurityStep step = new AdapterSpec.SecurityStep();
                    step.operation = operation;
                    step.implementation = "none";
                    step.rule = "synthetic-protocol-none";
                    steps.add(step);
                }
                directions.put(direction, steps);
            }
            spec.security.put(method, directions);
        }
        return spec;
    }

    private String text(Path root, String name) throws Exception {
        return new String(Files.readAllBytes(root.resolve(name)), StandardCharsets.UTF_8);
    }

    private void inertSdk(AdapterSpec spec) throws Exception {
        inertSdk(spec, true);
    }

    private void inertSdk(AdapterSpec spec, boolean includesContext) throws Exception {
        Path jar = temporary.resolve("sdk.jar");
        java.util.Set<String> entries = new java.util.HashSet<>();
        try (java.util.jar.JarOutputStream output = new java.util.jar.JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new java.util.jar.JarEntry("META-INF/maven/com.alipay.iacqintegrationhub/common-sdk/pom.properties"));
            output.write("groupId=com.alipay.iacqintegrationhub\nartifactId=common-sdk\nversion=1.5.2\n".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            for (Capability capability : Capability.values()) {
                entries.add(capability.getRequestType());
                entries.add(capability.getResponseType());
            }
            entries.add("com.alipay.iacqintegrationhub.channel.sdk.api.security.PlatformChannelSecurityService");
            entries.add("com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService");
            if (includesContext) {
                entries.add("com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext");
            }
            for (String entry : entries) {
                output.putNextEntry(new java.util.jar.JarEntry(entry.replace('.', '/') + ".class"));
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
