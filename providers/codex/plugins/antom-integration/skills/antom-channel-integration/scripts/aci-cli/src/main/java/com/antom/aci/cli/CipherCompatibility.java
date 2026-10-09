/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Shared SDK/IBCM cipher-family constraints, independent of the CLI machine's crypto providers. */
final class CipherCompatibility {
    /** Prevent construction of the shared compatibility rules. */
    private CipherCompatibility() {
    }

    /** Identify algorithms that use symmetric key material in the SDK. */
    static boolean isSymmetric(String algorithm) {
        return Arrays.asList("AES", "DES", "DESede", "SM4", "SM4_BOCSZ").contains(algorithm);
    }

    /** Return applicable block modes in the existing catalogue order. */
    static List<String> modes(String algorithm) {
        if ("RSA2".equals(algorithm)) {
            return Collections.singletonList("ECB");
        }
        if ("DES".equals(algorithm) || "DESede".equals(algorithm)) {
            // GCM requires a 128-bit block cipher; DES and Triple DES have 64-bit blocks.
            return Arrays.asList("CBC", "ECB", "CFB", "CTR");
        }
        if (isSymmetric(algorithm)) {
            return Arrays.asList("CBC", "ECB", "GCM", "CFB", "CTR");
        }
        return Collections.emptyList();
    }

    /** Return compatible padding names without changing the selected algorithm or mode. */
    static List<String> paddings(String algorithm, String mode) {
        if (!modes(algorithm).contains(mode)) {
            return Collections.emptyList();
        }
        if ("RSA2".equals(algorithm)) {
            return Arrays.asList("PKCS1Padding", "NoPadding", "OAEPPadding",
                    "OAEPWithSHA_256AndMGF1Padding", "OAEPWithSHA_1AndMGF1Padding");
        }
        if (Arrays.asList("GCM", "CTR", "CFB").contains(mode)) {
            return Collections.singletonList("NoPadding");
        }
        // Keep legacy provider-dependent symmetric padding choices exposed by the SDK.
        return Arrays.asList("PKCS5Padding", "PKCS7Padding", "NoPadding", "ISO10126Padding", "SSL3Padding");
    }
}
