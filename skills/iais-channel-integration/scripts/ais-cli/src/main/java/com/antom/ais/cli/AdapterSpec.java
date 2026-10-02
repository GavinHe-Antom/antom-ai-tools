/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicit generation inputs; no credentials or executable hooks are accepted. */
public final class AdapterSpec {
    /** Configuration format version, distinct from the SDK and template versions. */
    public int schemaVersion = 1;
    /** Maven group of the external developer. */
    public String groupId;
    /** Generated Maven artifact and default project directory name. */
    public String artifactId;
    /** External adapter release version. */
    public String version = "1.0.0-SNAPSHOT";
    /** Unique Java root package scanned only by the owning channel module. */
    public String packageName;
    /** Platform-registered channel identifier, not a domain or institution merchant. */
    public String channelCode;
    /** Exact platform SDK version supported by this template. */
    public String sdkVersion = "1.5.2";
    /** Local authorized SDK path, relative to the input configuration file. */
    public String sdkJar;
    /** Local standalone consumer POM, with resolved dependency versions and no platform parent. */
    public String sdkPom;
    /** card or non-card; controls offered and accepted capabilities. */
    public String paymentType;
    /** none, one or two; two requires authenticateAuthorize. */
    public String threeDS = "none";
    /** Selected SDK method names; optional methods are omitted rather than returning null. */
    public List<String> spi = new ArrayList<>();
    /** Simple generation choices; selected features produce demonstration hooks, not a protocol implementation. */
    public SecurityFeatures securityFeatures;
    /** Advanced or normalized contract: SPI method -> message direction -> ordered security steps. */
    public Map<String, Map<String, List<SecurityStep>>> security = new LinkedHashMap<>();

    /** Both choices are required when this compact input is supplied; no algorithm or key is inferred. */
    public static final class SecurityFeatures {
        /** Generate signing hooks for requests and verification hooks for responses/notifications. */
        public Boolean signature;
        /** Generate encryption hooks for requests and decryption hooks for responses/notifications. */
        public Boolean encryption;
    }

    /** One protocol step, in wire-protocol order; rule is a documentation reference. */
    public static final class SecurityStep {
        /** sign/encrypt for outbound messages, verify/decrypt for incoming messages. */
        public String operation;
        /** none, platform calculation, adapter custom calculation, or an unfinished platform demonstration. */
        public String implementation;
        /** Platform signing or encryption enum name, depending on operation. */
        public String algorithm;
        /** Storage algorithm used by queryKey in adapter calculation mode only. */
        public String keyAlgorithm;
        /** Non-secret reference to the confirmed protocol rule or no-operation rationale. */
        public String rule;
        /** SYMMETRIC or ASYMMETRIC for platform encryption/decryption only. */
        public String cipherType;
        /** Static mode, padding and optional tag length; dynamic nonces stay in protocol hooks. */
        public Map<String, Object> parameters = new LinkedHashMap<>();
    }
}
