package xyz.eclipseisoffline.eclipsescustomname;

import com.google.gson.JsonParser;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import xyz.eclipseisoffline.eclipsescustomname.network.FakeTextDisplayHolder;

public class PlayerNameManager extends SavedData {
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
    private final LuckPerms luckPerms;

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

    public void updatePlayerName(ServerPlayer player, Component name, NameType type) {
        if (name == null) {
            switch (type) {
                case PREFIX -> playerPrefixes.remove(player.getUUID());
                case SUFFIX -> playerSuffixes.remove(player.getUUID());
                case NICKNAME -> playerNicknames.remove(player.getUUID());
            }
        } else {
            switch (type) {
                case PREFIX -> playerPrefixes.put(player.getUUID(), name);
                case SUFFIX -> playerSuffixes.put(player.getUUID(), name);
                case NICKNAME -> playerNicknames.put(player.getUUID(), name);
            }
        }
        markDirty(player);
    }

    public Component getFullPlayerName(ServerPlayer player) {
        if (!fullPlayerNames.containsKey(player.getUUID())) {
            updateFullPlayerName(player);
        }
        return fullPlayerNames.get(player.getUUID());
    }

    public Component getPlayerName(ServerPlayer player, NameType nameType) {
        return switch (nameType) {
            case PREFIX -> playerPrefixes.get(player.getUUID());
            case SUFFIX -> playerSuffixes.get(player.getUUID());
            case NICKNAME -> playerNicknames.get(player.getUUID());
        };
    }

    private void markDirty(ServerPlayer player) {
        updateFullPlayerName(player);
        setDirty();
    }

    private void updateFullPlayerName(ServerPlayer player) {
        String permissionsPrefix = null;
        String permissionsSuffix = null;

        if (luckPerms != null) {
            User luckPermsUser = luckPerms.getUserManager().getUser(player.getUUID());
            if (luckPermsUser != null) {
                permissionsPrefix = luckPermsUser.getCachedData().getMetaData().getPrefix();
                permissionsSuffix = luckPermsUser.getCachedData().getMetaData().getSuffix();
            }
        }

        Component prefix = playerPrefixes.get(player.getUUID());
        Component suffix = playerSuffixes.get(player.getUUID());
        Component nickname = playerNicknames.get(player.getUUID());

        MutableComponent name = Component.literal("");
        if (permissionsPrefix != null) {
            name.append(CustomNameUtil.playerNameArgumentToComponent(permissionsPrefix, true));
            name.append(" ");
        }
        if (prefix != null) {
            name.append(prefix);
            name.append(" ");
        }
        name.append(Objects.requireNonNullElseGet(nickname, player::getName));
        if (suffix != null) {
            name.append(" ");
            name.append(suffix);
        }
        if (permissionsSuffix != null) {
            name.append(" ");
            name.append(CustomNameUtil.playerNameArgumentToComponent(permissionsSuffix, true));
        }

        fullPlayerNames.put(player.getUUID(), name);
        ((FakeTextDisplayHolder) player).customName$updateName();
    }

    private static SavedDataType<PlayerNameManager> type(MinecraftServer server) {
        Codec<PlayerNameManager> codec = RecordCodecBuilder.create(instance ->
                instance.group(
                        NAME_MAP_CODEC.fieldOf("prefixes").forGetter(manager -> manager.playerPrefixes),
                        NAME_MAP_CODEC.fieldOf("nicknames").forGetter(manager -> manager.playerNicknames),
                        NAME_MAP_CODEC.fieldOf("suffixes").forGetter(manager -> manager.playerSuffixes)
                ).apply(instance, (prefixes, nicknames, suffixes) -> new PlayerNameManager(server, prefixes, nicknames, suffixes))
        );
        return new SavedDataType<>(CustomName.MOD_ID, () -> new PlayerNameManager(server, Map.of(), Map.of(), Map.of()), codec, null);
    }

    public static PlayerNameManager getPlayerNameManager(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(type(server));
    }

    public static PlayerNameManager getPlayerNameManager(CommandSourceStack source) {
        return getPlayerNameManager(source.getServer());
    }
}
