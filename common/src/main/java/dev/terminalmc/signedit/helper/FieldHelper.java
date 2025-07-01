/*
 * Copyright 2025 TerminalMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.terminalmc.signedit.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FieldHelper extends TextFieldHelper {

    private final Supplier<String> getMessageFn;
    private final Supplier<Integer> getMaxWidthFn;
    private final int lineCount;

    public static @Nullable String cachedText = null;

    public FieldHelper(
            Supplier<String[]> getMessage,
            Consumer<String[]> setMessage,
            Supplier<String> getClipboard,
            Consumer<String> setClipboard,
            Supplier<Integer> getMaxWidth,
            Integer lineCount
    ) {
        super(
                () -> {
                    if (cachedText == null)
                        cachedText = unwrap(getMessage.get());
                    return cachedText;
                },
                (str) -> {
                    cachedText = str;
                    setMessage.accept(wrap(str, getMaxWidth.get(), lineCount));
                },
                getClipboard,
                setClipboard,
                (str) -> fits(str, getMaxWidth.get(), lineCount)
        );
        // Save an accessible copy of TextFieldHelper#getMessageFn
        this.getMessageFn = () -> {
            if (cachedText == null)
                cachedText = unwrap(getMessage.get());
            return cachedText;
        };
        this.getMaxWidthFn = getMaxWidth;
        this.lineCount = lineCount;
        this.setCursorToEnd();
    }

    /**
     * @return {@code true} if the previous line ends in a manual linebreak.
     */
    public boolean linebreakBefore(int line) {
        String text = getMessageFn.get();
        int maxWidth = getMaxWidthFn.get();
        if (line <= 0 || line >= lineCount)
            return false;
        String[] lines = wrap(text, maxWidth, lineCount);
        for (int i = 0; i <= text.length(); i++) {
            if (linePoint(text, lines, i).line() == line) {
                return i > 0 && String.valueOf(text.charAt(i - 1)).equals("\n");
            }
        }
        return false;
    }

    /**
     * Shifts the cursor to the end of the line.
     */
    public void cursorToLine(int line) {
        String text = getMessageFn.get();
        int maxWidth = getMaxWidthFn.get();
        String[] lines = wrap(text, maxWidth, lineCount);
        LinePoint primaryLp = new LinePoint(line, lines[line].length());
        for (int i = 0; i <= text.length(); i++) {
            if (linePoint(text, lines, i).equals(primaryLp)) {
                setCursorPos(i, Screen.hasShiftDown());
            }
        }
    }

    /**
     * Converts an index position in the backend string into a line-and-index position in the
     * frontend lines.
     *
     * @param targetIdx the target index in {@link FieldHelper#cachedText}, in the range
     *                  {@code 0-}{@link FieldHelper#cachedText}{@code .length()} inclusive.
     * @return the {@link LinePoint} corresponding to the target index.
     */
    public LinePoint linePoint(int targetIdx) {
        String text = getMessageFn.get();
        int maxWidth = getMaxWidthFn.get();
        return linePoint(text, wrap(text, maxWidth, lineCount), targetIdx);
    }

    /**
     * Converts an index position in the backend string into a line-and-index position in the
     * frontend lines.
     *
     * @param text      the backend string.
     * @param lines     the frontend lines.
     * @param targetIdx the target index in {@link FieldHelper#cachedText}, in the range
     *                  {@code 0-}{@link FieldHelper#cachedText}{@code .length()} inclusive.
     * @return the {@link LinePoint} corresponding to the target index.
     */
    private static LinePoint linePoint(String text, String[] lines, int targetIdx) {
        int idx = 0;
        for (int line = 0; line < lines.length; line++) {
            for (int point = 0; point <= lines[line].length(); point++) {
                if (point == lines[line].length()
                        && text.length() >= idx
                        && point > 0
                        && idx > 0
                        && String.valueOf(text.charAt(idx - 1)).equals("\n")
                        && idx++ == targetIdx) {
                    return new LinePoint(line + 1, 0);
                } else //noinspection ConstantValue
                    if (line > 0
                        && point == 0
                        && !String.valueOf(text.charAt(idx - 1)).equals("\n")
                        && point++ > 0
                        && idx++ == targetIdx) {
                    return new LinePoint(line, point + 1);
                } else if (idx++ == targetIdx) {
                    return new LinePoint(line, point);
                }
            }
        }

        return new LinePoint(0, 0);
    }

    /**
     * Converts text from frontend lines to a backend string.
     */
    public static String unwrap(String[] lines) {
        StringBuilder builder = new StringBuilder();
        // Add manual linebreaks for all lines
        for (String line : lines) {
            builder.append(line);
            builder.append("\n");
        }
        String str = builder.toString();
        // Remove trailing newlines
        while (str.endsWith("\n")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    /**
     * @return {@code true} if the entire string can be displayed within the given parameters.
     */
    public static boolean fits(String input, int maxWidth, int lineCount) {
        // Simulate creating frontend lines, then check if they would fit
        List<String> lines = new ArrayList<>();
        for (String rawLine : input.split("\n", Integer.MAX_VALUE)) {
            addWrapped(rawLine, maxWidth, lines);
        }
        return lines.size() <= lineCount;
    }

    /**
     * Converts text from a backend string to frontend lines.
     */
    public static String[] wrap(String input, int maxWidth, int lineCount) {
        List<String> lines = new ArrayList<>();

        // Preserve manual linebreaks
        String[] rawLines = input.split("\n", Integer.MAX_VALUE);

        // Wrap overlength lines
        for (String rawLine : rawLines) {
            addWrapped(rawLine, maxWidth, lines);
        }

        // Clamp to size
        while (lines.size() > lineCount) {
            lines.removeLast();
        }
        while (lines.size() < lineCount) {
            lines.add("");
        }

        return lines.toArray(new String[0]);
    }

    /**
     * Wraps the line if it is overlength, then adds it to the list.
     */
    private static void addWrapped(String line, int maxWidth, List<String> lines) {
        if (line.isEmpty()) {
            lines.add(line);
            return;
        }
        int start = 0;
        while (start < line.length()) {
            int end = findBreakpoint(line, start, maxWidth);
            lines.add(line.substring(start, end));
            start = end;
        }
    }

    /**
     * Finds an optimal point to break the string when wrapping across multiple lines.
     *
     * @param str      the string to search.
     * @param start    the starting point for the search.
     * @param maxWidth the maximum allowable width.
     * @return the optimal breakpoint index.
     */
    private static int findBreakpoint(String str, int start, int maxWidth) {
        int end = start;
        int lastGoodBreak = -1;

        // Search the string for a good breakpoint
        while (end < str.length()) {
            if (Minecraft.getInstance().font.width(str.substring(start, end + 1)) > maxWidth)
                // Maximum width reached, stop searching
                break;

            // Save the last good breakpoint
            char c = str.charAt(end);
            if (Character.isWhitespace(c) || c == '-') {
                lastGoodBreak = end + 1;
            }

            end++;
        }

        if (end == str.length())
            // Entire string fits
            return end;

        if (lastGoodBreak != -1 && lastGoodBreak > start) {
            // Good breakpoint exists; return it
            return lastGoodBreak;
        } else if (end == start) {
            // Very long single character (e.g. emoji or CJK char); force one char
            return start + 1;
        } else {
            // No good breakpoint available; revert to maximum fit
            return end;
        }
    }

    /**
     * Provides custom key-press handling.
     */
    @Override
    public boolean keyPressed(int key) {
        if (cachedText == null)
            return false;
        if (Screen.isSelectAll(key)) {
            selectAll();
            return true;
        } else if (Screen.isCopy(key)) {
            copy();
            return true;
        } else if (Screen.isPaste(key)) {
            paste();
            return true;
        } else if (Screen.isCut(key)) {
            cut();
            return true;
        } else {
            CursorStep step = Screen.hasControlDown() ? CursorStep.WORD : CursorStep.CHARACTER;
            return switch (key) {
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    insertText("\n");
                    yield true;
                }
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    removeFromCursor(-1, step);
                    yield true;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    removeFromCursor(1, step);
                    yield true;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    moveBy(-1, Screen.hasShiftDown(), step);
                    yield true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    moveBy(1, Screen.hasShiftDown(), step);
                    yield true;
                }
                case GLFW.GLFW_KEY_HOME -> {
                    // Scan backwards to the previous linebreak, ignoring an adjacent one
                    int start = getCursorPos();
                    for (int i = start; i >= 0; i--) {
                        if (i > 0
                                && i < start
                                && String.valueOf(cachedText.charAt(i - 1)).equals("\n")) {
                            setCursorPos(i, Screen.hasShiftDown());
                            yield true;
                        }
                    }
                    // No linebreak found; jump to start
                    setCursorToStart(Screen.hasShiftDown());
                    yield true;
                }
                case GLFW.GLFW_KEY_END -> {
                    // Scan forwards to the next linebreak, ignoring an adjacent one
                    int max = cachedText.length();
                    int start = getCursorPos();
                    for (int i = start; i <= max; i++) {
                        if (i == max ||
                                (i > start && String.valueOf(cachedText.charAt(i)).equals("\n"))) {
                            setCursorPos(i, Screen.hasShiftDown());
                            yield true;
                        }
                    }
                    // No linebreak found; jump to end
                    setCursorToEnd(Screen.hasShiftDown());
                    yield true;
                }
                default -> false;
            };
        }
    }

    /**
     * Unused override.
     */
    @Override
    public boolean charTyped(char character) {
        return super.charTyped(character);
    }

    /**
     * @return a print-friendly representation of the array.
     */
    @SuppressWarnings("unused")
    private static String escaped(String[] arr) {
        StringBuilder builder = new StringBuilder();
        for (String str : arr) {
            builder.append("'").append(escaped(str)).append("', ");
        }
        // Trim trailing delimiter
        String out = builder.length() > 2
                ? builder.substring(0, builder.length() - 2)
                : builder.toString();
        return "[" + out + "]";
    }

    /**
     * @return a print-friendly representation of the string.
     */
    @SuppressWarnings("unused")
    private static String escaped(String str) {
        return str.replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\f", "\\f")
                .replace("\b", "\\b");
    }
}
