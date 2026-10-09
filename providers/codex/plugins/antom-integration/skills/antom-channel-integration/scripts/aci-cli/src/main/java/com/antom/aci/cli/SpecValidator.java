/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.SourceVersion;

/** Capability and security constraints shared by interactive and JSON entry points. */
final class SpecValidator {
    static List<Capability> validate(AdapterSpec spec) {
        require(spec != null && spec.schemaVersion == 1, "schemaVersion must be 1");
        require(javaName(spec.groupId) && javaName(spec.packageName), "Invalid groupId or Java packageName");
        require(spec.artifactId != null && spec.artifactId.matches("[a-z][a-z0-9-]{1,63}"), "Invalid artifactId");
        require(spec.version != null && spec.version.matches("[0-9][A-Za-z0-9._-]{0,63}"), "Invalid version");
        require(spec.channelCode != null && spec.channelCode.matches("[a-z][a-z0-9_-]{1,63}"), "Invalid channelCode");
        if (!"1.5.2".equals(spec.sdkVersion)) {
            throw new GenerationException(3, "Template 0.1.0 supports SDK 1.5.2 only");
        }
        require(Arrays.asList("card", "non-card").contains(spec.paymentType), "paymentType must be card or non-card");
        require(Arrays.asList("none", "one", "two").contains(spec.threeDS), "threeDS must be none, one or two");
        require(spec.spi != null && !spec.spi.isEmpty(), "Select at least one SPI method");
        Set<String> selected = new HashSet<>(spec.spi);
        require(selected.size() == spec.spi.size(), "Duplicate SPI methods");
        List<Capability> capabilities = new ArrayList<>();
        for (String method : spec.spi) {
            capabilities.add(Capability.find(method));
        }
        if ("non-card".equals(spec.paymentType)) {
            require("none".equals(spec.threeDS), "Non-card cannot enable 3DS");
            require(!selected.contains("capture") && !selected.contains("notifyCapture"), "Non-card cannot enable capture");
        }
        require(selected.contains("authenticateAuthorize") == "two".equals(spec.threeDS),
                "Two-call 3DS requires authenticateAuthorize; other modes must omit it");
        for (Capability capability : capabilities) {
            if ("payment".equals(capability.getFamily())) {
                require(selected.contains("pay"), "PaymentService requires pay");
            }
            if ("refund".equals(capability.getFamily())) {
                require(selected.contains("refund"), "RefundService requires refund");
            }
            if (capability.isNotification()) {
                require(selected.contains("notifyPayment"), "NotificationService requires notifyPayment");
            }
        }
        normalizeSecurityFeatures(spec, capabilities);
        require(spec.security != null && spec.security.keySet().equals(selected),
                "security must explicitly cover exactly the selected methods");
        for (Capability capability : capabilities) {
            validateSecurity(capability, spec.security.get(capability.getMethod()));
        }
        return capabilities;
    }

    /** Normalize compact inputs once so persisted specs can be validated again by packaging. */
    private static void normalizeSecurityFeatures(AdapterSpec spec, List<Capability> capabilities) {
        if (spec.securityFeatures == null) {
            return;
        }
        require(spec.security == null || spec.security.isEmpty(),
                "Use securityFeatures or detailed security, not both");
        require(spec.securityFeatures.signature != null && spec.securityFeatures.encryption != null,
                "securityFeatures requires both signature and encryption booleans");
        Map<String, Map<String, List<AdapterSpec.SecurityStep>>> security = new LinkedHashMap<>();
        for (Capability capability : capabilities) {
            Map<String, List<AdapterSpec.SecurityStep>> directions = new LinkedHashMap<>();
            for (String direction : capability.getDirections()) {
                List<AdapterSpec.SecurityStep> steps = new ArrayList<>();
                if ("request".equals(direction)) {
                    steps.add(featureStep("sign", spec.securityFeatures.signature));
                    steps.add(featureStep("encrypt", spec.securityFeatures.encryption));
                } else {
                    // Demonstration order only; developers must confirm the institution's actual wire protocol.
                    steps.add(featureStep("verify", spec.securityFeatures.signature));
                    steps.add(featureStep("decrypt", spec.securityFeatures.encryption));
                }
                directions.put(direction, steps);
            }
            security.put(capability.getMethod(), directions);
        }
        spec.security = security;
        spec.securityFeatures = null;
    }

    private static AdapterSpec.SecurityStep featureStep(String operation, boolean enabled) {
        AdapterSpec.SecurityStep step = new AdapterSpec.SecurityStep();
        step.operation = operation;
        if (enabled) {
            step.implementation = "demo";
            step.rule = "demonstration-only feature selected - institution protocol not implemented";
        } else {
            step.implementation = "none";
            step.rule = "feature not selected during project generation";
        }
        return step;
    }

