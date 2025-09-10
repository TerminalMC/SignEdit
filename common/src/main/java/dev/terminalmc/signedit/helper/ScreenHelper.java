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

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ScreenHelper {

    /**
     * Handles vertical line navigation using arrow keys.
     * <p>
     * All other custom actions are to be handled by {@link FieldHelper#keyPressed}.
     */
    public static boolean keyPressed(String[] lines, FieldHelper field, int line, int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_UP -> {
                // Shift cursor to end of previous line (wrapped)
                field.cursorToLine(Math.floorMod(line - 1, lines.length));
                yield true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                // Shift cursor to end of next line (wrapped)
                field.cursorToLine(Math.floorMod(line + 1, lines.length));
                yield true;
            }
            default -> false;
        };
    }

    /**
     * For each line that ends with a manual line break, renders an indicator at the end.
     */
    @SuppressWarnings("UnnecessaryUnicodeEscape")
    public static void renderLinebreaks(
            GuiGraphics graphics,
            Font font,
            FieldHelper helper,
            SignBlockEntity sign,
            SignText text,
            String[] messages
    ) {
        int color = text.hasGlowingText()
                ? text.getColor().getTextColor()
                : SignRenderer.getDarkColor(text);
        int lineHeight = sign.getTextLineHeight();
        int centerY = messages.length * sign.getTextLineHeight() / 2;

        for (int i = 1; i < messages.length; i++) {
            String str = messages[i];
            if (str == null)
                continue;
            if (helper.linebreakBefore(i)) {
                graphics.drawString(
                        font,
                        "\u21a9",
                        sign.getMaxTextLineWidth() / 2,
                        (i - 1) * lineHeight - centerY,
                        color,
                        false
                );
            }
        }
    }

    /**
     * Renders a multi-line text-selection highlight area.
     */
    public static void renderHighlight(
            GuiGraphics graphics,
            Font font,
            FieldHelper helper,
            SignBlockEntity sign,
            String[] messages
    ) {
        int idx1 = Math.min(helper.getCursorPos(), helper.getSelectionPos());
        int idx2 = Math.max(helper.getCursorPos(), helper.getSelectionPos());
        LinePoint linePoint1 = helper.linePoint(idx1);
        LinePoint linePoint2 = helper.linePoint(idx2);

        List<LinePoint> linePoints = new ArrayList<>();
        linePoints.add(linePoint1);
        for (int i = linePoint1.line(); i < linePoint2.line(); i++) {
            linePoints.add(new LinePoint(i, messages[i].length()));
            linePoints.add(new LinePoint(i + 1, 0));
        }
        linePoints.add(linePoint2);

        for (int i = 0; i < linePoints.size() - 1; i += 2) {
            LinePoint lp1 = linePoints.get(i);
            LinePoint lp2 = linePoints.get(i + 1);
            int l = lp1.line();
            String line = messages[l];
            int startIdx = lp1.point();
            int endIdx = lp2.point();

            int middle = 4 * sign.getTextLineHeight() / 2;
            int m = l * sign.getTextLineHeight() - middle;

            int s = font.width(line.substring(0, startIdx)) - font.width(line) / 2;
            int t = font.width(line.substring(0, endIdx)) - font.width(line) / 2;
            int u = Math.min(s, t);
            int v = Math.max(s, t);
            graphics.fill(
                    RenderPipelines.GUI_TEXT_HIGHLIGHT,
                    u,
                    m,
                    v,
                    m + sign.getTextLineHeight(),
                    0xFF0000FF
            );
        }
    }
}
