package xyz.eclipseisoffline.eclipsescustomname;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.dynamic.Codecs;

public record CustomNameConfig(boolean formattingEnabled, boolean requirePermissions, List<Pattern> blacklistedNames,
                               int maxNameLength, boolean operatorsBypassRestrictions) {
    private static final int MAX_MAX_LENGTH = 32;
    private static final Path CONFIG_FILE = Path.of(CustomName.MOD_ID + ".json");

    private static final Codec<Pattern> PATTERN_CODEC = Codec.STRING.xmap(Pattern::compile, Pattern::pattern);
    public static final Codec<CustomNameConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.fieldOf("enable_formatting").orElse(true).forGetter(CustomNameConfig::formattingEnabled),
                    Codec.BOOL.fieldOf("require_permissions").orElse(true).forGetter(CustomNameConfig::requirePermissions),
                    PATTERN_CODEC.listOf().fieldOf("blacklisted_names").orElse(List.of()).forGetter(CustomNameConfig::blacklistedNames),
                    Codecs.rangedInt(1, MAX_MAX_LENGTH).fieldOf("max_name_length").orElse(16).forGetter(CustomNameConfig::maxNameLength),
                    Codec.BOOL.fieldOf("operators_bypass_restrictions").orElse(false).forGetter(CustomNameConfig::operatorsBypassRestrictions)
            ).apply(instance, CustomNameConfig::new)
    );

    public boolean nameBlacklisted(String name) {
        for (Pattern blacklisted : blacklistedNames) {
            if (blacklisted.matcher(name).matches()) {
                return true;
            }
        }

        return false;
    }

    public void write() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        DataResult<JsonElement> encoded = CODEC.encodeStart(JsonOps.INSTANCE, this);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        try (Writer writer = new FileWriter(configPath.toFile())) {
            gson.toJson(encoded.getOrThrow(), writer);
        } catch (IOException | IllegalStateException exception) {
            CustomName.LOGGER.error("Failed writing config file!", exception);
        }
    }

    public static CustomNameConfig readOrCreate() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);

        if (!Files.exists(configPath)) {
            CustomName.LOGGER.info("No config file found, generating a default one");
            CustomNameConfig config = createDefault();
            config.write();
            return config;
        }

        try (Reader reader = new FileReader(configPath.toFile())) {
            JsonElement configJson = JsonParser.parseReader(reader);
            DataResult<Pair<CustomNameConfig, JsonElement>> readConfig = CODEC.decode(JsonOps.INSTANCE, configJson);
            CustomNameConfig config = readConfig.getOrThrow(s -> new IllegalStateException("Codec failed parsing config file! " + s)).getFirst();
            config.write();
            return config;
        } catch (IOException | IllegalStateException exception) {
            CustomName.LOGGER.error("Failed reading config file! Using default config, please fix the errors listed to let the config load correctly!", exception);
        }
        return createDefault();
    }

    private static CustomNameConfig createDefault() {
        return new CustomNameConfig(true, true, List.of(), 16, false);
    }
}