    private static void validateSecurity(Capability capability, Map<String, List<AdapterSpec.SecurityStep>> directions) {
        require(directions != null && directions.keySet().equals(new HashSet<>(capability.getDirections())),
                "Explicit security directions required for " + capability.getMethod());
        for (String direction : capability.getDirections()) {
            List<String> operations = Arrays.asList("verify", "decrypt");
            if ("request".equals(direction)) {
                operations = Arrays.asList("sign", "encrypt");
            }
            List<AdapterSpec.SecurityStep> steps = directions.get(direction);
            require(steps != null && steps.size() == 2, "Declare both security operations, using none when inapplicable");
            Set<String> seen = new HashSet<>();
            for (AdapterSpec.SecurityStep step : steps) {
                require(step != null && operations.contains(step.operation) && seen.add(step.operation),
                        "Duplicate or invalid security operation in " + capability.getMethod() + "/" + direction);
                require(Arrays.asList("none", "platform", "adapter", "demo").contains(step.implementation), "Invalid security implementation");
                require(step.rule != null && step.rule.matches("[A-Za-z0-9][A-Za-z0-9 ._:/-]{0,255}"),
                        "Each step requires a non-secret rule reference/reason (ASCII, at most 256 characters)");
                require(step.parameters != null, "parameters cannot be null");
                if ("none".equals(step.implementation) || "demo".equals(step.implementation)) {
                    require(step.algorithm == null && step.keyAlgorithm == null
                            && step.cipherType == null && step.parameters.isEmpty(),
                            "none/demo cannot declare algorithm or parameters");
                    continue;
                }
                if ("adapter".equals(step.implementation)) {
                    require(inCatalogue("key", step.keyAlgorithm), "Unknown keyAlgorithm");
                    require(step.algorithm == null && step.cipherType == null && step.parameters.isEmpty(),
                            "adapter selects keyAlgorithm; implement protocol options in its customization hook");
                } else {
                    require(step.keyAlgorithm == null, "platform selects key classification internally");
                    if ("sign".equals(step.operation) || "verify".equals(step.operation)) {
                        require(inCatalogue("sign", step.algorithm), "Unknown signing algorithm");
                        require(step.cipherType == null && step.parameters.isEmpty(), "Signing does not accept cipher parameters");
                    } else {
                        require(inCatalogue("cipher", step.algorithm), "Unknown cipher algorithm");
                        validateCipher(step);
                    }
                }
            }
        }
    }

    private static void validateCipher(AdapterSpec.SecurityStep step) {
        boolean symmetric = CipherCompatibility.isSymmetric(step.algorithm);
        String expected = "ASYMMETRIC";
        if (symmetric) {
            expected = "SYMMETRIC";
        }
        require(expected.equals(step.cipherType), "cipherType does not match algorithm");
        // Runtime IV/AAD/PGP material must be derived in the generated typed request hook, never frozen here.
        Set<String> allowed = new HashSet<>(Arrays.asList("mode", "padding", "tagBitLength"));
        require(allowed.containsAll(step.parameters.keySet()), "Only static mode/padding/tagBitLength belong in generation config");
        if (symmetric || "RSA2".equals(step.algorithm)) {
            require(inCatalogue("mode", String.valueOf(step.parameters.get("mode"))), "Explicit cipher mode required");
            require(inCatalogue("padding", String.valueOf(step.parameters.get("padding"))), "Explicit cipher padding required");
            String mode = String.valueOf(step.parameters.get("mode"));
            String padding = String.valueOf(step.parameters.get("padding"));
            require(CipherCompatibility.modes(step.algorithm).contains(mode),
                    "Cipher mode " + mode + " is incompatible with " + step.algorithm);
            require(CipherCompatibility.paddings(step.algorithm, mode).contains(padding),
                    "Cipher padding " + padding + " is incompatible with " + step.algorithm + "/" + mode);
            if ("GCM".equals(mode)) {
                require(Arrays.asList(96, 104, 112, 120, 128).contains(step.parameters.get("tagBitLength")), "Invalid GCM tagBitLength");
            } else {
                require(!step.parameters.containsKey("tagBitLength"), "tagBitLength is GCM-only");
            }
        } else {
            require(step.parameters.isEmpty(), "This algorithm has no static block-cipher parameters");
        }
    }

    static boolean inCatalogue(String name, String value) {
        return catalogue(name).contains(value);
    }

    /** Share the bundled SDK choices between interactive menus and configuration validation. */
    static List<String> catalogue(String name) {
        try (InputStream input = SpecValidator.class.getResourceAsStream("/catalogue.json")) {
            JsonNode items = JsonFiles.MAPPER.readTree(input).get(name);
            List<String> values = new ArrayList<>();
            for (JsonNode item : items) {
                values.add(item.asText());
            }
            return values;
        } catch (IOException e) {
            throw new GenerationException(3, "Cannot read bundled algorithm catalogue: " + e.getMessage());
        }
    }

    private static boolean javaName(String value) {
        return value != null && value.length() < 160 && SourceVersion.isName(value)
                && value.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+");
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw new GenerationException(2, message);
        }
    }
}
