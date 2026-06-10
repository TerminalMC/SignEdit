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

package dev.terminalmc.signtweaks;

import dev.terminalmc.signtweaks.config.Config;
import dev.terminalmc.signtweaks.util.Logging;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SignTweaks {

    public static final String MOD_ID = "signtweaks";
    public static final String MOD_NAME = "SignTweaks";
    public static final Logger LOG = Logging.getLogger(MOD_ID);
    public static final Component PREFIX = Component.empty()
            .append(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY))
            .append(Component.literal(MOD_NAME).withStyle(ChatFormatting.GOLD))
            .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
            .withStyle(ChatFormatting.GRAY);
    public static final List<KeyMapping> KEYBINDS = List.of();

    public static final List<KeyMapping> checkKeys = new ArrayList<>();
    public static final Set<KeyMapping> downKeys = new HashSet<>();

    public static boolean enhancedEditing;

    public static String[] copiedLines;
    public static String[] originalLines;

    private SignTweaks() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Client initialization.
     */
    public static void init() {

    }

    /**
     * Client after-tick event listener.
     */
    public static void afterClientTick(Minecraft mc) {

    }

    /**
     * Config save listener.
     */
    public static void onConfigSaved(Config config) {
        // If you are maintaining caches based on config, update them here.
    }
}
