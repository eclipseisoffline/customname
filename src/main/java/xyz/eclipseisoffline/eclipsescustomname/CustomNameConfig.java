package xyz.eclipseisoffline.eclipsescustomname;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.dynamic.Codecs;

public record CustomNameConfig(boolean formattingEnabled, boolean requirePermissions, List<Pattern> blacklistedNames,
                               int maxNameLength, boolean operatorsBypassRestrictions, boolean displayAbovePlayer,
                               CustomNameGroups groups) {
    private static final int MAX_MAX_LENGTH = 32;
    private static final Path CONFIG_FILE = Path.of(CustomName.MOD_ID + ".json");

    private static final Codec<Pattern> PATTERN_CODEC = Codec.STRING.xmap(Pattern::compile, Pattern::pattern);
    public static final Codec<CustomNameConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    fallbackIfMissing(Codec.BOOL, "enable_formatting", true).forGetter(CustomNameConfig::formattingEnabled),
                    fallbackIfMissing(Codec.BOOL, "require_permissions", true).forGetter(CustomNameConfig::requirePermissions),
                    fallbackIfMissing(PATTERN_CODEC.listOf(), "blacklisted_names", List.of()).forGetter(CustomNameConfig::blacklistedNames),
                    fallbackIfMissing(Codecs.rangedInt(1, MAX_MAX_LENGTH), "max_name_length", 16).forGetter(CustomNameConfig::maxNameLength),
                    fallbackIfMissing(Codec.BOOL, "operators_bypass_restrictions", false).forGetter(CustomNameConfig::operatorsBypassRestrictions),
                    fallbackIfMissing(Codec.BOOL, "display_above_player", false).forGetter(CustomNameConfig::displayAbovePlayer),
                    fallbackIfMissing(CustomNameGroups.CODEC, "name_groups", CustomNameGroups.EMPTY).forGetter(CustomNameConfig::groups)
            ).apply(instance, CustomNameConfig::new)
    );

    private static final CustomNameConfig DEFAULT = new CustomNameConfig(true, true, List.of(),
            16, false, false, CustomNameGroups.EMPTY);

    public boolean nameBlacklisted(String name) {
        for (Pattern blacklisted : blacklistedNames) {
            if (blacklisted.matcher(name).find()) {
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
            DEFAULT.write();
            return DEFAULT;
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
        return DEFAULT;
    }

    private static <T> MapCodec<T> fallbackIfMissing(Codec<T> codec, String name, T fallback) {
        return codec.optionalFieldOf(name).xmap(optional -> optional.orElse(fallback), Optional::of);
    }

    public record CustomNameGroups(Map<NameType, Map<String, List<ParsedPlayerName>>> nameGroups,
                                   List<String> groupPermissionNodes) {
        private static final Predicate<String> GROUP_NAME_PREDICATE = Pattern.compile("^([A-Z]|[a-z]|_)+$").asMatchPredicate();
        private static final Codec<String> GROUP_NAME_CODEC = Codec.STRING.validate(name -> {
            if (GROUP_NAME_PREDICATE.test(name)) {
                return DataResult.success(name);
            }
            return DataResult.error(() -> "Group name must consist of characters A-Z, a-z, or '_'");
        });
        private static final Codec<Map<String, List<ParsedPlayerName>>> GROUPS_CODEC = Codec.unboundedMap(GROUP_NAME_CODEC, ParsedPlayerName.CODEC.listOf());
        public static final Codec<CustomNameGroups> CODEC = Codec.unboundedMap(NameType.CODEC, GROUPS_CODEC).xmap(CustomNameGroups::new, CustomNameGroups::nameGroups);
        public static final CustomNameGroups EMPTY;

        public CustomNameGroups(Map<NameType, Map<String, List<ParsedPlayerName>>> nameGroups) {
            this(nameGroups, nameGroups.entrySet().stream()
                    .flatMap(entry -> entry.getValue().keySet().stream()
                            .map(group -> getGroupPermissionNode(entry.getKey(), group)))
                    .toList());
        }

        public List<Text> validNames(CommandSource source, NameType nameType) {
            Map<String, List<ParsedPlayerName>> groups = nameGroups.get(nameType);
            if (groups == null) {
                return List.of();
            }

            List<ParsedPlayerName> names = new ArrayList<>();
            for (String group : groups.keySet()) {
                if (CustomName.checkPermission(source, getGroupPermissionNode(nameType, group))) {
                    names.addAll(groups.get(group));
                }
            }

            return names.stream().map(ParsedPlayerName::parsed).toList();
        }

        private static String getGroupPermissionNode(NameType nameType, String group) {
            return "customname.group." + nameType.asString() + "." + group;
        }

        static {
            // Prefill with all name types so that the user has a guide in the default config
            Map<NameType, Map<String, List<ParsedPlayerName>>> empty = new HashMap<>();
            for (NameType type : NameType.values()) {
                empty.put(type, Map.of());
            }
            EMPTY = new CustomNameGroups(Map.copyOf(empty));
        }
    }
}
