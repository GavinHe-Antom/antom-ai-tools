/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.util.Locale;
import javax.lang.model.SourceVersion;

/** Derives generation conventions from an institution code without implying platform registration. */
final class ProjectIdentity {
    private ProjectIdentity() {
    }

    static void derive(AdapterSpec spec, String institutionCode) {
        if (institutionCode == null || !institutionCode.matches("[A-Za-z0-9_-]+")) {
            throw invalidCode();
        }
        String artifactToken = institutionCode.toLowerCase(Locale.ROOT).replace('_', '-');
        String channelToken = artifactToken.replace("-", "");
        if (channelToken.isEmpty()) {
            throw invalidCode();
        }
        String namespaceToken = channelToken;
        if (Character.isDigit(namespaceToken.charAt(0)) || SourceVersion.isKeyword(namespaceToken)) {
            namespaceToken = "channel_" + namespaceToken;
        }

        String channelCode = spec.channelCode;
        if (channelCode == null) {
            channelCode = "iaischannel" + channelToken;
        }
        String groupId = spec.groupId;
        if (groupId == null) {
            groupId = "com." + namespaceToken + ".channel";
        }
        String artifactId = spec.artifactId;
        if (artifactId == null) {
            artifactId = "channel-" + artifactToken + "-adapter";
        }
        String packageName = spec.packageName;
        if (packageName == null) {
            packageName = groupId + ".adapter";
        }
        if (!validJavaName(groupId) || !validJavaName(packageName)
                || !artifactId.matches("[a-z][a-z0-9-]{1,63}")
                || !channelCode.matches("[a-z][a-z0-9_-]{1,63}")) {
            throw invalidCode();
        }
        // Publish all derived values together; an invalid code must not leave a partial identity.
        spec.channelCode = channelCode;
        spec.groupId = groupId;
        spec.artifactId = artifactId;
        spec.packageName = packageName;
    }

    private static boolean validJavaName(String value) {
        return value != null && value.length() < 160 && SourceVersion.isName(value)
                && value.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+");
    }

    private static GenerationException invalidCode() {
        return new GenerationException(2, "Company/institution code cannot yield valid project identifiers.");
    }
}
