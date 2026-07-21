package xyz.eclipseisoffline.eclipsescustomname.api;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import xyz.eclipseisoffline.eclipsescustomname.NameType;
import xyz.eclipseisoffline.eclipsescustomname.PlayerNameManager;

/**
 * Public API for reading player names managed by Custom Names.
 */
public final class CustomNameApi {

    private CustomNameApi() {
    }

    /**
     * Returns the player's Custom Names prefix, or {@code null} when no prefix is set.
     */
    public static @Nullable Component getPrefix(ServerPlayer player) {
        return getCustomName(player, NameType.PREFIX);
    }

    /**
     * Returns the player's Custom Names nickname, or {@code null} when no nickname is set.
     */
    public static @Nullable Component getNickname(ServerPlayer player) {
        return getCustomName(player, NameType.NICKNAME);
    }

    /**
     * Returns the player's Custom Names nickname, or their vanilla player name when no nickname is set.
     */
    public static Component getDisplayNickname(ServerPlayer player) {
        Component nickname = getNickname(player);
        return nickname == null ? player.getName() : nickname;
    }

    /**
     * Returns the player's Custom Names suffix, or {@code null} when no suffix is set.
     */
    public static @Nullable Component getSuffix(ServerPlayer player) {
        return getCustomName(player, NameType.SUFFIX);
    }

    /**
     * Returns the player's LuckPerms prefix, or {@code null} when LuckPerms is unavailable or no prefix is set.
     */
    public static @Nullable Component getLuckPermsPrefix(ServerPlayer player) {
        return getName(player, NameType.LUCKPERMS_PREFIX);
    }

    /**
     * Returns the player's LuckPerms suffix, or {@code null} when LuckPerms is unavailable or no suffix is set.
     */
    public static @Nullable Component getLuckPermsSuffix(ServerPlayer player) {
        return getName(player, NameType.LUCKPERMS_SUFFIX);
    }

    /**
     * Returns the player's full display name as used by Custom Names.
     */
    public static Component getFullName(ServerPlayer player) {
        return manager(player).getFullPlayerName(player);
    }

    private static @Nullable Component getCustomName(ServerPlayer player, NameType nameType) {
        return manager(player).getCustomPlayerName(player, nameType);
    }

    private static @Nullable Component getName(ServerPlayer player, NameType nameType) {
        return manager(player).getPlayerName(player, nameType);
    }

    private static PlayerNameManager manager(ServerPlayer player) {
        return PlayerNameManager.getPlayerNameManager(player.level().getServer());
    }
}
