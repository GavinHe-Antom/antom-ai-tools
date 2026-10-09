/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The BSD redistribution terms must survive resource copying and shading. */
class LicenseResourcesTest {
    @Test
    void includesJlineCopyrightConditionsAndDisclaimer() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/META-INF/licenses/jline-BSD-3-Clause.txt")) {
            assertNotNull(input);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            String license = new String(output.toByteArray(), StandardCharsets.UTF_8);
            assertTrue(license.contains("Copyright (c) 2002-2023"));
            assertTrue(license.contains("Redistributions in binary form must reproduce"));
            assertTrue(license.contains("Neither the name of JLine"));
            assertTrue(license.contains("OF THE POSSIBILITY OF SUCH DAMAGE."));
        }
    }
}
