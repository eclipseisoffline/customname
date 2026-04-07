package xyz.eclipseisoffline.eclipsescustomname;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JavaOps;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Display;
import xyz.eclipseisoffline.commonpermissionsapi.api.CommonPermissionNode;
import xyz.eclipseisoffline.commonpermissionsapi.api.CommonPermissions;

public record CustomNameConfig(boolean formattingEnabled, boolean requirePermissions, List<Pattern> blacklistedNames,
                               int maxNameLength, boolean operatorsBypassRestrictions, CustomNameDisplaySettings displaySettings,
                               CustomNameGroups groups) {
    private static final int MAX_MAX_LENGTH = 32;
    private static final Path CONFIG_FILE = Path.of(CustomName.MOD_ID + ".json");

    private static final Codec<Pattern> PATTERN_CODEC = Codec.STRING.xmap(Pattern::compile, Pattern::pattern);
    public static final Codec<CustomNameConfig> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    fallbackIfMissing(Codec.BOOL, "enable_formatting", true).forGetter(CustomNameConfig::formattingEnabled),
                    fallbackIfMissing(Codec.BOOL, "require_permissions", true).forGetter(CustomNameConfig::requirePermissions),
                    fallbackIfMissing(PATTERN_CODEC.listOf(), "blacklisted_names", List.of()).forGetter(CustomNameConfig::blacklistedNames),
                    fallbackIfMissing(ExtraCodecs.intRange(1, MAX_MAX_LENGTH), "max_name_length", 16).forGetter(CustomNameConfig::maxNameLength),
                    fallbackIfMissing(Codec.BOOL, "operators_bypass_restrictions", false).forGetter(CustomNameConfig::operatorsBypassRestrictions),
                    fallbackIfMissing(CustomNameDisplaySettings.CODEC_WITH_LEGACY_ALTERNATIVE, "display_above_player", CustomNameDisplaySettings.DEFAULT).forGetter(CustomNameConfig::displaySettings),
                    fallbackIfMissing(CustomNameGroups.CODEC, "name_groups", CustomNameGroups.EMPTY).forGetter(CustomNameConfig::groups)
            ).apply(instance, CustomNameConfig::new)
    );

    private static final CustomNameConfig DEFAULT = new CustomNameConfig(true, true, List.of(),
            16, false, CustomNameDisplaySettings.DEFAULT, CustomNameGroups.EMPTY);

    public boolean nameBlacklisted(String name) {
        for (Pattern blacklisted : blacklistedNames) {
            if (blacklisted.matcher(name).find()) {
                return true;
            }
        }

        return false;
    }

    public void write(Path configDir) {
        Path configPath = configDir.resolve(CONFIG_FILE);
        DataResult<JsonElement> encoded = CODEC.encodeStart(JsonOps.INSTANCE, this);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        try (Writer writer = new FileWriter(configPath.toFile())) {
            gson.toJson(encoded.getOrThrow(), writer);
        } catch (IOException | IllegalStateException exception) {
            CustomName.LOGGER.error("Failed writing config file!", exception);
        }
    }

    public static CustomNameConfig readOrCreate(Path configDir) {
        Path configPath = configDir.resolve(CONFIG_FILE);

        if (!Files.exists(configPath)) {
            CustomName.LOGGER.info("No config file found, generating a default one");
            DEFAULT.write(configDir);
            return DEFAULT;
        }

        try (Reader reader = new FileReader(configPath.toFile())) {
            JsonElement configJson = JsonParser.parseReader(reader);
            DataResult<Pair<CustomNameConfig, JsonElement>> readConfig = CODEC.decode(JsonOps.INSTANCE, configJson);
            CustomNameConfig config = readConfig.getOrThrow(s -> new IllegalStateException("Codec failed parsing config file! " + s)).getFirst();
            config.write(configDir);
            return config;
        } catch (IOException | IllegalStateException exception) {
            CustomName.LOGGER.error("Failed reading config file! Using default config, please fix the errors listed to let the config load correctly!", exception);
        }
        return DEFAULT;
    }

    private static <T> MapCodec<T> fallbackIfMissing(Codec<T> codec, String name, T fallback) {
        return codec.optionalFieldOf(name).xmap(optional -> optional.orElseGet(() -> {
            CustomName.LOGGER.info("Using default value for field {} as it was not present in the existing config file", name);
            return fallback;
        }), Optional::of);
    }

    public record CustomNameDisplaySettings(boolean enabled, int textOpacity, int backgroundColor, List<CustomNameDisplayLine> lines) {
        public static final CustomNameDisplaySettings DEFAULT = new CustomNameDisplaySettings(false, 255,
                Display.TextDisplay.INITIAL_BACKGROUND, List.of(CustomNameDisplayLine.DEFAULT));
        public static final Codec<CustomNameDisplaySettings> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.BOOL.fieldOf("enabled").forGetter(CustomNameDisplaySettings::enabled),
                        Codec.intRange(0, 255).fieldOf("text_opacity").forGetter(CustomNameDisplaySettings::textOpacity),
                        ExtraCodecs.STRING_ARGB_COLOR.fieldOf("background_color").forGetter(CustomNameDisplaySettings::backgroundColor),
                        fallbackIfMissing(ExtraCodecs.nonEmptyList(CustomNameDisplayLine.CODEC.listOf()), "lines", List.of(CustomNameDisplayLine.DEFAULT))
                                .forGetter(CustomNameDisplaySettings::lines)
                ).apply(instance, CustomNameDisplaySettings::new)
        );
        public static final Codec<CustomNameDisplaySettings> CODEC_WITH_LEGACY_ALTERNATIVE = Codec.withAlternative(CODEC, Codec.BOOL,
                enabled -> new CustomNameDisplaySettings(enabled, DEFAULT.textOpacity, DEFAULT.backgroundColor, DEFAULT.lines));
    }

    public record CustomNameDisplayLine(List<NameType> names) {
        public static final CustomNameDisplayLine DEFAULT = new CustomNameDisplayLine(
                List.of(NameType.LUCKPERMS_PREFIX, NameType.PREFIX, NameType.NICKNAME, NameType.SUFFIX, NameType.LUCKPERMS_SUFFIX));
        public static final Codec<CustomNameDisplayLine> CODEC = Codec.STRING.comapFlatMap(CustomNameDisplayLine::parse, CustomNameDisplayLine::toString);

        public static DataResult<CustomNameDisplayLine> parse(String raw) {
            DataResult<List<NameType>> names = DataResult.success(List.of());
            String[] split = raw.split(" ");
            for (String name : split) {
                names = names.apply2((list, parsed) -> Stream.concat(list.stream(), Stream.of(parsed)).toList(), NameType.CODEC.parse(JavaOps.INSTANCE, name));
            }
            return names.map(Collections::unmodifiableList).map(CustomNameDisplayLine::new);
        }

        @Override
        public String toString() {
            return names.stream().map(NameType::getSerializedName).collect(Collectors.joining(" "));
        }
    }

    // TODO Structure a bit messy here, maybe rework the codecs later
    public record CustomNameGroups(Map<NameType, Map<String, List<ParsedPlayerName>>> nameGroups,
                                   Map<NameType, List<CustomNameGroup>> parsedGroups) {
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
                    .map(entry -> Pair.of(entry.getKey(), entry.getValue().entrySet().stream()
                            .map(group -> new CustomNameGroup(entry.getKey(), group))
                            .toList()))
                    .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond)));
        }

        public List<ParsedPlayerName> validNames(CommandSourceStack source, NameType nameType) {
            List<CustomNameGroup> groups = parsedGroups.get(nameType);
            if (groups == null) {
                return List.of();
            }

            List<ParsedPlayerName> names = new ArrayList<>();
            for (CustomNameGroup group : groups) {
                if (CommonPermissions.check(source, group.permission)) {
                    names.addAll(group.allowedNames);
                }
            }

            return List.copyOf(names);
        }

        public boolean validName(CommandSourceStack source, NameType nameType, Component name) {
            return validNames(source, nameType).stream()
                    .map(ParsedPlayerName::parsed)
                    .anyMatch(Predicate.isEqual(name));
        }

        public Predicate<CommandSourceStack> partOfGroup(NameType type) {
            return parsedGroups.getOrDefault(type, List.of()).stream()
                    .map(group -> CommonPermissions.require(group.permission))
                    .reduce(Predicate::or)
                    .orElse(_ -> false);
        }

        public SuggestionProvider<CommandSourceStack> createSuggestionsProvider(NameType nameType) {
            return (context, builder) ->
                    SharedSuggestionProvider.suggest(validNames(context.getSource(), nameType).stream().map(ParsedPlayerName::raw), builder);
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

    public record CustomNameGroup(CommonPermissionNode permission, List<ParsedPlayerName> allowedNames) {

        public CustomNameGroup(NameType type, Map.Entry<String, List<ParsedPlayerName>> stringListEntry) {
            this(CustomNamePermissions.createNode(getGroupPermissionNode(type, stringListEntry.getKey())), stringListEntry.getValue());
        }

        private static String getGroupPermissionNode(NameType nameType, String group) {
            return "group." + nameType.getSerializedName() + "." + group;
        }
    }
}
