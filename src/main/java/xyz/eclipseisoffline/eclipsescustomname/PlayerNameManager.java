package xyz.eclipseisoffline.eclipsescustomname;

import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import net.luckperms.api.model.user.User;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class PlayerNameManager extends PersistentState {
    private final Map<UUID, Text> playerPrefixes = new HashMap<>();
    private final Map<UUID, Text> playerSuffixes = new HashMap<>();
    private final Map<UUID, Text> playerNicknames = new HashMap<>();
    private final Map<UUID, Text> fullPlayerNames = new HashMap<>();
    private final LuckPerms luckPerms;

    private PlayerNameManager(MinecraftServer server) {
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

    public boolean calculatedFullPlayerName(ServerPlayerEntity player) {
        return fullPlayerNames.containsKey(player.getUuid());
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
                    .argumentToText(permissionsPrefix,
                            CustomNameConfig.getInstance().formattingEnabled(),
                            true, false));
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
                    .argumentToText(permissionsSuffix,
                            CustomNameConfig.getInstance().formattingEnabled(),
                            true, false));
        }

        fullPlayerNames.put(player.getUuid(), name);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.put("prefixes", writeNames(playerPrefixes, registries));
        nbt.put("suffixes", writeNames(playerSuffixes, registries));
        nbt.put("nicknames", writeNames(playerNicknames, registries));
        return nbt;
    }

    private static NbtCompound writeNames(Map<UUID, Text> names, RegistryWrapper.WrapperLookup registries) {
        NbtCompound namesTag = new NbtCompound();
        names.forEach((uuid, text) -> {
            if (text != null) {
                namesTag.putString(uuid.toString(), Text.Serialization.toJsonString(text, registries));
            }
        });
        return namesTag;
    }

    public static PlayerNameManager loadFromNbt(NbtCompound nbtCompound,
            RegistryWrapper.WrapperLookup registries, MinecraftServer server) {
        PlayerNameManager playerNameManager = new PlayerNameManager(server);

        NbtCompound prefixes = nbtCompound.getCompound("prefixes");
        readNames(prefixes, playerNameManager.playerPrefixes, registries);
        NbtCompound suffixes = nbtCompound.getCompound("suffixes");
        readNames(suffixes, playerNameManager.playerSuffixes, registries);
        NbtCompound nicknames = nbtCompound.getCompound("nicknames");
        readNames(nicknames, playerNameManager.playerNicknames, registries);

        return playerNameManager;
    }

    private static void readNames(NbtCompound compound, Map<UUID, Text> nameMap,
            RegistryWrapper.WrapperLookup registries) {
        compound.getKeys().forEach(key -> {
            Text name;
            String raw = compound.getString(key);
            boolean old;
            try {
                old = !JsonParser.parseString(raw).isJsonObject();
            } catch (JsonParseException exception) {
                old = true;
            }
            if (old) {
                CustomName.LOGGER.info("Converting old name of " + key + " to new format");
                name = CustomName.argumentToText(raw.replaceAll("\"", "").replaceAll(
                                String.valueOf(Formatting.FORMATTING_CODE_PREFIX), "&"),
                        true, false, false);
            } else {
                name = Text.Serialization.fromJson(compound.getString(key), registries);
            }
            nameMap.put(UUID.fromString(key), name);
        });
    }

    public static PlayerNameManager getPlayerNameManager(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD)
                .getPersistentStateManager();
        return persistentStateManager.getOrCreate(new Type<>(
                () -> new PlayerNameManager(server),
                        (nbt, registries) -> loadFromNbt(nbt, registries, server), null),
                CustomName.MOD_ID);
    }

    public enum NameType {
        PREFIX("Prefix"),
        SUFFIX("Suffix"),
        NICKNAME("Nickname");

        private final String displayName;

        NameType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
