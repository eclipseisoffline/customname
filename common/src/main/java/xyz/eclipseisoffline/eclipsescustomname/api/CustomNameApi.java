package xyz.eclipseisoffline.eclipsescustomname.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import xyz.eclipseisoffline.eclipsescustomname.NameType;
import xyz.eclipseisoffline.eclipsescustomname.PlayerNameManager;

/// Public API for reading player names managed by Custom Names.
public final class CustomNameApi {

    private CustomNameApi() {}

    /// Returns the player's name for the given {@link CustomNameType} key, or {@code null} if none was set.
    ///
    /// @param player the player to look up the name for
    /// @param type the {@link CustomNameType} key
    /// @return the player's name for that type, or {@code null} if none was set
    public static @Nullable Component getName(ServerPlayer player, ResourceKey<CustomNameType> type) {
        return PlayerNameManager.getInstance(player).getPlayerName(player, NameType.fromKey(type));
    }

    /// Returns the player's nickname, or their vanilla name if none was set.
    ///
    /// @param player the player to look up the nickname for
    /// @return the player's nickname, or their vanilla name if none was set
    public static Component getDisplayNickname(ServerPlayer player) {
        Component nickname = getName(player, CustomNameType.NICKNAME);
        return nickname == null ? player.getName() : nickname;
    }

    /// Returns the player's full display name, as used by Custom Names.
    ///
    /// @param player the player to look up the display name for
    /// @return the player's display name
    public static Component getFullName(ServerPlayer player) {
        return PlayerNameManager.getInstance(player).getFullPlayerName(player);
    }
}
