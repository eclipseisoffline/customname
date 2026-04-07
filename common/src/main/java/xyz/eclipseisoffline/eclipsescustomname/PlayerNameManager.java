package xyz.eclipseisoffline.eclipsescustomname;

import com.google.gson.JsonParser;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;
import xyz.eclipseisoffline.eclipsescustomname.network.FakeTextDisplayHolder;

public class PlayerNameManager extends SavedData {
    public static final Identifier ID = CustomName.getModdedIdentifier("names");

    private static final Codec<Component> LEGACY_COMPONENT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
            if (ops instanceof RegistryOps<?> registryOps) {
                return ops.getStringValue(input).map(string -> {
                    JsonReader reader = new JsonReader(new StringReader(string));
                    reader.setStrictness(Strictness.LENIENT);
                    return JsonParser.parseReader(reader);
                }).flatMap(element -> ComponentSerialization.CODEC.parse(registryOps.withParent(JsonOps.INSTANCE), element))
                        .map(text -> Pair.of(text, ops.empty()));
            }
            return DataResult.error(() -> "Decoding text requires registry ops");
        }

        @Override
        public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
            return DataResult.error(() -> "Unsupported operation; legacy codec should not be used to encode");
        }
    };
    // Order is important here: we should attempt LEGACY_TEXT_CODEC here, and not go straight to trying to parse a text component from NBT
    // The latter would just put the entire legacy JSON string as a literal component
    private static final Codec<Component> NAME_COMPONENT_CODEC = Codec.either(LEGACY_COMPONENT_CODEC, ComponentSerialization.CODEC).xmap(Either::unwrap, Either::right);

    private static final Codec<Map<UUID, Component>> NAME_MAP_CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, NAME_COMPONENT_CODEC);

    private final Map<UUID, Component> playerPrefixes = new HashMap<>();
    private final Map<UUID, Component> playerSuffixes = new HashMap<>();
    private final Map<UUID, Component> playerNicknames = new HashMap<>();
    private final Map<UUID, Component> fullPlayerNames = new HashMap<>();
    private final @Nullable LuckPerms luckPerms;

    private PlayerNameManager(MinecraftServer server, Map<UUID, Component> prefixes, Map<UUID, Component> nicknames, Map<UUID, Component> suffixes) {
        this.playerPrefixes.putAll(prefixes);
        this.playerNicknames.putAll(nicknames);
        this.playerSuffixes.putAll(suffixes);

        LuckPerms luckPerms;
        String luckPermsState = "found";
        try {
            luckPerms = LuckPermsProvider.get();
            luckPerms.getEventBus().subscribe(UserDataRecalculateEvent.class, event -> {
                UUID uuid = event.getUser().getUniqueId();
                fullPlayerNames.remove(uuid);

                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    CustomNameUtil.updateListName(player);
                }
            });
        } catch (NoClassDefFoundError | IllegalStateException exception) {
            luckPerms = null;
            luckPermsState = "not found";
        }
        this.luckPerms = luckPerms;

        CustomName.LOGGER.info("Creating player name mappings - LuckPerms {}!", luckPermsState);
    }

    public void updatePlayerName(ServerPlayer player, @Nullable Component name, NameType type) {
        Map<UUID, Component> nameTypeMap = switch (type) {
            case PREFIX -> playerPrefixes;
            case SUFFIX -> playerSuffixes;
            case NICKNAME -> playerNicknames;
            default -> throw new IllegalArgumentException("Unsupported name type " + type);
        };

        if (name == null) {
            nameTypeMap.remove(player.getUUID());
        } else {
            nameTypeMap.put(player.getUUID(), name);
        }
        markDirty(player);
    }

    public Component getFullPlayerName(ServerPlayer player) {
        if (!fullPlayerNames.containsKey(player.getUUID())) {
            updateFullPlayerName(player);
        }
        return fullPlayerNames.get(player.getUUID());
    }

    public @Nullable Component getPlayerName(ServerPlayer player, NameType nameType) {
        return switch (nameType) {
            case PREFIX -> playerPrefixes.get(player.getUUID());
            case SUFFIX -> playerSuffixes.get(player.getUUID());
            case NICKNAME -> playerNicknames.getOrDefault(player.getUUID(), player.getName());
            case LUCKPERMS_PREFIX -> supplyWithLuckPerms(() -> parseLuckPermsName(player.getUUID(), CachedMetaData::getPrefix));
            case LUCKPERMS_SUFFIX -> supplyWithLuckPerms(() -> parseLuckPermsName(player.getUUID(), CachedMetaData::getSuffix));
        };
    }

    private void markDirty(ServerPlayer player) {
        updateFullPlayerName(player);
        setDirty();
    }

    private void updateFullPlayerName(ServerPlayer player) {
        Component prefix = getPlayerName(player, NameType.PREFIX);
        Component suffix = getPlayerName(player, NameType.SUFFIX);
        Component nickname = getPlayerName(player, NameType.NICKNAME);
        Component luckPermsPrefix = getPlayerName(player, NameType.LUCKPERMS_PREFIX);
        Component luckPermsSuffix = getPlayerName(player, NameType.LUCKPERMS_SUFFIX);

        MutableComponent name = Component.empty();
        if (luckPermsPrefix != null) {
            name.append(luckPermsPrefix);
            name.append(" ");
        }
        if (prefix != null) {
            name.append(prefix);
            name.append(" ");
        }
        name.append(Objects.requireNonNull(nickname));
        if (suffix != null) {
            name.append(" ");
            name.append(suffix);
        }
        if (luckPermsSuffix != null) {
            name.append(" ");
            name.append(luckPermsSuffix);
        }

        fullPlayerNames.put(player.getUUID(), name);
        ((FakeTextDisplayHolder) player).customName$updateName(false);
    }

    private @Nullable Component parseLuckPermsName(UUID uuid, Function<CachedMetaData, @Nullable String> getter) {
        return getLuckPermsMeta(uuid, getter.andThen(string -> {
            if (string == null) {
                return null;
            }
            return CustomNameUtil.playerNameArgumentToComponent(string, true);
        }));
    }

    private <T> @Nullable T getLuckPermsMeta(UUID uuid, Function<CachedMetaData, @Nullable T> getter) {
        assert luckPerms != null;
        User luckPermsUser = luckPerms.getUserManager().getUser(uuid);
        if (luckPermsUser != null) {
            return getter.apply(luckPermsUser.getCachedData().getMetaData());
        }
        return null;
    }

    private <T> @Nullable T supplyWithLuckPerms(Supplier<@Nullable T> supplier) {
        if (luckPerms != null) {
            return supplier.get();
        }
        return null;
    }

    private static SavedDataType<PlayerNameManager> type(MinecraftServer server) {
        Codec<PlayerNameManager> codec = RecordCodecBuilder.create(instance ->
                instance.group(
                        NAME_MAP_CODEC.fieldOf("prefixes").forGetter(manager -> manager.playerPrefixes),
                        NAME_MAP_CODEC.fieldOf("nicknames").forGetter(manager -> manager.playerNicknames),
                        NAME_MAP_CODEC.fieldOf("suffixes").forGetter(manager -> manager.playerSuffixes)
                ).apply(instance, (prefixes, nicknames, suffixes) -> new PlayerNameManager(server, prefixes, nicknames, suffixes))
        );
        return new SavedDataType<>(ID, () -> new PlayerNameManager(server, Map.of(), Map.of(), Map.of()), codec, null);
    }

    public static PlayerNameManager getPlayerNameManager(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(type(server));
    }

    public static PlayerNameManager getPlayerNameManager(CommandSourceStack source) {
        return getPlayerNameManager(source.getServer());
    }
}
