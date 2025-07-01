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

import dev.terminalmc.signedit.SignEdit;
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

    private final Supplier<String[]> getLinesFn;
    private final Supplier<String> getMessageFn;
    private final Supplier<Integer> getMaxWidthFn;
    private final int lineCount;
    public static @Nullable String text;

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
//                    unwrap(getMessage.get());
                    if (text == null) {
                        String[] arr = getMessage.get();
                        String str = unwrap(arr);
                        SignEdit.LOG.warn("unwrapped {} into {}", arrToStr(arr), escaped(str));
                        text = str;
                    }
                    return text;
                }, (str) -> {
//                    setMessage.accept(wrap(str, getMaxWidth.get()))
                    text = str;
                    String[] arr = wrap(str, getMaxWidth.get(), lineCount);
                    setMessage.accept(arr);
                    SignEdit.LOG.warn("wrapped {} into {}", escaped(str), arrToStr(arr));
                }, getClipboard, setClipboard, (str) -> fits(str, getMaxWidth.get(), lineCount)
        );
        text = null;
        this.getLinesFn = getMessage;
        this.getMessageFn = () -> {
            if (text == null) {
                text = unwrap(getMessage.get());
            }
            return text;
        };
        this.getMaxWidthFn = getMaxWidth;
        this.lineCount = lineCount;
        this.setCursorToEnd();
    }

    // this is a long line wit

    public boolean newLineBefore(int line) {
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

    public LinePoint linePoint(int targetIdx) {
        String text = getMessageFn.get();
        int maxWidth = getMaxWidthFn.get();
        return linePoint(text, wrap(text, maxWidth, lineCount), targetIdx);
    }

    public static LinePoint linePoint(String text, String[] lines, int targetIdx) {
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
                } else if (line > 0
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


    public static String unwrap(String[] lines) {
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            builder.append(line);
            builder.append("\n");
        }
        String str = builder.toString();
        if (!str.isEmpty()) {
            while (str.endsWith("\n")) {
                str = str.substring(0, str.length() - 1);
            }
        }
        return str;
    }

    public static boolean fits(String input, int maxWidth, int lineCount) {
        List<String> lines = new ArrayList<>();
//        String[] rawLines = input.split("\n");
        String[] rawLines = input.split("\n", Integer.MAX_VALUE);

        for (String rawLine : rawLines) {
            wrapLine(rawLine, maxWidth, lines);
        }

        return lines.size() <= lineCount;
    }

    public static String[] wrap(String input, int maxWidth, int lineCount) {
        List<String> lines = new ArrayList<>();
//        String[] rawLines = input.split("\n");
        String[] rawLines = input.split("\n", Integer.MAX_VALUE);

        for (String rawLine : rawLines) {
            wrapLine(rawLine, maxWidth, lines);
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

    private static void wrapLine(String line, int maxWidth, List<String> outputLines) {
        if (line.isEmpty()) {
            outputLines.add(line);
            return;
        }
        int start = 0;
        while (start < line.length()) {
            int end = findBreakPoint(line, start, maxWidth);
            outputLines.add(line.substring(start, end));
            start = end;
        }
    }

    private static int findBreakPoint(String line, int start, int maxWidth) {
        int end = start;
        int lastGoodBreak = -1;

        while (end < line.length()) {
            String substr = line.substring(start, end + 1);

            if (getWidth(substr) > maxWidth)
                break;

            char c = line.charAt(end);
            if (Character.isWhitespace(c) || c == '-') {
                lastGoodBreak = end + 1;
            }

            end++;
        }

        if (end == line.length())
            return end; // End of line fits

        if (lastGoodBreak != -1 && lastGoodBreak > start) {
            return lastGoodBreak; // Prefer break at whitespace or hyphen
        } else if (end > start) {
            return end; // Fallback to breaking at current point
        } else {
            // Very long single character (e.g. emoji or CJK char); force one char
            return start + 1;
        }
    }

    private static int getWidth(String str) {
        return Minecraft.getInstance().font.width(str);
    }

    @Override
    public boolean charTyped(char character) {
        return super.charTyped(character);
    }

    @Override
    public boolean keyPressed(int key) {
        if (Screen.isSelectAll(key)) {
            this.selectAll();
            return true;
        } else if (Screen.isCopy(key)) {
            this.copy();
            return true;
        } else if (Screen.isPaste(key)) {
            this.paste();
            return true;
        } else if (Screen.isCut(key)) {
            this.cut();
            return true;
        } else {
            CursorStep textfieldhelper$cursorstep =
                    Screen.hasControlDown() ? CursorStep.WORD : CursorStep.CHARACTER;
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                this.removeFromCursor(-1, textfieldhelper$cursorstep);
                return true;
            } else {
                if (key == GLFW.GLFW_KEY_DELETE) {
                    this.removeFromCursor(1, textfieldhelper$cursorstep);
                } else {
                    if (key == GLFW.GLFW_KEY_LEFT) {
                        this.moveBy(-1, Screen.hasShiftDown(), textfieldhelper$cursorstep);
                        return true;
                    }

                    if (key == GLFW.GLFW_KEY_RIGHT) {
                        this.moveBy(1, Screen.hasShiftDown(), textfieldhelper$cursorstep);
                        return true;
                    }

                    if (key == GLFW.GLFW_KEY_HOME) {
                        for (int i = getCursorPos(); i >= 0; i--) {
                            if (i > 0 && String.valueOf(text.charAt(i - 1)).equals("\n")) {
                                this.setCursorPos(i, Screen.hasShiftDown());
                                return true;
                            }
                        }
                        this.setCursorToStart(Screen.hasShiftDown());
                        return true;
                    }

                    if (key == GLFW.GLFW_KEY_END) {
                        int max = text.length();
                        for (int i = getCursorPos(); i <= max; i++) {
                            if (i == max || String.valueOf(text.charAt(i)).equals("\n")) {
                                this.setCursorPos(i, Screen.hasShiftDown());
                                return true;
                            }
                        }
                        this.setCursorToEnd(Screen.hasShiftDown());
                        return true;
                    }
                }

                return false;
            }
        }
    }

    private static String arrToStr(String[] arr) {
        StringBuilder builder = new StringBuilder();
        for (String str : arr) {
            builder.append(str);
            builder.append(", ");
        }
        String out = builder.length() > 2
                ? builder.substring(0, builder.length() - 2)
                : builder.toString();
        return escaped("[" + out + "]");
    }

    private static String escaped(String str) {
        return str.replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\f", "\\f")
                .replace("\b", "\\b");
    }
}
