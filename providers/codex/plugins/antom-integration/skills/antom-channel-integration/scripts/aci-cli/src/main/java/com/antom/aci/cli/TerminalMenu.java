/* SPDX-License-Identifier: Apache-2.0 */
package com.antom.aci.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.Display;
import org.jline.utils.InfoCmp.Capability;

/** A small arrow-key selector; JLine owns terminal detection, key decoding and screen updates. */
final class TerminalMenu {
    private enum Action { UP, DOWN, CONFIRM, CANCEL, EOF, IGNORE }

    private final Terminal terminal;
    private final BindingReader input;
    private final KeyMap<Action> keys;

    TerminalMenu(Terminal terminal) {
        this.terminal = terminal;
        input = new BindingReader(terminal.reader());
        keys = new KeyMap<>();
        keys.setNomatch(Action.IGNORE);
        keys.setUnicode(Action.IGNORE);
        keys.bind(Action.UP, "\033[A", "\033OA", KeyMap.key(terminal, Capability.key_up));
        keys.bind(Action.DOWN, "\033[B", "\033OB", KeyMap.key(terminal, Capability.key_down));
        keys.bind(Action.CONFIRM, "\r", "\n");
        keys.bind(Action.CANCEL, KeyMap.ctrl('C'));
        keys.bind(Action.EOF, KeyMap.ctrl('D'));
    }

    /** Require a real input/stderr terminal; automation must use --config instead. */
    static Terminal open() throws IOException {
        try {
            Terminal terminal = TerminalBuilder.builder().name("aci").system(true)
                    .systemOutput(TerminalBuilder.SystemOutput.SysErr).provider("jni").dumb(false).build();
            if (terminal.getType().startsWith("dumb")) {
                terminal.close();
                throw new GenerationException(2, "Arrow menus require an interactive terminal; use --config for automation");
            }
            return terminal;
        } catch (IllegalStateException failure) {
            throw new GenerationException(2, "Cannot open interactive terminal; run aci init in a terminal or use --config");
        }
    }

    /** Select without typed indexes; always restore terminal settings on return, cancel or failure. */
    String choose(String prompt, String... choices) {
        if (choices.length == 0) {
            throw new IllegalArgumentException("A menu requires at least one choice");
        }
        Attributes original = terminal.enterRawMode();
        Display display = new Display(terminal, false);
        int selected = 0;
        try {
            // JLine raw mode preserves ISIG; read Ctrl+C as a key so cancellation reaches finally.
            Attributes raw = terminal.getAttributes();
            raw.setLocalFlag(Attributes.LocalFlag.ISIG, false);
            terminal.setAttributes(raw);
            while (true) {
                render(display, prompt, choices, selected);
                Action action = input.readBinding(keys);
                if (action == null || action == Action.EOF) {
                    throw new GenerationException(2, "Input ended before configuration was complete; no project generated");
                }
                if (action == Action.CANCEL) {
                    throw new GenerationException(130, "Initialization cancelled; no project generated");
                }
                if (action == Action.CONFIRM) {
                    display.update(Collections.emptyList(), 0);
                    terminal.writer().println(prompt + ": " + choices[selected]);
                    return choices[selected];
                }
                if (action == Action.UP && selected > 0) {
                    selected--;
                } else if (action == Action.DOWN && selected < choices.length - 1) {
                    selected++;
                }
            }
        } finally {
            terminal.setAttributes(original);
            terminal.flush();
        }
    }

    /** Keep long catalogues within a viewport; recompute dimensions after each key for resize safety. */
    private void render(Display display, String prompt, String[] choices, int selected) {
        int rows = terminal.getHeight();
        int columns = terminal.getWidth();
        if (rows <= 0) {
            rows = 24;
        }
        if (columns <= 0) {
            columns = 80;
        }
        int visible = Math.max(1, Math.min(10, rows - 4));
        int first = Math.max(0, selected - visible + 1);
        int last = Math.min(choices.length, first + visible);
        List<AttributedString> lines = new ArrayList<>();
        lines.add(new AttributedString(prompt));
        for (int index = first; index < last; index++) {
            if (index == selected) {
                lines.add(new AttributedString(" > " + choices[index], AttributedStyle.INVERSE));
            } else {
                lines.add(new AttributedString("   " + choices[index]));
            }
        }
        lines.add(new AttributedString("Up/Down: select | Enter: confirm | Ctrl+C: cancel ("
                + (selected + 1) + "/" + choices.length + ")"));
        List<AttributedString> fitted = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            AttributedStringBuilder line = new AttributedStringBuilder()
                    .append(lines.get(index).columnSubSequence(0, Math.max(1, columns - 1)));
            if (index < lines.size() - 1) {
                line.append('\n');
            }
            fitted.add(line.toAttributedString());
        }
        display.resize(rows, columns);
        display.update(fitted, -1);
        terminal.flush();
    }
}
