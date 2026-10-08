/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Test reports are inert fixtures; no Maven process or institution protocol executes. */
class AdapterPackagerTest {
    private static final String PACKAGE = "com.example.adapter";
    private static final String STRUCTURE = PACKAGE + ".GeneratedStructureTest";
    private static final String DELIVERY = PACKAGE + ".PayDeliveryTest";
    private static final String SECURITY = PACKAGE + ".PaySecurityContractTest";
    @TempDir Path temporary;

    @Test
    void acceptsCompleteStructureDeliveryAndSecuritySuites() throws Exception {
        completeReports();
        assertDoesNotThrow(this::checkTests);
    }

    @Test
    void requiresStructureAndEachSelectedSecuritySuiteEvenWhenDeliveryPassed() throws Exception {
        for (String missing : new String[]{STRUCTURE, SECURITY}) {
            completeReports();
            Files.delete(reportPath(missing));
            GenerationException failure = assertThrows(GenerationException.class, this::checkTests);
            assertEquals(6, failure.exitCode);
            assertTrue(failure.getMessage().contains(missing));
        }
    }

    @Test
    void rejectsZeroCaseRequiredSuites() throws Exception {
        for (String empty : new String[]{STRUCTURE, SECURITY, DELIVERY}) {
            completeReports();
            report(empty, "tests=\"0\" failures=\"0\" errors=\"0\" skipped=\"0\"", "");
            GenerationException failure = assertThrows(GenerationException.class, this::checkTests);
            assertEquals(6, failure.exitCode);
            assertTrue(failure.getMessage().contains(empty));
        }
    }

    @Test
    void rejectsSkippedStructureAndSecuritySuites() throws Exception {
        for (String skipped : new String[]{STRUCTURE, SECURITY}) {
            completeReports();
            report(skipped, "tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"1\"",
                    testCase(skipped, "<skipped/>"));
            assertEquals(6, assertThrows(GenerationException.class, this::checkTests).exitCode);
        }
    }

    @Test
    void requiresBothTestClassesForEverySelectedMethod() throws Exception {
        completeReports();
        String notification = PACKAGE + ".NotifyPaymentDeliveryTest";
        passingReport(notification);
        GenerationException failure = assertThrows(GenerationException.class,
                () -> new AdapterPackager().checkTests(temporary, PACKAGE,
                        Arrays.asList(Capability.PAY, Capability.NOTIFY_PAYMENT)));
        assertTrue(failure.getMessage().contains("NotifyPaymentSecurityContractTest"));
        passingReport(PACKAGE + ".NotifyPaymentSecurityContractTest");
        assertDoesNotThrow(() -> new AdapterPackager().checkTests(temporary, PACKAGE,
                Arrays.asList(Capability.PAY, Capability.NOTIFY_PAYMENT)));
    }

    @Test
    void usesExecutedCaseClassesInsteadOfReportFileOrSuiteNames() throws Exception {
        completeReports();
        report(SECURITY, successCounts(), testCase(PACKAGE + ".UnrelatedTest", ""));
        GenerationException failure = assertThrows(GenerationException.class, this::checkTests);
        assertTrue(failure.getMessage().contains(SECURITY));
        report(SECURITY, successCounts(), testCase("other.package.PaySecurityContractTest", ""));
        assertThrows(GenerationException.class, this::checkTests);
        Files.delete(reportPath(SECURITY));
        report(PACKAGE + ".CombinedSuite", successCounts(), testCase(SECURITY, ""));
        assertDoesNotThrow(this::checkTests);
    }

    @Test
    void rejectsActualFailedOrSkippedCasesEvenWithZeroSummaryCounters() throws Exception {
        for (String outcome : new String[]{"failure", "error", "skipped"}) {
            completeReports();
            report(SECURITY, successCounts(), testCase(SECURITY, "<" + outcome + "/>"));
            assertEquals(6, assertThrows(GenerationException.class, this::checkTests).exitCode);
        }
    }

    @Test
    void rejectsFailuresOrSkipsInAnySuiteNotJustRequiredClasses() throws Exception {
        completeReports();
        String unrelated = PACKAGE + ".ExtraTest";
        for (String field : new String[]{"failures", "errors", "skipped"}) {
            String counts = successCounts().replace(field + "=\"0\"", field + "=\"1\"");
            report(unrelated, counts, testCase(unrelated, ""));
            assertEquals(6, assertThrows(GenerationException.class, this::checkTests).exitCode);
        }
    }

    @Test
    void rejectsMalformedOrInconsistentExecutionCounters() throws Exception {
        for (String counts : new String[]{successCounts().replace("tests=\"1\"", "tests=\"0\""),
                successCounts().replace("tests=\"1\"", "tests=\"invalid\""),
                successCounts().replace("skipped=\"0\"", "")}) {
            completeReports();
            report(SECURITY, counts, testCase(SECURITY, ""));
            assertEquals(6, assertThrows(GenerationException.class, this::checkTests).exitCode);
        }
    }

    @Test
    void rejectsMissingExecutionReports() {
        assertEquals(6, assertThrows(GenerationException.class, this::checkTests).exitCode);
    }

    private void checkTests() throws Exception {
        new AdapterPackager().checkTests(temporary, PACKAGE, Arrays.asList(Capability.PAY));
    }

    private void completeReports() throws Exception {
        for (String name : new String[]{STRUCTURE, DELIVERY, SECURITY}) {
            passingReport(name);
        }
    }

    private void passingReport(String name) throws Exception {
        report(name, successCounts(), testCase(name, ""));
    }

    private String successCounts() {
        return "tests=\"1\" failures=\"0\" errors=\"0\" skipped=\"0\"";
    }

    private String testCase(String className, String outcome) {
        return "<testcase name=\"executedContract\" classname=\"" + className + "\">" + outcome + "</testcase>";
    }

    private Path reportPath(String name) {
        return temporary.resolve("target/surefire-reports/TEST-" + name + ".xml");
    }

    private void report(String name, String counts, String cases) throws Exception {
        Path file = reportPath(name);
        Files.createDirectories(file.getParent());
        String xml = "<testsuite name=\"" + name + "\" " + counts + ">" + cases + "</testsuite>";
        Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
    }
}
