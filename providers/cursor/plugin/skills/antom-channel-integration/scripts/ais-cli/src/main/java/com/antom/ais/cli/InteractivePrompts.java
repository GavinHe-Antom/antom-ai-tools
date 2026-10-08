/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.io.IOException;
import java.io.PrintWriter;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

/** Human prompts collect project capabilities and two security feature choices. */
final class InteractivePrompts {
    private final LineReader input;
    private final PrintWriter output;
    private final TerminalMenu menu;

    InteractivePrompts(Terminal terminal) {
        input = LineReaderBuilder.builder().terminal(terminal)
                .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true).build();
        // SEND_BREAK only clears a line in this JLine release; cancellation must end the wizard.
        input.getWidgets().put("ais-cancel", () -> { throw new UserInterruptException(""); });
        input.getKeyMaps().get(LineReader.MAIN).bind(new Reference("ais-cancel"), "\003");
        output = terminal.writer();
        menu = new TerminalMenu(terminal);
    }

    AdapterSpec read() throws IOException {
        AdapterSpec spec = new AdapterSpec();
        String institutionCode;
        while (true) {
            institutionCode = ask("Company/institution code", null);
            try {
                ProjectIdentity.derive(spec, institutionCode);
                break;
            } catch (GenerationException failure) {
                output.println(failure.getMessage());
            }
        }
        output.println("SDK 1.5.2 is supplied separately by the platform; the CLI reads its local sdk/ JAR and consumer POM.");
        spec.paymentType = choose("Payment type", "card", "non-card");
        if ("card".equals(spec.paymentType)) {
            spec.threeDS = choose("3DS calls", "one", "two", "none");
        }
        spec.spi.add("pay");
        if ("two".equals(spec.threeDS)) {
            spec.spi.add("authenticateAuthorize");
        }
        select(spec, "inquiryPayment", "Implement payment inquiry?");
        select(spec, "cancel", "Implement cancellation?");
        if ("card".equals(spec.paymentType)) {
            select(spec, "capture", "Implement capture?");
        }
        if (select(spec, "refund", "Implement refund transaction?")) {
            select(spec, "inquiryRefund", "Implement refund inquiry?");
        } else {
            output.println("Refund transaction/query services will not be generated; refund notifications are selected separately.");
        }
        if (select(spec, "notifyPayment", "Implement payment notification?")) {
            if ("card".equals(spec.paymentType)) {
                select(spec, "notifyCapture", "Implement capture notification?");
            }
            select(spec, "notifyRefund", "Implement refund notification?");
        } else {
            output.println("Other notifications require notifyPayment in this SDK; no notification Bean will be generated.");
        }
        output.println("Selected SPI methods: " + String.join(", ", spec.spi));
        output.println("Notifications do not enable transaction or query services; only selected SPI methods are generated.");
        output.println("Security choices generate platform-service demonstration hooks, not an institution implementation.");
        output.println("Complete the actual protocol later using platform services or custom calculations. Do not enter secrets.");
        spec.securityFeatures = new AdapterSpec.SecurityFeatures();
        spec.securityFeatures.signature = "y".equals(choose("Need signing and verification?", "n", "y"));
        spec.securityFeatures.encryption = "y".equals(choose("Need encryption and decryption?", "n", "y"));
        output.println("Company/institution code: " + institutionCode);
        output.println("Maven coordinates: " + spec.groupId + ":" + spec.artifactId + ":" + spec.version);
        output.println("Java package: " + spec.packageName);
        output.println("Channel code: " + spec.channelCode + " (platform registration still required)");
        output.flush();
        return spec;
    }

    private boolean select(AdapterSpec spec, String method, String prompt) throws IOException {
        if ("y".equals(choose(prompt, "n", "y"))) {
            spec.spi.add(method);
            return true;
        }
        return false;
    }

    /** All fixed choices use arrow keys, including yes/no questions. */
    private String choose(String prompt, String... choices) throws IOException {
        return menu.choose(prompt, choices);
    }

    /** Free-form fields retain normal editable line input, with explicit cancellation semantics. */
    String ask(String prompt, String defaultValue) throws IOException {
        while (true) {
            String label = prompt;
            if (defaultValue != null) {
                label += " [" + defaultValue + "]";
            }
            output.flush();
            String line;
            try {
                line = input.readLine(label + ": ");
            } catch (UserInterruptException failure) {
                throw new GenerationException(130, "Initialization cancelled; no project generated");
            } catch (EndOfFileException failure) {
                throw new GenerationException(2, "Input ended before configuration was complete; no project generated");
            }
            String answer = line.trim();
            if (!answer.isEmpty()) {
                return answer;
            }
            if (defaultValue != null) {
                return defaultValue;
            }
            output.println("A value is required.");
        }
    }
}
