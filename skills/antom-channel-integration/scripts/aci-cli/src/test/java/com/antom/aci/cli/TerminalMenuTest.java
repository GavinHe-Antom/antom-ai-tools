/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Keyboard and terminal lifecycle regression tests without a user's physical terminal. */
@Timeout(10)
class TerminalMenuTest {
    /** ANSI and application-mode arrows move immediately; Enter confirms the highlighted value. */
    @Test
    void movesUpAndDownWithoutTypedIndexes() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Terminal terminal = terminal("\033[B\033OB\033[A\r", output)) {
            Attributes original = terminal.getAttributes();
            assertEquals("second", new TerminalMenu(terminal).choose("Payment type", "first", "second", "third"));
            assertEquals(original.toString(), terminal.getAttributes().toString());
            assertTrue(output.toString("UTF-8").contains("Payment type: second"));
        }
    }

    /** Numeric and textual input is ignored; there is no legacy typed-choice fallback. */
    @Test
    void ignoresTypedChoicesAndClampsAtListEdges() throws Exception {
        try (Terminal terminal = terminal("2third\033[A\r\033[B\033[B\033[B\r", new ByteArrayOutputStream())) {
            TerminalMenu menu = new TerminalMenu(terminal);
            assertEquals("first", menu.choose("Default", "first", "second"));
            assertEquals("second", menu.choose("Last", "first", "second"));
        }
    }

    /** Menus scroll without requiring the whole catalogue to fit on the screen. */
    @Test
    void selectsBeyondViewportOnSmallTerminal() throws Exception {
        StringBuilder keys = new StringBuilder();
        String[] choices = new String[48];
        for (int index = 0; index < choices.length; index++) {
            choices[index] = "algorithm-" + index;
            keys.append("\033[B");
        }
        keys.append("\r");
        try (Terminal terminal = terminal(keys.toString(), new ByteArrayOutputStream())) {
            terminal.setSize(new Size(40, 8));
            assertEquals("algorithm-47", new TerminalMenu(terminal).choose("Algorithm", choices));
        }
    }

    /** Ctrl+C and Ctrl+D stop the wizard and restore terminal attributes. */
    @Test
    void restoresTerminalOnCancellationAndEof() throws Exception {
        for (String key : new String[]{"\003", "\004"}) {
            try (Terminal terminal = terminal(key, new ByteArrayOutputStream())) {
                Attributes original = terminal.getAttributes();
                GenerationException failure = assertThrows(GenerationException.class,
                        () -> new TerminalMenu(terminal).choose("Cancel", "first"));
                int expected = 2;
                if ("\003".equals(key)) {
                    expected = 130;
                }
                assertEquals(expected, failure.exitCode);
                assertEquals(original.toString(), terminal.getAttributes().toString());
            }
        }
    }

    /** Text fields also support clean cancellation without producing a partial specification. */
    @Test
    void cancelsTextInput() throws Exception {
        try (Terminal terminal = terminal("\003", new ByteArrayOutputStream())) {
            assertEquals(130, assertThrows(GenerationException.class,
                    () -> new InteractivePrompts(terminal).ask("Company/institution code", null)).exitCode);
        }
    }

    /** Use real JLine key decoding over in-memory terminal streams. */
    static Terminal terminal(String input, ByteArrayOutputStream output) throws Exception {
        Terminal terminal = new DumbTerminal("test", "xterm",
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), output, StandardCharsets.UTF_8);
        terminal.setSize(new Size(100, 24));
        return terminal;
    }
}
