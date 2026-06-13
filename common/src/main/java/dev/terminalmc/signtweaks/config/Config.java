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

package dev.terminalmc.signtweaks.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.terminalmc.signtweaks.SignTweaks;
import dev.terminalmc.signtweaks.platform.services.PlatformServices;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.Supplier;

public class Config {

    private static final Path DIR_PATH = PlatformServices.getInstance().getConfigDir();
    private static final String FILE_NAME = SignTweaks.MOD_ID + ".json";
    private static final String BACKUP_FILE_NAME = SignTweaks.MOD_ID + ".unreadable.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Config() {
        // Deserializer and self-instantiation only.
    }

    // Enumerations

    public enum EditCondition {
        SNEAKING,
        NOT_SNEAKING,
        ALWAYS,
        NEVER
    }

    public enum ConfigAction {
        ACTION_1,
        ACTION_2
    }

    // Options

    public final Options options = new Options();

    public static Options options() {
        return Config.get().options;
    }

    public static class Options {

        // General

        public static final boolean useEnhancedEditorDefault = true;
        public boolean useEnhancedEditor = useEnhancedEditorDefault;

        public static final boolean showEditorToggleButtonDefault = false;
        public boolean showEditorToggleButton = showEditorToggleButtonDefault;

        public static final boolean showActionButtonsDefault = true;
        public boolean showActionButtons = showActionButtonsDefault;

        public static final boolean actionButtonsCloseUiDefault = false;
        public boolean actionButtonsCloseUi = actionButtonsCloseUiDefault;

        public static final boolean showLineBreakIndicatorDefault = false;
        public boolean showLineBreakIndicator = showLineBreakIndicatorDefault;

        public static final boolean blockHeldKeysDefault = true;
        public boolean blockHeldKeys = blockHeldKeysDefault;

        public static final boolean revertOnEscapeDefault = false;
        public boolean revertOnEscape = revertOnEscapeDefault;

        public static final EditCondition editConditionDefault = EditCondition.ALWAYS;
        public EditCondition editCondition = editConditionDefault;

        public static final boolean blockEntitySneakEditOverrideDefault = true;
        public boolean blockEntitySneakEditOverride = blockEntitySneakEditOverrideDefault;

        // ClickThrough

        public static final boolean clickThroughSignsDefault = true;
        public boolean clickThroughSigns = clickThroughSignsDefault;

        public static final boolean clickThroughBannersDefault = false;
        public boolean clickThroughBanners = clickThroughBannersDefault;

        public static final boolean clickThroughHangingEntitiesDefault = false;
        public boolean clickThroughHangingEntities = clickThroughHangingEntitiesDefault;

        // AutoFill

        public static final boolean useAutoFillDefault = false;
        public boolean useAutoFill = useAutoFillDefault;

        public static final Supplier<String[]> autoFillLinesDefault =
                () -> new String[]{"", "", "", ""};
        /**
         * Always contains exactly four elements.
         */
        public String[] autoFillLines = autoFillLinesDefault.get();

        public transient ConfigAction lastAutoFillAction = ConfigAction.ACTION_1;
    }

    // Instance management

    private static Config instance = null;

    public static Config get() {
        if (instance == null) {
            instance = Config.load();
        }
        return instance;
    }

    public static Config getAndSave() {
        get();
        save();
        return instance;
    }

    public static Config resetAndSave() {
        instance = new Config();
        save();
        return instance;
    }

    // Validation

    private void validate() {
        // Called after config is loaded
        String[] newLines = Options.autoFillLinesDefault.get();
        for (int i = 0; i < newLines.length; i++) {
            if (i < options.autoFillLines.length) {
                String oldLine = options.autoFillLines[i];
                newLines[i] = Objects.requireNonNullElse(oldLine, "");
            }
        }
        options.autoFillLines = newLines;
    }

    // Cleanup

    private void cleanup() {
        // Called before config is saved
    }

    // Load and save

    public static @NotNull Config load() {
        Path file = DIR_PATH.resolve(FILE_NAME);
        Config config = null;
        if (Files.exists(file)) {
            config = load(file, GSON);
            if (config == null) {
                backup();
                SignTweaks.LOG.warn("Resetting config");
            } else {
                config.validate();
            }
        }
        return config != null ? config : new Config();
    }

    private static @Nullable Config load(Path file, Gson gson) {
        try (
                InputStreamReader reader = new InputStreamReader(
                        new FileInputStream(file.toFile()),
                        StandardCharsets.UTF_8
                )
        ) {
            return gson.fromJson(reader, Config.class);
        } catch (Exception e) {
            // Catch Exception as errors in deserialization may not fall under
            // IOException or JsonParseException, but should not crash the game.
            SignTweaks.LOG.error("Unable to load config", e);
            return null;
        }
    }

    private static void backup() {
        try {
            SignTweaks.LOG.warn("Copying {} to {}", FILE_NAME, BACKUP_FILE_NAME);
            if (!Files.isDirectory(DIR_PATH))
                Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path backupFile = file.resolveSibling(BACKUP_FILE_NAME);
            Files.move(
                    file,
                    backupFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException e) {
            SignTweaks.LOG.error("Unable to copy config file", e);
        }
    }

    public static void save() {
        if (instance == null)
            return;
        instance.cleanup();
        try {
            if (!Files.isDirectory(DIR_PATH))
                Files.createDirectories(DIR_PATH);
            Path file = DIR_PATH.resolve(FILE_NAME);
            Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");
            try (
                    OutputStreamWriter writer = new OutputStreamWriter(
                            new FileOutputStream(tempFile.toFile()),
                            StandardCharsets.UTF_8
                    )
            ) {
                writer.write(GSON.toJson(instance));
            } catch (IOException e) {
                throw new IOException(e);
            }
            Files.move(
                    tempFile,
                    file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
            SignTweaks.onConfigSaved(instance);
        } catch (IOException e) {
            SignTweaks.LOG.error("Unable to save config", e);
        }
    }
}
