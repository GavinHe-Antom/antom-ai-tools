/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Inert evidence fixtures verify coverage accounting, not the institution protocol. */
class DeliveryEvidenceTest {
    @TempDir Path temporary;
    private static final String TEST_CLASS = "com.example.adapter.PayDeliveryTest";

    @Test
    void reportsEveryExecutedCaseAndItsIndependentFixtureHash() throws Exception {
        fixture("success", "success", true);
        fixture("rejected", "business-failure", true);
        Map<String, Object> report = inspect("case=success", "case=rejected");
        Map<?, ?> pay = (Map<?, ?>) report.get("pay");
        List<?> cases = (List<?>) pay.get("cases");
        assertEquals(2, cases.size());
        assertEquals(new LinkedHashSet<>(Arrays.asList("business-failure", "success")), pay.get("categoriesCovered"));
        Map<?, ?> first = (Map<?, ?>) cases.get(0);
        assertEquals("rejected", first.get("id"));
        assertEquals(SdkBundle.hash(path("rejected"), "SHA-256"), first.get("fixtureSha256"));
        assertEquals("passed", first.get("status"));
        assertTrue(pay.get("securityBoundary").toString().contains("algorithm proof"));
    }

    @Test
    void rejectsADeclaredCaseThatDidNotExecuteEvenWhenAnotherCasePassed() throws Exception {
        fixture("success", "success", true);
        fixture("rejected", "business-failure", true);
        GenerationException error = assertThrows(GenerationException.class, () -> inspect("case=success"));
        assertTrue(error.getMessage().contains("case=rejected"));
        assertEquals(6, error.exitCode);
    }

    @Test
    void ordinaryMethodNameIsNotEvidenceForAConfiguredScenario() throws Exception {
        fixture("success", "success", true);
        assertThrows(GenerationException.class, () -> inspect("selectedSpiMatchesConfirmedProtocol[1]"));
    }

    @Test
    void rejectsEmptyOrUnconfirmedProtocolFixtures() throws Exception {
        assertThrows(GenerationException.class, () -> inspect("case=success"));
        fixture("success", "success", false);
        GenerationException error = assertThrows(GenerationException.class, () -> inspect("case=success"));
        assertTrue(error.getMessage().contains("Unconfirmed"));
    }

    @Test
    void rejectsMismatchedFixtureIdentityAndMissingCategory() throws Exception {
        fixture("success", "success", true);
        Files.move(path("success"), path("other"));
        assertThrows(GenerationException.class, () -> inspect("case=success"));
        Files.move(path("other"), path("success"));
        Files.write(path("success"), "{\"id\":\"success\",\"confirmed\":true}".getBytes(StandardCharsets.UTF_8));
        assertThrows(GenerationException.class, () -> inspect("case=success"));
    }

    @Test
    void changedFixtureProducesDifferentEvidenceHash() throws Exception {
        fixture("success", "success", true);
        Map<String, Object> before = inspect("case=success");
        Files.write(path("success"), ("{\"id\":\"success\",\"category\":\"success\",\"confirmed\":true,"
                + "\"expectedResult\":{},\"input\":{\"amount\":2}}").getBytes(StandardCharsets.UTF_8));
        Map<String, Object> after = inspect("case=success");
        assertNotEquals(before, after);
    }

    @Test
    void hashesReferencedRawTextAndRejectsTraversalOrMissingText() throws Exception {
        fixture("success", "success", true);
        Files.write(path("success"), ("{\"id\":\"success\",\"category\":\"success\",\"confirmed\":true,"
                + "\"expectedResult\":{},\"expectedRequest\":{\"bodyFile\":\"raw.txt\"}}").getBytes(StandardCharsets.UTF_8));
        assertThrows(GenerationException.class, () -> inspect("case=success"));
        Path raw = path("success").getParent().resolve("raw.txt");
        Files.write(raw, "first\n".getBytes(StandardCharsets.UTF_8));
        Map<String, Object> before = inspect("case=success");
        Files.write(raw, "second\n".getBytes(StandardCharsets.UTF_8));
        assertNotEquals(before, inspect("case=success"));
        Files.write(path("success"), ("{\"id\":\"success\",\"category\":\"success\",\"confirmed\":true,"
                + "\"expectedResult\":{},\"http\":{\"response\":{\"bodyFile\":\"../raw.txt\"}}}").getBytes(StandardCharsets.UTF_8));
        assertThrows(GenerationException.class, () -> inspect("case=success"));
    }

    @Test
    void exceptionOnlyCoverageCannotClaimWorkingSpiMappings() throws Exception {
        Path file = path("invalid");
        Files.createDirectories(file.getParent());
        Files.write(file, ("{\"id\":\"invalid\",\"category\":\"validation-failure\",\"confirmed\":true,"
                + "\"expectedException\":{\"type\":\"IllegalArgumentException\",\"message\":\"missing input\"}}")
                .getBytes(StandardCharsets.UTF_8));
        GenerationException error = assertThrows(GenerationException.class, () -> inspect("case=invalid"));
        assertTrue(error.getMessage().contains("exception-only"));
        Files.write(file, ("{\"id\":\"invalid\",\"category\":\"success\",\"confirmed\":true,"
                + "\"expectedException\":{\"type\":\"IllegalArgumentException\",\"message\":\"missing input\"}}")
                .getBytes(StandardCharsets.UTF_8));
        error = assertThrows(GenerationException.class, () -> inspect("case=invalid"));
        assertTrue(error.getMessage().contains("expectedResult"));
    }

    private Map<String, Object> inspect(String... names) throws Exception {
        Map<String, Set<String>> executed = new LinkedHashMap<>();
        Set<String> identities = new LinkedHashSet<>();
        for (String name : names) {
            if (name.startsWith("case=")) {
                identities.add("selectedSpiMatchesConfirmedProtocol " + name);
            } else {
                identities.add(name);
            }
        }
        executed.put(TEST_CLASS, identities);
        return DeliveryEvidence.inspect(temporary, "com.example.adapter", Collections.singletonList(Capability.PAY), executed);
    }

    private Path path(String id) {
        return temporary.resolve("src/test/resources/scenarios/pay/" + id + ".json");
    }

    private void fixture(String id, String category, boolean confirmed) throws Exception {
        Files.createDirectories(path(id).getParent());
        Files.write(path(id), ("{\"id\":\"" + id + "\",\"category\":\"" + category
                + "\",\"confirmed\":" + confirmed + ",\"expectedResult\":{}}").getBytes(StandardCharsets.UTF_8));
    }
}
