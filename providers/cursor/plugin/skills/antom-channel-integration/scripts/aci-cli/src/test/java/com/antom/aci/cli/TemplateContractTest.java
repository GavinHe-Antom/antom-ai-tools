/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Skeleton-only expectations belong to the generator, not to an adapter after its hooks are completed. */
class TemplateContractTest {
    @TempDir Path temporary;

    @Test
    void defaultDemonstrationHooksAreUnfinishedWithoutRequiringAdaptersToStayUnfinished() throws Exception {
        for (boolean signature : new boolean[]{false, true}) {
            for (boolean encryption : new boolean[]{false, true}) {
                Path workspace = Files.createDirectory(temporary.resolve("features-" + signature + "-" + encryption));
                AdapterSpec spec = GeneratorTest.spec("pay", "notifyPayment");
                spec.security.clear();
                spec.securityFeatures = new AdapterSpec.SecurityFeatures();
                spec.securityFeatures.signature = signature;
                spec.securityFeatures.encryption = encryption;
                inertSdk(spec, workspace);
                Path project = workspace.resolve("adapter");
                new ProjectGenerator().generate(spec, workspace, project, false);
                String customization = text(project, "src/main/java/com/example/adapter/customize/security/ChannelSecurityCustomization.java");
                for (String method : spec.spi) {
                    Capability capability = Capability.find(method);
                    String contract = text(project, "src/test/java/com/example/adapter/"
                            + capability.getTitle() + "SecurityContractTest.java");
                    assertTrue(contract.contains("security = new SyntheticSecurity("));
                    assertTrue(contract.contains("selectedStepsPassTypedArgumentsInOrder"));
                    assertFalse(contract.contains("new ChannelSecurityCustomization("));
                    assertFalse(contract.contains("DemonstrationFailsUntilInstitutionProtocolIsImplemented"));
                    for (String direction : capability.getDirections()) {
                        for (AdapterSpec.SecurityStep step : spec.security.get(method).get(direction)) {
                            if (!"demo".equals(step.implementation)) {
                                continue;
                            }
                            String hook = capability.getTitle() + title(direction) + title(step.operation);
                            assertTrue(customization.contains("throw new UnsupportedOperationException(\"Complete "
                                    + hook + " demonstration with the confirmed institution protocol\")"), hook);
                            assertTrue(contract.contains(direction + title(step.operation) + "FailureAbortsBefore"));
                            if ("verify".equals(step.operation)) {
                                assertTrue(contract.contains(direction + "VerifyFalseAbortsBeforeMapping"));
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    void structureContractChecksSelectedSdkSignaturesAndMocksSupportedDependencies() throws Exception {
        AdapterSpec spec = GeneratorTest.spec("pay", "refund", "notifyPayment");
        inertSdk(spec, temporary);
        Path project = temporary.resolve("structure");
        new ProjectGenerator().generate(spec, temporary, project, false);
        String pom = text(project, "pom.xml");
        assertTrue(pom.contains("<IAIS-SDK-Version>${common-sdk.version}</IAIS-SDK-Version>"));
        String structure = text(project, "src/test/java/com/example/adapter/GeneratedStructureTest.java");
        assertTrue(structure.contains("assertSelectedSpiMethods("));
        assertTrue(structure.contains("pay(com.alipay.iacqintegrationhub.channel.sdk.spi.payment.request.PayRequest)"
                + ":com.alipay.iacqintegrationhub.channel.sdk.spi.payment.response.PayResponse"));
        assertTrue(structure.contains("Modifier.isPublic(method.getModifiers())"));
        assertTrue(structure.contains("Missing, extra or incorrectly typed SPI overrides"));
        assertTrue(structure.contains("context.getBeansOfType("));
        assertFalse(structure.contains("getDeclaredConstructors()"));
        assertTrue(structure.contains("context.registerBean(ResultCodeService.class, () -> mock(ResultCodeService.class))"));
        String delivery = text(project, "src/test/java/com/example/adapter/PayDeliveryTest.java");
        assertTrue(delivery.contains("context.registerBean(ResultCodeService.class, () -> resultCodes)"));
        assertTrue(delivery.contains("scenario.stubResultCodes(resultCodes)"));
        assertTrue(delivery.contains("scenario.verifyResultCodes(resultCodes)"));
    }

    @Test
    void protocolAdaptationGuidancePreservesBoundariesWithoutFreezingTheSkeleton() throws Exception {
        AdapterSpec spec = GeneratorTest.spec("pay");
        inertSdk(spec, temporary);
        Path project = temporary.resolve("guidance");
        new ProjectGenerator().generate(spec, temporary, project, false);
        String template = text(project, "src/main/java/com/example/adapter/template/ChannelInvocationTemplate.java");
        assertFalse(template.contains("External developers must not modify this class"));
        assertTrue(template.contains("Protocol-driven changes"));
        assertTrue(template.contains("platform-owned HTTP routing"));
        assertTrue(text(project, "README.md").contains("Completing the real hooks must leave those wiring tests passing"));
        assertTrue(text(project, "DELIVERY.md").contains("private helpers"));
    }

    private static String title(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String text(Path project, String relative) throws Exception {
        return new String(Files.readAllBytes(project.resolve(relative)), StandardCharsets.UTF_8);
    }

    /** Inert entry names are enough for rendering; semantic SDK execution lives in verify-generated.mjs. */
    private static void inertSdk(AdapterSpec spec, Path workspace) throws Exception {
        Path jar = workspace.resolve("sdk.jar");
        Set<String> entries = new HashSet<>(Arrays.asList(
                "com.alipay.iacqintegrationhub.channel.sdk.api.security.PlatformChannelSecurityService",
                "com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService",
                "com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext"));
        for (Capability capability : Capability.values()) {
            entries.add(capability.getRequestType());
            entries.add(capability.getResponseType());
        }
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry("META-INF/maven/com.alipay.iacqintegrationhub/common-sdk/pom.properties"));
            output.write("groupId=com.alipay.iacqintegrationhub\nartifactId=common-sdk\nversion=1.5.2\n"
                    .getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            for (String entry : entries) {
                output.putNextEntry(new JarEntry(entry.replace('.', '/') + ".class"));
                output.closeEntry();
            }
        }
        Path pom = workspace.resolve("sdk.pom");
        Files.write(pom, ("<project><modelVersion>4.0.0</modelVersion><groupId>com.alipay.iacqintegrationhub</groupId>"
                + "<artifactId>common-sdk</artifactId><version>1.5.2</version></project>").getBytes(StandardCharsets.UTF_8));
        spec.sdkJar = jar.toString();
        spec.sdkPom = pom.toString();
    }
}
