/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks that JSON validation and terminal choices share the SDK's cipher-family constraints. */
class CipherCompatibilityTest {
    @TempDir
    Path temporary;

    /** RSA must retain its supported padding options and never offer symmetric padding. */
    @Test
    void shouldRestrictRsaToCompatiblePadding() {
        assertEquals(Collections.singletonList("ECB"), CipherCompatibility.modes("RSA2"));
        assertEquals(Arrays.asList("PKCS1Padding", "NoPadding", "OAEPPadding",
                "OAEPWithSHA_256AndMGF1Padding", "OAEPWithSHA_1AndMGF1Padding"),
                CipherCompatibility.paddings("RSA2", "ECB"));
        for (String direction : Arrays.asList("request", "response")) {
            for (String padding : Arrays.asList("PKCS5Padding", "PKCS7Padding", "ISO10126Padding", "SSL3Padding")) {
                AdapterSpec invalid = cipherSpec("RSA2", "ECB", padding, direction);
                assertEquals(2, assertThrows(GenerationException.class,
                        () -> SpecValidator.validate(invalid)).exitCode);
            }
        }
    }

    /** Reject incompatible JSON configurations before any SDK or output-directory handling. */
    @Test
    void shouldRejectCrossFamilyPaddingFromJson() throws Exception {
        Path config = temporary.resolve("adapter-spec.json");
        for (String algorithm : Arrays.asList("AES", "DES", "DESede", "SM4", "SM4_BOCSZ")) {
            for (String padding : CipherCompatibility.paddings("RSA2", "ECB")) {
                if ("NoPadding".equals(padding)) {
                    continue;
                }
                for (String direction : Arrays.asList("request", "response")) {
                    JsonFiles.write(config, cipherSpec(algorithm, "CBC", padding, direction));
                    AdapterSpec parsed = JsonFiles.read(config);
                    GenerationException failure = assertThrows(GenerationException.class,
                            () -> SpecValidator.validate(parsed));
                    assertEquals(2, failure.exitCode);
                    assertTrue(failure.getMessage().contains("incompatible"));
                }
            }
        }
    }

    /** GCM is not a valid mode for the 64-bit block ciphers. */
    @Test
    void shouldRejectGcmForDesAndTripleDes() {
        for (String algorithm : Arrays.asList("DES", "DESede")) {
            assertEquals(Arrays.asList("CBC", "ECB", "CFB", "CTR"), CipherCompatibility.modes(algorithm));
            AdapterSpec invalid = cipherSpec(algorithm, "GCM", "NoPadding", "request");
            assertEquals(2, assertThrows(GenerationException.class,
                    () -> SpecValidator.validate(invalid)).exitCode);
        }
        for (String algorithm : Arrays.asList("AES", "SM4", "SM4_BOCSZ")) {
            assertTrue(CipherCompatibility.modes(algorithm).contains("GCM"));
        }
    }

    /** Every menu option must pass JSON validation without mutating the confirmed configuration. */
    @Test
    void shouldAcceptAllCompatibleMenuChoicesInBothDirections() throws Exception {
        for (String algorithm : Arrays.asList("RSA2", "AES", "DES", "DESede", "SM4", "SM4_BOCSZ")) {
            for (String mode : CipherCompatibility.modes(algorithm)) {
                assertTrue(SpecValidator.inCatalogue("mode", mode));
                for (String padding : CipherCompatibility.paddings(algorithm, mode)) {
                    assertTrue(SpecValidator.inCatalogue("padding", padding));
                    for (String direction : Arrays.asList("request", "response")) {
                        AdapterSpec spec = cipherSpec(algorithm, mode, padding, direction);
                        String original = JsonFiles.MAPPER.writeValueAsString(spec);
                        assertEquals(Collections.singletonList(Capability.PAY), SpecValidator.validate(spec));
                        assertEquals(original, JsonFiles.MAPPER.writeValueAsString(spec));
                    }
                }
            }
        }
        // The SDK retains legacy symmetric choices whose availability depends on the host provider.
        assertTrue(CipherCompatibility.paddings("AES", "CBC").contains("SSL3Padding"));
    }

