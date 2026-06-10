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

package dev.terminalmc.signtweaks.mixin.gui;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.terminalmc.signtweaks.util.inject.ISignScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static dev.terminalmc.signtweaks.config.Config.options;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow
    public abstract boolean shouldCloseOnEsc();

    /**
     * Wraps the keypress handler to optionally revert changes when closing the screen using the
     * escape key.
     */
    @WrapMethod(
            method = "keyPressed"
    )
    protected boolean wrapKeyPressed(KeyEvent event, Operation<Boolean> original) {
        //noinspection ConstantValue
        if (event.isEscape() && shouldCloseOnEsc()
                && (Object) this instanceof AbstractSignEditScreen signScreen) {
            if (options().revertOnEscape) {
                ((ISignScreen) signScreen).signEdit$revertText(false);
            }
        }
        return original.call(event);
    }
}
