package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.HoverEvent.Action;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class PlayerNameManager extends PersistentState {

    private static Type<PlayerNameManager> DATA_TYPE = new Type<>(PlayerNameManager::new,
            PlayerNameManager::loadFromNbt, null);
    private final Map<UUID, Text> playerPrefixes = new HashMap<>();
    private final Map<UUID, Text> playerSuffixes = new HashMap<>();
    private final Map<UUID, Text> playerNicknames = new HashMap<>();
    private final Map<UUID, Text> fullPlayerNames = new HashMap<>();

    public void updatePlayerPrefix(ServerPlayerEntity player, Text prefix) {
        playerPrefixes.put(player.getUuid(), prefix);
        markDirty(player);
    }

    public void updatePlayerSuffix(ServerPlayerEntity player, Text suffix) {
        playerSuffixes.put(player.getUuid(), suffix);
        markDirty(player);
    }

    public void updatePlayerNickname(ServerPlayerEntity player, Text nickname) {
        playerNicknames.put(player.getUuid(), nickname);
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
        Text prefix = playerPrefixes.get(player.getUuid());
        Text suffix = playerSuffixes.get(player.getUuid());
        Text nickname = playerNicknames.get(player.getUuid());

        MutableText name = Text.literal("");
        if (prefix != null) {
            name.append(prefix);
            name.append(" ");
        }
        if (nickname != null) {
            name.append(nickname.copy().styled(style -> style.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, player.getName()))));
        } else {
            name.append(Team.decorateName(player.getScoreboardTeam(), player.getName()));
        }
        if (suffix != null) {
            name.append(" ");
            name.append(suffix);
        }

        fullPlayerNames.put(player.getUuid(), name);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound prefixes = new NbtCompound();
        playerPrefixes.forEach((uuid, text) -> prefixes.putString(uuid.toString(), text.getString()));
        NbtCompound suffixes = new NbtCompound();
        playerSuffixes.forEach((uuid, text) -> suffixes.putString(uuid.toString(), text.getString()));
        NbtCompound nicknames = new NbtCompound();
        playerNicknames.forEach((uuid, text) -> nicknames.putString(uuid.toString(), text.getString()));

        nbt.put("prefixes", prefixes);
        nbt.put("suffixes", suffixes);
        nbt.put("nicknames", nicknames);
        return nbt;
    }

    public static PlayerNameManager loadFromNbt(NbtCompound nbtCompound) {
        PlayerNameManager playerNameManager = new PlayerNameManager();

        NbtCompound prefixes = nbtCompound.getCompound("prefixes");
        prefixes.getKeys().forEach(key -> playerNameManager.playerPrefixes.put(UUID.fromString(key),
                Text.of(prefixes.getString(key))));
        NbtCompound suffixes = nbtCompound.getCompound("suffixes");
        suffixes.getKeys().forEach(key -> playerNameManager.playerSuffixes.put(UUID.fromString(key),
                Text.of(suffixes.getString(key))));
        NbtCompound nicknames = nbtCompound.getCompound("nicknames");
        nicknames.getKeys().forEach(key -> playerNameManager.playerNicknames.put(UUID.fromString(key),
                Text.of(nicknames.getString(key))));

        return playerNameManager;
    }

    public static PlayerNameManager getPlayerNameManager(MinecraftServer server) {
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return persistentStateManager.getOrCreate(DATA_TYPE, CustomName.MOD_ID);
    }
}