    /** The SDK requires NoPadding for GCM, CTR and CFB regardless of provider capabilities. */
    @Test
    void shouldKeepStreamingPaddingAndGcmTagRequirements() {
        for (String mode : Arrays.asList("GCM", "CTR", "CFB")) {
            assertEquals(Collections.singletonList("NoPadding"), CipherCompatibility.paddings("AES", mode));
            AdapterSpec invalid = cipherSpec("AES", mode, "PKCS5Padding", "request");
            assertThrows(GenerationException.class, () -> SpecValidator.validate(invalid));
        }
        AdapterSpec missingTag = cipherSpec("AES", "GCM", "NoPadding", "request");
        missingTag.security.get("pay").get("request").get(1).parameters.remove("tagBitLength");
        assertThrows(GenerationException.class, () -> SpecValidator.validate(missingTag));
        AdapterSpec unexpectedTag = cipherSpec("RSA2", "ECB", "PKCS1Padding", "response");
        unexpectedTag.security.get("pay").get("response").get(1).parameters.put("tagBitLength", 128);
        assertThrows(GenerationException.class, () -> SpecValidator.validate(unexpectedTag));
    }

    /** Missing and unsupported options must fail closed rather than selecting an implicit default. */
    @Test
    void shouldRejectMissingOrUnknownChoices() {
        assertTrue(CipherCompatibility.modes("unknown").isEmpty());
        assertTrue(CipherCompatibility.paddings("RSA2", "CBC").isEmpty());
        for (String mode : Arrays.asList(null, "unknown", "CBC")) {
            AdapterSpec invalid = cipherSpec("RSA2", mode, "PKCS1Padding", "request");
            assertThrows(GenerationException.class, () -> SpecValidator.validate(invalid));
        }
        for (String padding : Arrays.asList(null, "unknown")) {
            AdapterSpec invalid = cipherSpec("RSA2", "ECB", padding, "request");
            assertThrows(GenerationException.class, () -> SpecValidator.validate(invalid));
        }
    }

    /** Envelope algorithms remain available and do not acquire block-cipher parameters. */
    @Test
    void shouldPreserveNonBlockAlgorithmConfiguration() {
        for (String algorithm : Arrays.asList("PGP", "CMS_DESded", "CMS_BASE64_DESded",
                "SM2", "SM2_CBMC", "SM2_CITICSH")) {
            AdapterSpec spec = cipherSpec(algorithm, null, null, "request");
            spec.security.get("pay").get("request").get(1).parameters.clear();
            assertTrue(CipherCompatibility.modes(algorithm).isEmpty());
            assertEquals(Collections.singletonList(Capability.PAY), SpecValidator.validate(spec));
        }
    }

    /** The real terminal wizard must default to PKCS1Padding after RSA2 is selected. */
    @Test
    void shouldSelectCompatibleRsaPaddingInTheWizard() throws Exception {
        String answers = "\r\r\r\rexample\r\033[B\r\r\r\r\r"
                + "\r\rprotocol-no-sign\r"
                + "\033[B\rprotocol-rsa-encrypt\r\033[B\033[B\033[B\r\r\r\r"
                + "\r\rprotocol-no-verify\r\rprotocol-no-decrypt\r";
        ByteArrayOutputStream transcript = new ByteArrayOutputStream();
        try (Terminal terminal = TerminalMenuTest.terminal(answers, transcript)) {
            AdapterSpec spec = new InteractivePrompts(terminal).read();
            AdapterSpec.SecurityStep encrypt = spec.security.get("pay").get("request").get(1);
            assertEquals("RSA2", encrypt.algorithm);
            assertEquals("ECB", encrypt.parameters.get("mode"));
            assertEquals("PKCS1Padding", encrypt.parameters.get("padding"));
            assertEquals(Collections.singletonList(Capability.PAY), SpecValidator.validate(spec));
        }
        assertTrue(transcript.toString("UTF-8").contains("Cipher padding: PKCS1Padding"));
        assertFalse(transcript.toString("UTF-8").contains("PKCS5Padding"));
    }

    /** Build a complete contract with one platform cipher step and explicit remaining no-op steps. */
    private AdapterSpec cipherSpec(String algorithm, String mode, String padding, String direction) {
        AdapterSpec spec = GeneratorTest.spec("pay");
        AdapterSpec.SecurityStep cipher = spec.security.get("pay").get(direction).get(1);
        cipher.implementation = "platform";
        cipher.algorithm = algorithm;
        cipher.cipherType = "ASYMMETRIC";
        if (CipherCompatibility.isSymmetric(algorithm)) {
            cipher.cipherType = "SYMMETRIC";
        }
        cipher.parameters.put("mode", mode);
        cipher.parameters.put("padding", padding);
        if ("GCM".equals(mode)) {
            cipher.parameters.put("tagBitLength", 128);
        }
        return spec;
    }
}
