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

package dev.terminalmc.signtweaks.gui.screen;

import dev.terminalmc.signtweaks.config.Config;
import dev.terminalmc.signtweaks.config.Config.Options;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import static dev.terminalmc.signtweaks.util.Localization.localized;

public class ClothScreenProvider {

    private ClothScreenProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    /**
     * Builds and returns a Cloth Config options screen.
     *
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the Cloth Config API mod is not available.
     */
    static Screen getConfigScreen(Screen parent) {
        Config.Options options = Config.options();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(localized("name"))
                .setSavingRunnable(dev.terminalmc.signtweaks.config.Config::save);
        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(localized("option", "general"));

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "general.useEnhancedEditor"),
                        options.useEnhancedEditor
                )
                .setTooltip(localized("option", "general.useEnhancedEditor.tooltip"))
                .setDefaultValue(Config.Options.useEnhancedEditorDefault)
                .setSaveConsumer(val -> options.useEnhancedEditor = val)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "general.showEditorToggleButton"),
                        options.showEditorToggleButton
                )
                .setTooltip(localized("option", "general.showEditorToggleButton.tooltip"))
                .setDefaultValue(Config.Options.showEditorToggleButtonDefault)
                .setSaveConsumer(val -> options.showEditorToggleButton = val)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "general.showActionButtons"),
                        options.showActionButtons
                )
                .setTooltip(localized("option", "general.showActionButtons.tooltip"))
                .setDefaultValue(Config.Options.showActionButtonsDefault)
                .setSaveConsumer(val -> options.showActionButtons = val)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "general.actionButtonsCloseUi"),
                        options.actionButtonsCloseUi
                )
                .setTooltip(localized("option", "general.actionButtonsCloseUi.tooltip"))
                .setDefaultValue(Config.Options.actionButtonsCloseUiDefault)
                .setSaveConsumer(val -> options.actionButtonsCloseUi = val)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "general.showLineBreakIndicator"),
                        options.showLineBreakIndicator
                )
                .setTooltip(localized("option", "general.showLineBreakIndicator.tooltip"))
                .setDefaultValue(Config.Options.showLineBreakIndicatorDefault)
                .setSaveConsumer(val -> options.showLineBreakIndicator = val)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "general.blockMovementKeys"),
                        options.blockMovementKeys
                )
                .setTooltip(localized("option", "general.blockMovementKeys.tooltip"))
                .setDefaultValue(Options.blockMovementKeysDefault)
                .setSaveConsumer(val -> options.blockMovementKeys = val)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "general.saveOnEscape"),
                        options.saveOnEscape
                )
                .setTooltip(localized(
                        "option",
                        "general.saveOnEscape.tooltip",
                        Component.translatable("key.keyboard.escape").getString(),
                        CommonComponents.GUI_DONE.getString()
                        ))
                .setDefaultValue(Config.Options.saveOnEscapeDefault)
                .setSaveConsumer(val -> options.saveOnEscape = val)
                .build());

        general.addEntry(eb.startEnumSelector(
                        localized("option", "general.editCondition"),
                        Config.EditCondition.class,
                        options.editCondition
                )
                .setTooltip(localized("option", "general.editCondition.tooltip"))
                .setEnumNameProvider(val -> localized("option", "general.editCondition." + val))
                .setDefaultValue(Config.Options.editConditionDefault)
                .setSaveConsumer(val -> options.editCondition = val)
                .build());

        return builder.build();
    }
}
