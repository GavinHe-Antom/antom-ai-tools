/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;

/** Human prompts collect the same explicit contract accepted by --config. */
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
        spec.groupId = ask("Maven groupId", "com.example.channel");
        spec.artifactId = ask("Maven artifactId", "my-channel-adapter");
        spec.packageName = ask("Java package", "com.example.channel.adapter");
        spec.version = ask("Adapter version", "1.0.0-SNAPSHOT");
        spec.channelCode = ask("Platform channelCode", null);
        output.println("SDK 1.5.2: using the CLI's bundled JAR and consumer POM (no path input required).");
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
        if (select(spec, "refund", "Implement refund?")) {
            select(spec, "inquiryRefund", "Implement refund inquiry?");
        }
        if (select(spec, "notifyPayment", "Implement payment notification?")) {
            if ("card".equals(spec.paymentType)) {
                select(spec, "notifyCapture", "Implement capture notification?");
            }
            select(spec, "notifyRefund", "Implement refund notification?");
        } else {
            output.println("Other notifications require notifyPayment in this SDK; no notification Bean will be generated.");
        }
        output.println("Security: configure each method/direction explicitly. Do not enter secrets.");
        for (String method : spec.spi) {
            Map<String, List<AdapterSpec.SecurityStep>> directions = new LinkedHashMap<>();
            for (String direction : Capability.find(method).getDirections()) {
                List<String> operations = Arrays.asList("verify", "decrypt");
                if ("request".equals(direction)) {
                    operations = Arrays.asList("sign", "encrypt");
                }
                List<AdapterSpec.SecurityStep> steps = new ArrayList<>();
                String order = choose(method + "/" + direction + " operation order",
                        operations.get(0) + "," + operations.get(1), operations.get(1) + "," + operations.get(0));
                for (String operation : order.split(",")) {
                    steps.add(readStep(method, direction, operation));
                }
                directions.put(direction, steps);
            }
            spec.security.put(method, directions);
        }
        return spec;
    }

    private AdapterSpec.SecurityStep readStep(String method, String direction, String operation) throws IOException {
        AdapterSpec.SecurityStep step = new AdapterSpec.SecurityStep();
        step.operation = operation;
        step.implementation = choose(method + "/" + direction + "/" + operation, "none", "platform", "adapter");
        step.rule = ask("Confirmed rule reference (or reason for none, ASCII)", null);
        if ("adapter".equals(step.implementation)) {
            step.keyAlgorithm = chooseCatalogue("Key algorithm (queryKey purpose follows operation)", "key");
        }
        if ("platform".equals(step.implementation)) {
            String catalogue = "sign";
            if ("encrypt".equals(operation) || "decrypt".equals(operation)) {
                catalogue = "cipher";
            }
            step.algorithm = chooseCatalogue("SDK " + catalogue + " algorithm", catalogue);
            if ("encrypt".equals(operation) || "decrypt".equals(operation)) {
                if (CipherCompatibility.isSymmetric(step.algorithm)) {
                    step.cipherType = choose("Cipher type", "SYMMETRIC");
                } else {
                    step.cipherType = choose("Cipher type", "ASYMMETRIC");
                }
                List<String> modes = CipherCompatibility.modes(step.algorithm);
                if (!modes.isEmpty()) {
                    String mode = choose("Cipher mode", modes.toArray(new String[0]));
                    step.parameters.put("mode", mode);
                    List<String> paddings = CipherCompatibility.paddings(step.algorithm, mode);
                    step.parameters.put("padding", choose("Cipher padding", paddings.toArray(new String[0])));
                    if ("GCM".equals(mode)) {
                        step.parameters.put("tagBitLength", Integer.parseInt(
                                choose("GCM tag bits", "128", "120", "112", "104", "96")));
                    }
                    output.println("Derive IV/AAD from the protocol in the generated request hook; do not freeze them in CLI config.");
                }
            }
        }
        return step;
    }

    private boolean select(AdapterSpec spec, String method, String prompt) throws IOException {
        if ("y".equals(choose(prompt, "n", "y"))) {
            spec.spi.add(method);
            return true;
        }
        return false;
    }

    /** Read algorithm options from the same SDK catalogue used by the validator. */
    private String chooseCatalogue(String prompt, String catalogue) throws IOException {
        List<String> values = SpecValidator.catalogue(catalogue);
        return choose(prompt, values.toArray(new String[0]));
    }

    /** All fixed choices use arrow keys, including algorithms and yes/no questions. */
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
