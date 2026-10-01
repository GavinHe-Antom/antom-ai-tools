/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.ais.cli;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import org.jline.terminal.Terminal;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

/** AIS project generation and local delivery commands. */
@Command(name = "ais", mixinStandardHelpOptions = true, version = "ais 0.1.0 (SDK 1.5.2)",
        subcommands = {AisCli.Init.class, AisCli.Package.class})
public final class AisCli implements Runnable {
    @Spec
    private CommandSpec command;

    /** Show usage when no operation was selected. */
    @Override
    public void run() {
        command.commandLine().usage(command.commandLine().getOut());
    }

    /** Run the CLI; diagnostics never mix into structured stdout. @param args command-line arguments */
    public static void main(String[] args) {
        System.exit(commandLine().execute(args));
    }

    static CommandLine commandLine() {
        CommandLine cli = new CommandLine(new AisCli());
        cli.setExpandAtFiles(false);
        cli.setParameterExceptionHandler((failure, arguments) -> {
            failure.getCommandLine().getErr().println(failure.getMessage());
            if (Arrays.asList(arguments).contains("--json")) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("status", "failed");
                result.put("exitCode", 2);
                result.put("error", failure.getMessage());
                failure.getCommandLine().getOut().println(JsonFiles.MAPPER.writeValueAsString(result));
            }
            return 2;
        });
        cli.setExecutionExceptionHandler((failure, command, parsed) -> {
            int code = 2;
            if (failure instanceof GenerationException) {
                code = ((GenerationException) failure).exitCode;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", "failed");
            result.put("exitCode", code);
            result.put("error", failure.getMessage());
            // The error boundary retains the cause in stderr without dumping configuration contents.
            command.getErr().println(failure.getClass().getSimpleName() + ": " + failure.getMessage());
            while (parsed.hasSubcommand()) {
                parsed = parsed.subcommand();
            }
            if (parsed.hasMatchedOption("--json")) {
                command.getOut().println(JsonFiles.MAPPER.writeValueAsString(result));
                command.getOut().flush();
            }
            return code;
        });
        return cli;
    }

    /** Interactive and machine-readable project initialization. */
    @Command(name = "init", mixinStandardHelpOptions = true, description = "Generate a new Adapter project")
    static final class Init implements Callable<Integer> {
        @Option(names = "--config", description = "Strict adapter-spec.json; paths relative to this file")
        private Path config;
        @Option(names = "--output", description = "New or empty project directory")
        private Path output;
        @Option(names = "--dry-run", description = "Validate and list files without writing")
        private boolean dryRun;
        @Option(names = "--json", description = "Write one result object to stdout")
        private boolean json;
        @Spec
        private CommandSpec command;

        @Override
        public Integer call() throws Exception {
            AdapterSpec spec;
            Path base = Paths.get("").toAbsolutePath();
            if (config == null) {
                try (Terminal terminal = TerminalMenu.open()) {
                    InteractivePrompts prompts = new InteractivePrompts(terminal);
                    spec = prompts.read();
                    if (output == null) {
                        output = Paths.get(prompts.ask("Output directory", "./" + spec.artifactId));
                    }
                }
            } else {
                spec = JsonFiles.read(config);
                base = config.toAbsolutePath().getParent();
                if (output == null) {
                    throw new GenerationException(2, "--output is required with --config");
                }
            }
            SdkDefaults.apply(spec);
            Map<String, Object> result = new ProjectGenerator().generate(spec, base, output, dryRun);
            print(command.commandLine().getOut(), result, json);
            return 0;
        }
    }

    /** Package a trusted local project; does not upload or register platform capabilities. */
    @Command(name = "package", mixinStandardHelpOptions = true, description = "Test and inspect a trusted local Adapter")
    static final class Package implements Callable<Integer> {
        @Option(names = "--project", defaultValue = ".", description = "Project directory")
        private Path project;
        @Option(names = "--offline", description = "Use already cached Maven dependencies")
        private boolean offline;
        @Option(names = "--json", description = "Write one result object to stdout")
        private boolean json;
        @Spec
        private CommandSpec command;

        @Override
        public Integer call() throws Exception {
            command.commandLine().getErr().println("Building trusted local project; Maven output is retained in .ais-build-*.log.");
            Map<String, Object> result = new AdapterPackager().pack(project, offline);
            print(command.commandLine().getOut(), result, json);
            return 0;
        }
    }

    private static void print(PrintWriter output, Map<String, Object> result, boolean json) throws Exception {
        if (json) {
            output.println(JsonFiles.MAPPER.writeValueAsString(result));
        } else {
            output.println(JsonFiles.MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result));
        }
        output.flush();
    }
}
