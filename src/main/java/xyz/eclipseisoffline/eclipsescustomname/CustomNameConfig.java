package xyz.eclipseisoffline.eclipsescustomname;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

public class CustomNameConfig {
    private static final CustomNameConfig DEFAULT_CONFIG = new CustomNameConfig(true, true, List.of());
    private static final Path CONFIG_FILE = Path.of(CustomName.MOD_ID + ".json");

    private final boolean enableFormatting;
    private final boolean requirePermissions;
    private final List<Pattern> blacklistedNames;

    private CustomNameConfig(boolean enableFormatting, boolean requirePermissions,
            List<Pattern> blacklistedNames) {
        this.enableFormatting = enableFormatting;
        this.requirePermissions = requirePermissions;
        this.blacklistedNames = blacklistedNames;
    }

    public boolean formattingEnabled() {
        return enableFormatting;
    }

    public boolean requirePermissions() {
        return requirePermissions;
    }

    public boolean nameBlacklisted(String name) {
        for (Pattern blacklisted : blacklistedNames) {
            if (blacklisted.matcher(name).matches()) {
                return true;
            }
        }

        return false;
    }

    private void writeConfig() {
        JsonObject configJson = new JsonObject();
        configJson.addProperty("enable_formatting", enableFormatting);
        configJson.addProperty("require_permissions", requirePermissions);

        JsonArray blacklistedNamesJson = new JsonArray();
        blacklistedNames.forEach(pattern -> blacklistedNamesJson.add(pattern.pattern()));
        configJson.add("blacklisted_names", blacklistedNamesJson);

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        Gson gson = gsonBuilder.create();

        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        try {
            Files.writeString(configPath, gson.toJson(configJson));
        } catch (IOException exception) {
            CustomName.LOGGER.error("Failed writing config file!", exception);
        }
    }

    public static CustomNameConfig getInstance() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);

        if (!Files.exists(configPath)) {
            CustomName.LOGGER.info("No config file found, generating a default one");
            DEFAULT_CONFIG.writeConfig();
            return DEFAULT_CONFIG;
        }

        try {
            String config = Files.readString(configPath);

            JsonObject configJson = JsonParser.parseString(config).getAsJsonObject();
            boolean enableFormatting = configJson.get("enable_formatting").getAsBoolean();
            boolean requirePermissions = configJson.get("require_permissions").getAsBoolean();
            List<String> blacklistedNames = configJson
                    .getAsJsonArray("blacklisted_names").asList().stream().map(JsonElement::getAsString).toList();

            List<Pattern> finalBlacklisted = new ArrayList<>();
            for (String blacklisted : blacklistedNames) {
                try {
                    finalBlacklisted.add(Pattern.compile(blacklisted));
                } catch (PatternSyntaxException exception) {
                    CustomName.LOGGER.error("Invalid blacklisted regex " + blacklisted, exception);
                }
            }

            return new CustomNameConfig(enableFormatting, requirePermissions, finalBlacklisted);
        } catch (IOException exception) {
            CustomName.LOGGER.error("Failed reading config file! Using default config", exception);
        }

        return DEFAULT_CONFIG;
    }
}
