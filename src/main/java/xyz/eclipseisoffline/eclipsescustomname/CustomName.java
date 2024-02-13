package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.function.Predicate;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomName implements ModInitializer {

    public static final String MOD_ID = "eclipsescustomname";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private CustomNameConfig config;

    @Override
    public void onInitialize() {
        String modVersion = String.valueOf(
                FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata().getVersion());
        LOGGER.info("Custom Names " + modVersion + " initialising");
        LOGGER.info("Reading config");
        config = CustomNameConfig.getInstance();

        CommandRegistrationCallback.EVENT.register(
                ((dispatcher, registryAccess, environment) -> dispatcher.register(
                        CommandManager.literal("name")
                                .requires(ServerCommandSource::isExecutedByPlayer)
                                .then(CommandManager.literal("prefix")
                                        .requires(permissionCheck("customname.prefix"))
                                        .then(CommandManager.argument("prefix",
                                                        StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = context.getSource()
                                                            .getPlayer();
                                                    Text prefix = argumentToText(
                                                            StringArgumentType.getString(context,
                                                                    "prefix"));

                                                    if (invalidNameArgument(prefix)) {
                                                        context.getSource().sendError(Text.of("That prefix is invalid"));
                                                        return 1;
                                                    }

                                                    assert player != null;
                                                    PlayerNameManager.getPlayerNameManager(
                                                                    context.getSource().getServer())
                                                            .updatePlayerPrefix(player, prefix);

                                                    context.getSource().sendFeedback(
                                                            () -> Text.literal("Prefix set to ")
                                                                    .formatted(Formatting.GOLD)
                                                                    .append(prefix), true);
                                                    updateListName(player);
                                                    return 0;
                                                }))
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource()
                                                    .getPlayer();

                                            assert player != null;
                                            PlayerNameManager.getPlayerNameManager(
                                                            context.getSource().getServer())
                                                    .updatePlayerPrefix(player, null);

                                            context.getSource().sendFeedback(
                                                    () -> Text.literal("Prefix cleared")
                                                            .formatted(Formatting.GOLD), true);
                                            updateListName(player);
                                            return 0;
                                        })
                                )
                                .then(CommandManager.literal("suffix")
                                        .requires(permissionCheck("customname.suffix"))
                                        .then(CommandManager.argument("suffix",
                                                        StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = context.getSource()
                                                            .getPlayer();
                                                    Text suffix = argumentToText(
                                                            StringArgumentType.getString(context,
                                                                    "suffix"));

                                                    if (invalidNameArgument(suffix)) {
                                                        context.getSource().sendError(Text.of("That suffix is invalid"));
                                                        return 1;
                                                    }

                                                    assert player != null;
                                                    PlayerNameManager.getPlayerNameManager(
                                                                    context.getSource().getServer())
                                                            .updatePlayerSuffix(player, suffix);

                                                    context.getSource().sendFeedback(
                                                            () -> Text.literal("Suffix set to ")
                                                                    .formatted(Formatting.GOLD)
                                                                    .append(suffix), true);
                                                    updateListName(player);
                                                    return 0;
                                                }))
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource()
                                                    .getPlayer();

                                            assert player != null;
                                            PlayerNameManager.getPlayerNameManager(
                                                            context.getSource().getServer())
                                                    .updatePlayerSuffix(player, null);

                                            context.getSource().sendFeedback(
                                                    () -> Text.literal("Suffix cleared")
                                                            .formatted(Formatting.GOLD), true);
                                            updateListName(player);
                                            return 0;
                                        })
                                )
                                .then(CommandManager.literal("nickname")
                                        .requires(permissionCheck("customname.nick"))
                                        .then(CommandManager.argument("nickname",
                                                        StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayerEntity player = context.getSource()
                                                            .getPlayer();
                                                    Text nickname = argumentToText(
                                                            StringArgumentType.getString(context,
                                                                    "nickname"));

                                                    if (invalidNameArgument(nickname)) {
                                                        context.getSource().sendError(Text.of("That nickname is invalid"));
                                                        return 1;
                                                    }

                                                    assert player != null;
                                                    PlayerNameManager.getPlayerNameManager(
                                                                    context.getSource().getServer())
                                                            .updatePlayerNickname(player, nickname);

                                                    context.getSource().sendFeedback(
                                                            () -> Text.literal("Nickname set to ")
                                                                    .formatted(Formatting.GOLD)
                                                                    .append(nickname), true);
                                                    updateListName(player);
                                                    return 0;
                                                }))
                                        .executes(context -> {
                                            ServerPlayerEntity player = context.getSource()
                                                    .getPlayer();

                                            assert player != null;
                                            PlayerNameManager.getPlayerNameManager(
                                                            context.getSource().getServer())
                                                    .updatePlayerNickname(player, null);

                                            context.getSource().sendFeedback(
                                                    () -> Text.literal("Nickname cleared")
                                                            .formatted(Formatting.GOLD), true);
                                            updateListName(player);
                                            return 0;
                                        })
                                )
                )));
    }

    private Text argumentToText(String argument) {
        argument = argument.split(" ")[0];
        if (config.formattingEnabled()) {
            argument = argument.replaceAll("&", String.valueOf(
                    Formatting.FORMATTING_CODE_PREFIX));
            argument += Formatting.FORMATTING_CODE_PREFIX + "r";
        }
        return Text.of(argument);
    }

    private boolean invalidNameArgument(Text argument) {
        String name = Formatting.strip(argument.getString());
        assert name != null;
        return name.isEmpty() || config.nameBlacklisted(name);
    }

    private Predicate<ServerCommandSource> permissionCheck(String permission) {
        if (config.requirePermissions()) {
            return Permissions.require(permission, 2);
        }
        return (source) -> true;
    }

    private void updateListName(ServerPlayerEntity player) {
        assert player.getServer() != null;
        player.getServer().getPlayerManager()
                .sendToAll(new PlayerListS2CPacket(Action.UPDATE_DISPLAY_NAME, player));
    }
}
