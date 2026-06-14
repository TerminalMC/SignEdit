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

import dev.terminalmc.signtweaks.SignTweaks;
import dev.terminalmc.signtweaks.config.Config;
import dev.terminalmc.signtweaks.config.Config.ConfigAction;
import dev.terminalmc.signtweaks.config.Config.EditCondition;
import dev.terminalmc.signtweaks.config.Config.Options;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

import static dev.terminalmc.signtweaks.util.Localization.localized;

public class ClothScreenProvider {

    private ClothScreenProvider() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    private static ClothConfigScreen instance;

    /**
     * Builds and returns a Cloth Config options screen.
     *
     * @param parent the current screen.
     * @return a new options {@link Screen}.
     * @throws NoClassDefFoundError if the Cloth Config API mod is not available.
     */
    static Screen getConfigScreen(Screen parent) {
        instance = null;
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
                        localized("option", "general.blockHeldKeys"),
                        options.blockHeldKeys
                )
                .setTooltip(localized("option", "general.blockHeldKeys.tooltip"))
                .setDefaultValue(Options.blockHeldKeysDefault)
                .setSaveConsumer(val -> options.blockHeldKeys = val)
                .build());

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "general.revertOnEscape"),
                        options.revertOnEscape
                )
                .setTooltip(localized(
                        "option",
                        "general.revertOnEscape.tooltip",
                        Component.translatable("key.keyboard.escape").getString(),
                        CommonComponents.GUI_DONE.getString()
                        ))
                .setDefaultValue(Config.Options.revertOnEscapeDefault)
                .setSaveConsumer(val -> options.revertOnEscape = val)
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

        general.addEntry(eb.startBooleanToggle(
                        localized("option", "general.blockEntitySneakEditOverride"),
                        options.blockEntitySneakEditOverride
                )
                .setTooltip(localized(
                        "option",
                        "general.blockEntitySneakEditOverride.tooltip",
                        localized("option", "general.editCondition"),
                        localized("option", "general.editCondition." + EditCondition.NOT_SNEAKING)
                ))
                .setDefaultValue(Options.blockEntitySneakEditOverrideDefault)
                .setSaveConsumer(val -> options.blockEntitySneakEditOverride = val)
                .build());

        ConfigCategory clickThrough =
                builder.getOrCreateCategory(localized("option", "clickThrough"));

        clickThrough.addEntry(eb.startBooleanToggle(
                        localized("option", "clickThrough.clickThroughSigns"),
                        options.clickThroughSigns
                )
                .setTooltip(localized("option", "clickThrough.clickThroughSigns.tooltip"))
                .setDefaultValue(Options.clickThroughSignsDefault)
                .setSaveConsumer(val -> options.clickThroughSigns = val)
                .build());

        clickThrough.addEntry(eb.startBooleanToggle(
                        localized("option", "clickThrough.clickThroughBanners"),
                        options.clickThroughBanners
                )
                .setTooltip(localized("option", "clickThrough.clickThroughBanners.tooltip"))
                .setDefaultValue(Options.clickThroughBannersDefault)
                .setSaveConsumer(val -> options.clickThroughBanners = val)
                .build());

        clickThrough.addEntry(eb.startBooleanToggle(
                        localized("option", "clickThrough.clickThroughHangingEntities"),
                        options.clickThroughHangingEntities
                )
                .setTooltip(localized("option", "clickThrough.clickThroughHangingEntities.tooltip"))
                .setDefaultValue(Options.clickThroughHangingEntitiesDefault)
                .setSaveConsumer(val -> options.clickThroughHangingEntities = val)
                .build());

        ConfigCategory autoFill = builder.getOrCreateCategory(localized("option", "autoFill"));

        autoFill.addEntry(eb.startBooleanToggle(
                        localized("option", "autoFill.useAutoFill"),
                        options.useAutoFill
                )
                .setTooltip(localized("option", "autoFill.useAutoFill.tooltip"))
                .setDefaultValue(Config.Options.useAutoFillDefault)
                .setSaveConsumer(val -> options.useAutoFill = val)
                .build());

        autoFill.addEntry(eb.startStrList(
                        localized("option", "autoFill.autoFillLines"),
                        List.of(options.autoFillLines)
                )
                .setTooltip(localized("option", "autoFill.autoFillLines.tooltip"))
                .setDefaultValue(List.of(Config.Options.autoFillLinesDefault.get()))
                .setSaveConsumer(val -> {
                    for (int i = 0; i < options.autoFillLines.length; i++) {
                        if (i < val.size()) {
                            options.autoFillLines[i] = val.get(i);
                        } else {
                            options.autoFillLines[i] = "";
                        }
                    }
                })
                .setInsertButtonEnabled(false)
                .setDeleteButtonEnabled(false)
                .setExpanded(true)
                .build());

        autoFill.addEntry(eb.startEnumSelector(
                        localized("option", "autoFill.useCopiedLines"),
                        ConfigAction.class,
                        options.lastAutoFillAction
                )
                .setTooltip(localized("option", "autoFill.useCopiedLines.tooltip"))
                .setEnumNameProvider(val -> {
                    if (val != options.lastAutoFillAction && val instanceof ConfigAction ca) {
                        options.lastAutoFillAction = ca;
                        if (SignTweaks.copiedLines != null) {
                            for (int i = 0; i < options.autoFillLines.length; i++) {
                                if (i < SignTweaks.copiedLines.length) {
                                    options.autoFillLines[i] = SignTweaks.copiedLines[i];
                                }
                                else {
                                    options.autoFillLines[i] = "";
                                }
                            }
                            if (instance != null) {
                                instance.save();
                                Minecraft.getInstance().setScreen(parent);
                            }
                        }
                    }
                    return localized("option", "autoFill.useCopiedLines.value");
                })
                .setDefaultValue(options.lastAutoFillAction)
                .setSaveConsumer(val -> {
                })
                .setRequirement(() -> SignTweaks.copiedLines != null)
                .build());

        Screen screen = builder.build();
        if (screen instanceof ClothConfigScreen ccs) {
            instance = ccs;
        }
        return screen;
    }
}
