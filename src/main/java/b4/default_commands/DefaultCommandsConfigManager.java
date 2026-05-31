package b4.default_commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DefaultCommandsConfigManager {
    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("default_commands.json");

    private static DefaultCommandsConfig cachedConfig;

    private DefaultCommandsConfigManager() {
    }

    public static synchronized DefaultCommandsConfig getConfig() {
        if (cachedConfig == null) {
            cachedConfig = loadConfig();
        }
        return cachedConfig;
    }

    public static synchronized DefaultCommandsConfig getConfigCopy() {
        return getConfig().copy();
    }

    public static synchronized void setConfig(DefaultCommandsConfig newConfig) {
        newConfig.normalize();
        cachedConfig = newConfig;
        saveConfig(newConfig);
    }

    private static DefaultCommandsConfig loadConfig() {
        if (!Files.exists(CONFIG_PATH)) {
            DefaultCommandsConfig config = new DefaultCommandsConfig();
            saveConfig(config);
            return config;
        }

        try {
            String raw = Files.readString(CONFIG_PATH);
            DefaultCommandsConfig loadedConfig = GSON.fromJson(raw, DefaultCommandsConfig.class);
            if (loadedConfig == null) {
                Default_commands.LOGGER.warn("Config file {} was empty, replacing with defaults.", CONFIG_PATH);
                loadedConfig = new DefaultCommandsConfig();
            }
            loadedConfig.normalize();
            return loadedConfig;
        } catch (IOException | JsonParseException e) {
            Default_commands.LOGGER.error("Failed to read config file {}, using defaults.", CONFIG_PATH, e);
            return new DefaultCommandsConfig();
        }
    }

    private static void saveConfig(DefaultCommandsConfig config) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(config));
        } catch (IOException e) {
            Default_commands.LOGGER.error("Failed to save config file {}", CONFIG_PATH, e);
        }
    }
}
