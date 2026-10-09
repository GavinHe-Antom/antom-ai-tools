/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Institution intake derives identifiers directly and never asks for naming acceptance. */
class ProjectIdentityTest {
    @Test
    void derivesIdentifiersUsingOnlyDocumentedAsciiNormalization() {
        AdapterSpec spec = new AdapterSpec();
        ProjectIdentity.derive(spec, "Acme_12-X");
        assertEquals("acichannelacme12x", spec.channelCode);
        assertEquals("com.acme12x.channel", spec.groupId);
        assertEquals("channel-acme-12-x-adapter", spec.artifactId);
        assertEquals("com.acme12x.channel.adapter", spec.packageName);
        assertEquals("1.0.0-SNAPSHOT", spec.version);
    }

    @Test
    void protectsDigitAndKeywordNamespacesWithoutChangingChannelOrArtifactTokens() {
        for (String code : Arrays.asList("123", "class", "null")) {
            AdapterSpec spec = new AdapterSpec();
            ProjectIdentity.derive(spec, code);
            assertEquals("com.channel_" + code + ".channel", spec.groupId);
            assertEquals(spec.groupId + ".adapter", spec.packageName);
            assertEquals("acichannel" + code, spec.channelCode);
            assertEquals("channel-" + code + "-adapter", spec.artifactId);
        }
    }

    @Test
    void preservesExplicitIdentifiersAndUsesExplicitGroupForMissingPackage() {
        AdapterSpec spec = new AdapterSpec();
        spec.channelCode = "registered-code";
        spec.groupId = "org.owner.payments";
        spec.artifactId = "owner-adapter";
        spec.version = "2.0.0";
        ProjectIdentity.derive(spec, "Acme");
        assertEquals("registered-code", spec.channelCode);
        assertEquals("org.owner.payments", spec.groupId);
        assertEquals("owner-adapter", spec.artifactId);
        assertEquals("org.owner.payments.adapter", spec.packageName);
        assertEquals("2.0.0", spec.version);
        spec.packageName = "org.owner.custom.adapter";
        ProjectIdentity.derive(spec, "Acme");
        assertEquals("org.owner.custom.adapter", spec.packageName);
    }

    @Test
    void rejectsUnusableCodesWithoutTruncatingOrPartiallyApplyingNames() {
        char[] tooLong = new char[49];
        Arrays.fill(tooLong, 'a');
        for (String code : Arrays.asList("", "---___", "two words", "a.b", "a/b", "\u673a\u6784", new String(tooLong))) {
            AdapterSpec spec = new AdapterSpec();
            assertEquals(2, assertThrows(GenerationException.class,
                    () -> ProjectIdentity.derive(spec, code)).exitCode, code);
            assertNull(spec.channelCode);
            assertNull(spec.groupId);
            assertNull(spec.artifactId);
            assertNull(spec.packageName);
        }
    }

    @Test
    void acceptsNamesAtTheArtifactLengthBoundary() {
        char[] maximum = new char[48];
        Arrays.fill(maximum, 'a');
        AdapterSpec spec = new AdapterSpec();
        ProjectIdentity.derive(spec, new String(maximum));
        assertEquals(64, spec.artifactId.length());
        spec.paymentType = "non-card";
        spec.spi.add("pay");
        spec.securityFeatures = new AdapterSpec.SecurityFeatures();
        spec.securityFeatures.signature = false;
        spec.securityFeatures.encryption = false;
        assertEquals(1, SpecValidator.validate(spec).size());
    }

    @Test
    void wizardAsksOnePlainCodeQuestionAndReportsDerivedNamesWithoutAcceptance() throws Exception {
        ByteArrayOutputStream transcript = new ByteArrayOutputStream();
        try (Terminal terminal = TerminalMenuTest.terminal("Acme_12-X\r\033[B\r\r\r\r\r\r\r", transcript)) {
            AdapterSpec spec = new InteractivePrompts(terminal).read();
            assertEquals("channel-acme-12-x-adapter", spec.artifactId);
            assertEquals("non-card", spec.paymentType);
            SpecValidator.validate(spec);
        }
        String text = transcript.toString("UTF-8");
        assertTrue(text.contains("Company/institution code: Acme_12-X\r\n"), text);
        assertFalse(text.contains("Company/institution code ["));
        assertTrue(text.contains("Maven coordinates: com.acme12x.channel:channel-acme-12-x-adapter:1.0.0-SNAPSHOT"));
        assertTrue(text.contains("platform registration still required"));
        int lastQuestion = text.lastIndexOf("Need encryption and decryption?");
        assertTrue(lastQuestion >= 0, text);
        assertTrue(text.indexOf("Selected SPI methods:") > lastQuestion, text);
        assertTrue(text.indexOf("Maven coordinates:") > lastQuestion, text);
        String intake = text.substring(0, lastQuestion);
        assertFalse(intake.contains("SDK"), intake);
        assertFalse(intake.contains("platform"), intake);
        assertFalse(intake.contains("services will not be generated"), intake);
        for (String forbidden : Arrays.asList("Maven groupId", "Maven artifactId", "Adapter version",
                "Platform channelCode", "com.example", "my-channel", "Accept", "sandbox", "bundled JAR")) {
            assertFalse(text.contains(forbidden), forbidden);
        }
    }

    @Test
    void wizardRepeatsOnlyAnUnusableInstitutionCode() throws Exception {
        ByteArrayOutputStream transcript = new ByteArrayOutputStream();
        try (Terminal terminal = TerminalMenuTest.terminal("bad.code\rAcme\r\033[B\r\r\r\r\r\r\r", transcript)) {
            AdapterSpec spec = new InteractivePrompts(terminal).read();
            assertEquals("acichannelacme", spec.channelCode);
        }
        assertTrue(transcript.toString("UTF-8").contains("cannot yield valid project identifiers"));
    }
}
