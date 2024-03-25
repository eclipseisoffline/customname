package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.io.IOException;
import java.io.StringReader;
import java.util.function.Predicate;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.eclipseisoffline.eclipsescustomname.PlayerNameManager.NameType;

public class CustomName implements ModInitializer {

    public static final String MOD_ID = "eclipsescustomname";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final char FORMATTING_CODE = '&';
    private static final char HEX_CODE = '#';
    private static final int MAX_LENGTH = 20;
    private CustomNameConfig config;

    @Override
    public void onInitialize() {
        String modVersion = String.valueOf(
                FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata()
                        .getVersion());
        LOGGER.info("Custom Names " + modVersion + " initialising");
        LOGGER.info("Reading config");
        config = CustomNameConfig.getInstance();

        CommandRegistrationCallback.EVENT.register(
                ((dispatcher, registryAccess, environment) -> {
                    dispatcher.register(
                            CommandManager.literal("name")
                                    .requires(ServerCommandSource::isExecutedByPlayer)
                                    .then(CommandManager.literal("prefix")
                                            .requires(permissionCheck("customname.prefix"))
                                            .then(CommandManager.argument("name",
                                                            StringArgumentType.greedyString())
                                                    .executes(updatePlayerName(NameType.PREFIX))
                                            )
                                            .executes(clearPlayerName(NameType.PREFIX))
                                    )
                                    .then(CommandManager.literal("suffix")
                                            .requires(permissionCheck("customname.suffix"))
                                            .then(CommandManager.argument("name",
                                                            StringArgumentType.greedyString())
                                                    .executes(updatePlayerName(NameType.SUFFIX))
                                            )
                                            .executes(clearPlayerName(NameType.SUFFIX))
                                    )
                                    .then(CommandManager.literal("nickname")
                                            .requires(permissionCheck("customname.nick"))
                                            .then(CommandManager.argument("name",
                                                            StringArgumentType.greedyString())
                                                    .executes(updatePlayerName(NameType.NICKNAME))
                                            )
                                            .executes(clearPlayerName(NameType.NICKNAME))
                                    )
                    );

                    dispatcher.register(
                            CommandManager.literal("itemname")
                                    .requires(ServerCommandSource::isExecutedByPlayer)
                                    .requires(permissionCheck("customname.itemname"))
                                    .requires(source -> config.formattingEnabled())
                                    .then(CommandManager.argument("name",
                                                    StringArgumentType.greedyString())
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource()
                                                        .getPlayerOrThrow();

                                                ItemStack holding = player.getStackInHand(
                                                        Hand.MAIN_HAND);

                                                if (holding.isEmpty()) {
                                                    throw new SimpleCommandExceptionType(
                                                            Text.of("Must hold an item to name")).create();
                                                }

                                                Text argument = argumentToText(
                                                        StringArgumentType.getString(context,
                                                                "name"), true, true, true);
                                                if (Formatting.strip(argument.getString()).isEmpty()) {
                                                    throw new SimpleCommandExceptionType(
                                                            Text.of("Invalid item name")).create();
                                                }

                                                holding.setCustomName(argument);
                                                context.getSource().sendFeedback(
                                                        () -> Text.literal("Set item name to ")
                                                                .append(argument), true);

                                                return 0;
                                            })
                                    )
                    );
                }));
    }

    private Command<ServerCommandSource> updatePlayerName(PlayerNameManager.NameType nameType) {
        return context -> {
            ServerPlayerEntity player = context.getSource()
                    .getPlayerOrThrow();
            Text name;
            try {
                name = argumentToText(
                        StringArgumentType.getString(context, "name"));
            } catch (IllegalArgumentException exception) {
                throw new SimpleCommandExceptionType(Text.of(exception.getMessage())).create();
            }

            if (invalidNameArgument(name)) {
                throw new SimpleCommandExceptionType(Text.of("That name is invalid")).create();
            }

            PlayerNameManager.getPlayerNameManager(context.getSource().getServer())
                    .updatePlayerName(player, name, nameType);

            context.getSource().sendFeedback(
                    () -> Text.literal(nameType.getDisplayName() + " set to ")
                            .formatted(Formatting.GOLD)
                            .append(name), true);
            updateListName(player);
            return 0;
        };
    }

    private Command<ServerCommandSource> clearPlayerName(PlayerNameManager.NameType nameType) {
        return context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

            PlayerNameManager.getPlayerNameManager(context.getSource().getServer())
                    .updatePlayerName(player, null, nameType);

            context.getSource().sendFeedback(
                    () -> Text.literal(nameType.getDisplayName() + " cleared")
                            .formatted(Formatting.GOLD), true);
            updateListName(player);
            return 0;
        };
    }

    private boolean invalidNameArgument(Text argument) {
        String name = Formatting.strip(argument.getString());
        assert name != null;
        return name.isEmpty() || config.nameBlacklisted(name) || name.length() > MAX_LENGTH;
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

    private Text argumentToText(String argument) {
        return argumentToText(argument, config.formattingEnabled(), false, false);
    }

    public static Text argumentToText(String argument, boolean formattingEnabled,
            boolean spaceAllowed, boolean forceItalics) {
        if (!spaceAllowed) {
            argument = argument.split(" ")[0];
        }
        if (formattingEnabled) {
            MutableText complete = Text.empty();
            MutableText currentText = Text.empty();

            if (forceItalics) {
                currentText = currentText.styled(style -> style.withItalic(false));
            }

            StringReader argumentReader = new StringReader(argument);

            try {
                int c = argumentReader.read();
                boolean formatting = false;
                while (c != -1) {
                    char current = (char) c;

                    if (current == FORMATTING_CODE) {
                        if (formatting) {
                            throw new IllegalArgumentException("Invalid formatting code");
                        }
                        formatting = true;
                    } else if (current == HEX_CODE && formatting) {
                        formatting = false;
                        complete.append(currentText);
                        currentText = Text.empty();
                        if (forceItalics) {
                            currentText = currentText.styled(style -> style.withItalic(false));
                        }

                        char[] hexChars = new char[6];
                        int read = argumentReader.read(hexChars, 0, 6);
                        if (read < 6) {
                            throw new IllegalArgumentException("Invalid hex formatting code");
                        }

                        try {
                            int colour = Integer.parseInt(String.valueOf(hexChars), 16);
                            currentText.styled(style -> style.withColor(colour));
                        } catch (NumberFormatException exception) {
                            throw new IllegalArgumentException("Invalid hex formatting code",
                                    exception);
                        }
                    } else if (formatting) {
                        formatting = false;

                        Formatting newStyle = Formatting.byCode(current);
                        if (newStyle == null) {
                            throw new IllegalArgumentException("Invalid formatting code");
                        }
                        if (newStyle.isColor() || newStyle == Formatting.RESET) {
                            complete.append(currentText);
                            currentText = Text.empty();
                            if (forceItalics) {
                                currentText = currentText.styled(style -> style.withItalic(false));
                            }
                        }
                        currentText.styled(style -> style.withFormatting(newStyle));
                    } else {
                        currentText.append(String.valueOf(current));
                    }

                    c = argumentReader.read();
                }
            } catch (IOException exception) {
                throw new AssertionError(exception);
            }

            complete.append(currentText);
            return complete;
        }
        return Text.of(argument);
    }
}
