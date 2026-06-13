/*
 * Copyright 2026 TerminalMC
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

package dev.terminalmc.signtweaks.gui.widget;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class UnfocusingButton extends Button.Plain {

    public UnfocusingButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            OnPress onPress
    ) {
        super(x, y, width, height, message, onPress, Button.DEFAULT_NARRATION);
    }

    /**
     * Why they didn't make this use a field I have no idea.
     */
    @Override
    public boolean shouldTakeFocusAfterInteraction() {
        return false;
    }
}
