package xyz.eclipseisoffline.eclipsescustomname;

import com.google.gson.JsonParser;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
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
import net.minecraft.registry.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

public class PlayerNameManager extends PersistentState {
    private static final Codec<Text> LEGACY_TEXT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<Text, T>> decode(DynamicOps<T> ops, T input) {
            if (ops instanceof RegistryOps<?> registryOps) {
                return ops.getStringValue(input).map(string -> {
                    JsonReader reader = new JsonReader(new StringReader(string));
                    reader.setStrictness(Strictness.LENIENT);
                    return JsonParser.parseReader(reader);
                }).flatMap(element -> TextCodecs.CODEC.parse(registryOps.withDelegate(JsonOps.INSTANCE), element))
                        .map(text -> Pair.of(text, ops.empty()));
            }
            return DataResult.error(() -> "Decoding text requires registry ops");
        }

        @Override
        public <T> DataResult<T> encode(Text input, DynamicOps<T> ops, T prefix) {
            return DataResult.error(() -> "Unsupported operation; legacy codec should not be used to encode");
        }
    };
    private static final Codec<Text> NAME_TEXT_CODEC = Codec.either(LEGACY_TEXT_CODEC, TextCodecs.CODEC).xmap(either -> either.left().orElseGet(() -> either.right().orElseThrow()), Either::right);

    private static final Codec<Map<UUID, Text>> NAME_MAP_CODEC = Codec.unboundedMap(Uuids.STRING_CODEC, NAME_TEXT_CODEC);

    private final CustomNameConfig config;
    private final Map<UUID, Text> playerPrefixes = new HashMap<>();
    private final Map<UUID, Text> playerSuffixes = new HashMap<>();
    private final Map<UUID, Text> playerNicknames = new HashMap<>();
    private final Map<UUID, Text> fullPlayerNames = new HashMap<>();
    private final LuckPerms luckPerms;

    private PlayerNameManager(MinecraftServer server, CustomNameConfig config, Map<UUID, Text> prefixes, Map<UUID, Text> nicknames, Map<UUID, Text> suffixes) {
        this.config = config;
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

                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                if (player != null) {
                    CustomName.updateListName(player);
                }
            });
        } catch (NoClassDefFoundError | IllegalStateException exception) {
            luckPerms = null;
            luckPermsState = "not found";
        }
        this.luckPerms = luckPerms;

        CustomName.LOGGER.info("Creating player name mappings - LuckPerms {}!", luckPermsState);
    }

    public void updatePlayerName(ServerPlayerEntity player, Text name, NameType type) {
        switch (type) {
            case PREFIX -> playerPrefixes.put(player.getUuid(), name);
            case SUFFIX -> playerSuffixes.put(player.getUuid(), name);
            case NICKNAME -> playerNicknames.put(player.getUuid(), name);
        }
        markDirty(player);
    }

    public Text getFullPlayerName(ServerPlayerEntity player) {
        if (!fullPlayerNames.containsKey(player.getUuid())) {
            updateFullPlayerName(player);
        }
        return fullPlayerNames.get(player.getUuid());
    }

    private void markDirty(ServerPlayerEntity player) {
        updateFullPlayerName(player);
        markDirty();
    }

    private void updateFullPlayerName(ServerPlayerEntity player) {
        String permissionsPrefix = null;
        String permissionsSuffix = null;

        if (luckPerms != null) {
            User luckPermsUser = luckPerms.getUserManager().getUser(player.getUuid());
            if (luckPermsUser != null) {
                permissionsPrefix = luckPermsUser.getCachedData().getMetaData().getPrefix();
                permissionsSuffix = luckPermsUser.getCachedData().getMetaData().getSuffix();
            }
        }

        Text prefix = playerPrefixes.get(player.getUuid());
        Text suffix = playerSuffixes.get(player.getUuid());
        Text nickname = playerNicknames.get(player.getUuid());

        MutableText name = Text.literal("");
        if (permissionsPrefix != null) {
            name.append(CustomName
                    .argumentToText(permissionsPrefix, config.formattingEnabled(), true, false));
            name.append(" ");
        }
        if (prefix != null) {
            name.append(prefix);
            name.append(" ");
        }
        if (nickname != null) {
            name.append(nickname);
        } else {
            name.append(player.getName());
        }
        if (suffix != null) {
            name.append(" ");
            name.append(suffix);
        }
        if (permissionsSuffix != null) {
            name.append(" ");
            name.append(CustomName
                    .argumentToText(permissionsSuffix, config.formattingEnabled(), true, false));
        }

        fullPlayerNames.put(player.getUuid(), name);
    }

    private static PersistentStateType<PlayerNameManager> type(MinecraftServer server, CustomNameConfig config) {
        Codec<PlayerNameManager> codec = RecordCodecBuilder.create(instance ->
                instance.group(
                        NAME_MAP_CODEC.fieldOf("prefixes").forGetter(manager -> manager.playerPrefixes),
                        NAME_MAP_CODEC.fieldOf("nicknames").forGetter(manager -> manager.playerNicknames),
                        NAME_MAP_CODEC.fieldOf("suffixes").forGetter(manager -> manager.playerSuffixes)
                ).apply(instance, (prefixes, nicknames, suffixes) -> new PlayerNameManager(server, config, prefixes, nicknames, suffixes))
        );
        return new PersistentStateType<>(CustomName.MOD_ID, () -> new PlayerNameManager(server, config, Map.of(), Map.of(), Map.of()), codec, null);
    }

    public static PlayerNameManager getPlayerNameManager(MinecraftServer server, CustomNameConfig config) {
        return server.getWorld(World.OVERWORLD).getPersistentStateManager().getOrCreate(type(server, config));
    }

    public enum NameType {
        PREFIX("prefix", "customname.prefix", "Prefix"),
        SUFFIX("suffix", "customname.suffix", "Suffix"),
        NICKNAME("nickname", "customname.nick", "Nickname");

        private final String name;
        private final String permission;
        private final String displayName;

        NameType(String name, String permission, String displayName) {
            this.name = name;
            this.permission = permission;
            this.displayName = displayName;
        }

        public String getName() {
            return name;
        }

        public String getPermission() {
            return permission;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
