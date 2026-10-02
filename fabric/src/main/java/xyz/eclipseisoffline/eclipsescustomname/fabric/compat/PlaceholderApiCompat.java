package xyz.eclipseisoffline.eclipsescustomname.fabric.compat;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;
import xyz.eclipseisoffline.eclipsescustomname.NameType;
import xyz.eclipseisoffline.eclipsescustomname.PlayerNameManager;

public final class PlaceholderApiCompat {

    private PlaceholderApiCompat() {
    }

    public static void register() {
        register("prefix", NameType.PREFIX);
        register("nickname", NameType.NICKNAME);
        register("suffix", NameType.SUFFIX);
        register("luckperms_prefix", NameType.LUCKPERMS_PREFIX);
        register("luckperms_suffix", NameType.LUCKPERMS_SUFFIX);
        Placeholders.registerServer(CustomName.getModdedIdentifier("full_name"), (context, _) -> value(getPlayerName(context)));
    }

    private static void register(String id, NameType nameType) {
        Placeholders.registerServer(CustomName.getModdedIdentifier(id), (context, _) -> value(getPlayerName(context, nameType)));
    }

    private static @Nullable Component getPlayerName(ServerPlaceholderContext context, NameType nameType) {
        ServerPlayer player = context.serverPlayer();
        if (player == null) {
            return null;
        }
        return PlayerNameManager.getPlayerNameManager(context.server()).getPlayerName(player, nameType);
    }

    private static @Nullable Component getPlayerName(ServerPlaceholderContext context) {
        ServerPlayer player = context.serverPlayer();
        if (player == null) {
            return null;
        }
        return PlayerNameManager.getPlayerNameManager(context.server()).getFullPlayerName(player);
    }

    private static PlaceholderResult value(@Nullable Component value) {
        return PlaceholderResult.value(value == null ? Component.empty() : value);
    }
}
