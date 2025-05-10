package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Action;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.eclipseisoffline.eclipsescustomname.PlayerNameManager.NameType;
import xyz.eclipseisoffline.eclipsescustomname.network.FakeTextDisplayHolder;

public class CustomName implements ModInitializer {

    public static final String MOD_ID = "eclipsescustomname";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final char FORMATTING_CODE = '&';
    private static final char HEX_CODE = '#';
    private static CustomNameConfig config;

    @Override
    public void onInitialize() {
        String modVersion = String.valueOf(
                FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata()
                        .getVersion());
        LOGGER.info("Custom Names {} initialising", modVersion);
        LOGGER.info("Reading config");
        config = CustomNameConfig.readOrCreate();

        CommandRegistrationCallback.EVENT.register(
                ((dispatcher, registryAccess, environment) -> {
                    dispatcher.register(
                            CommandManager.literal("name")
                                    .then(CommandManager.literal("other")
                                            .then(otherPlayerNameCommand(NameType.PREFIX))
                                            .then(otherPlayerNameCommand(NameType.SUFFIX))
                                            .then(otherPlayerNameCommand(NameType.NICKNAME))
                                    )
                                    .then(playerNameCommand(NameType.PREFIX))
                                    .then(playerNameCommand(NameType.SUFFIX))
                                    .then(playerNameCommand(NameType.NICKNAME))
                    );

                    dispatcher.register(
                            CommandManager.literal("itemname")
                                    .requires(permissionCheck("customname.itemname").and(ServerCommandSource::isExecutedByPlayer).and(source -> config.formattingEnabled()))
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

                                                Text argument;
                                                try {
                                                    argument = argumentToText(StringArgumentType.getString(context, "name"),
                                                            true, true, true);
                                                } catch (IllegalArgumentException exception) {
                                                    throw new SimpleCommandExceptionType(Text.of(exception.getMessage())).create();
                                                }
                                                if (Formatting.strip(argument.getString()).isEmpty()) {
                                                    throw new SimpleCommandExceptionType(
                                                            Text.of("Invalid item name")).create();
                                                }

                                                holding.set(DataComponentTypes.CUSTOM_NAME, argument);
                                                player.setStackInHand(Hand.MAIN_HAND, holding);
                                                context.getSource().sendFeedback(
                                                        () -> Text.literal("Set item name to ")
                                                                .append(argument), true);

                                                return 0;
                                            })
                                    )
                                    .executes(resetItemComponent(DataComponentTypes.CUSTOM_NAME, "item name"))
                    );

                    dispatcher.register(
                            CommandManager.literal("itemlore")
                                    .requires(permissionCheck("customname.itemlore").and(ServerCommandSource::isExecutedByPlayer).and(source -> config.formattingEnabled()))
                                    .then(CommandManager.argument("lore",
                                                    StringArgumentType.greedyString())
                                            .executes(context -> {
                                                ServerPlayerEntity player = context.getSource()
                                                        .getPlayerOrThrow();

                                                ItemStack holding = player.getStackInHand(
                                                        Hand.MAIN_HAND);

                                                if (holding.isEmpty()) {
                                                    throw new SimpleCommandExceptionType(
                                                            Text.of("Must hold an item to set lore of")).create();
                                                }

                                                List<Text> arguments = new ArrayList<>();
                                                try {
                                                    List<String> lines = splitArgument(StringArgumentType.getString(context, "lore"));
                                                    for (String line : lines) {
                                                        Text argument = argumentToText(line, true, true, true);

                                                        if (Formatting.strip(argument.getString()).isEmpty()) {
                                                            throw new SimpleCommandExceptionType(
                                                                    Text.of("Invalid item lore")).create();
                                                        }
                                                        arguments.add(argument);
                                                    }
                                                } catch (IllegalArgumentException exception) {
                                                    throw new SimpleCommandExceptionType(Text.of(exception.getMessage())).create();
                                                }

                                                holding.set(DataComponentTypes.LORE, new LoreComponent(arguments));
                                                player.setStackInHand(Hand.MAIN_HAND, holding);
                                                context.getSource().sendFeedback(
                                                        () -> {
                                                            if (arguments.size() == 1) {
                                                                return Text.literal("Set item lore to ").append(arguments.get(0));
                                                            } else {
                                                                return Text.literal("Updated item lore");
                                                            }
                                                        }, true);

                                                return 0;
                                            })
                                    )
                                    .executes(resetItemComponent(DataComponentTypes.LORE, "item lore"))
                    );
                }));

        ServerLifecycleEvents.SERVER_STARTED.register(server -> server.getScoreboard().addUpdateListener(() -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ((FakeTextDisplayHolder) player).customName$updateName();
            }
        }));
    }

    private LiteralArgumentBuilder<ServerCommandSource> playerNameCommand(PlayerNameManager.NameType nameType) {
        return CommandManager.literal(nameType.getName())
                .requires(permissionCheck(nameType.getPermission()).and(ServerCommandSource::isExecutedByPlayer))
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                        .executes(updatePlayerName(nameType, false))
                )
                .executes(clearPlayerName(nameType, false));
    }

    private LiteralArgumentBuilder<ServerCommandSource> otherPlayerNameCommand(PlayerNameManager.NameType nameType) {
        return CommandManager.literal(nameType.getName())
                .requires(permissionCheck(nameType.getPermission()).and(permissionCheck("customname.other")))
                .then(CommandManager.argument("player", EntityArgumentType.player())
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(updatePlayerName(nameType, true))
                        )
                        .executes(clearPlayerName(nameType, true))
                );
    }

    private Command<ServerCommandSource> updatePlayerName(PlayerNameManager.NameType nameType, boolean other) {
        return context -> {
            ServerPlayerEntity player = other ? EntityArgumentType.getPlayer(context, "player") : context.getSource().getPlayerOrThrow();
            Text name;

            boolean bypassRestrictions = config.operatorsBypassRestrictions() && Permissions.check(context.getSource(), "customname.bypass_restrictions", 2);
            try {
                name = playerNameArgumentToText(StringArgumentType.getString(context, "name"), bypassRestrictions);
            } catch (IllegalArgumentException exception) {
                throw new SimpleCommandExceptionType(Text.of(exception.getMessage())).create();
            }

            if (invalidNameArgument(name, bypassRestrictions)) {
                throw new SimpleCommandExceptionType(Text.of("That name is invalid")).create();
            }

            PlayerNameManager.getPlayerNameManager(context.getSource().getServer(), config)
                    .updatePlayerName(player, name, nameType);

            context.getSource().sendFeedback(
                    () -> Text.literal(nameType.getDisplayName() + " set to ")
                            .formatted(Formatting.GOLD)
                            .append(name), true);
            updateListName(player);
            return 0;
        };
    }

    private Command<ServerCommandSource> clearPlayerName(PlayerNameManager.NameType nameType, boolean other) {
        return context -> {
            ServerPlayerEntity player = other ? EntityArgumentType.getPlayer(context, "player") : context.getSource().getPlayerOrThrow();

            PlayerNameManager.getPlayerNameManager(context.getSource().getServer(), config)
                    .updatePlayerName(player, null, nameType);

            context.getSource().sendFeedback(
                    () -> Text.literal(nameType.getDisplayName() + " cleared")
                            .formatted(Formatting.GOLD), true);
            updateListName(player);
            return 0;
        };
    }

    private Command<ServerCommandSource> resetItemComponent(ComponentType<?> component, String name) {
        return context -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            ItemStack holding = player.getStackInHand(Hand.MAIN_HAND);

            if (holding.isEmpty()) {
                throw new SimpleCommandExceptionType(
                        Text.of("Must hold an item to reset " + name + " of")).create();
            }

            holding.remove(component);
            player.setStackInHand(Hand.MAIN_HAND, holding);
            context.getSource().sendFeedback(() -> Text.literal("Reset " + name + " of item"), true);
            return 0;
        };
    }

    private boolean invalidNameArgument(Text argument, boolean bypassRestrictions) {
        String name = Formatting.strip(argument.getString());
        assert name != null;
        return name.isEmpty() || (!bypassRestrictions && (config.nameBlacklisted(name) || name.length() > config.maxNameLength()));
    }

    private Predicate<ServerCommandSource> permissionCheck(String permission) {
        if (config.requirePermissions()) {
            return Permissions.require(permission, 2);
        }
        return (source) -> true;
    }

    private Text playerNameArgumentToText(String argument, boolean spaceAllowed) {
        return argumentToText(argument, config.formattingEnabled(), spaceAllowed, false);
    }

    private static List<String> splitArgument(String argument) {
        boolean inBackslash = false;
        StringReader reader = new StringReader(argument);
        StringBuilder currentString = new StringBuilder();
        List<String> strings = new ArrayList<>();

        try {
            int c = reader.read();
            while (c != -1) {
                char current = (char) c;
                if (current == '\\') {
                    if (inBackslash) {
                        currentString.append('\\');
                        inBackslash = false;
                    } else {
                        inBackslash = true;
                    }
                } else if (inBackslash) {
                    if ((char) c == 'n') {
                        strings.add(currentString.toString());
                        currentString = new StringBuilder();
                    }
                    inBackslash = false;
                } else {
                    currentString.append(current);
                }
                c = reader.read();
            }
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }

        if (!currentString.isEmpty()) {
            strings.add(currentString.toString());
        }
        return strings;
    }

    public static Text argumentToText(String argument, boolean formattingEnabled,
            boolean spaceAllowed, boolean forceItalics) {
        if (!spaceAllowed) {
            argument = argument.split(" ")[0];
        }
        if (formattingEnabled) {
            MutableText complete = Text.empty();

            StringReader argumentReader = new StringReader(argument);
            StringBuilder currentText = new StringBuilder();
            Style currentStyle = Style.EMPTY;
            if (forceItalics) {
                currentStyle = currentStyle.withItalic(false);
            }

            try {
                int c = argumentReader.read();
                boolean formatting = false;
                boolean wasFormatting = false;
                while (c != -1) {
                    char current = (char) c;

                    if (current == FORMATTING_CODE) {
                        if (formatting) {
                            formatting = false;
                            wasFormatting = false;
                            currentText.append(current);
                        } else {
                            formatting = true;
                        }
                    } else if (current == HEX_CODE && formatting) {
                        formatting = false;
                        if (!currentText.isEmpty()) {
                            complete.append(Text.literal(currentText.toString()).setStyle(currentStyle));
                        }

                        currentText = new StringBuilder();
                        currentStyle = Style.EMPTY;
                        if (forceItalics) {
                            currentStyle = currentStyle.withItalic(false);
                        }

                        char[] hexChars = new char[6];
                        int read = argumentReader.read(hexChars, 0, 6);
                        if (read < 6) {
                            throw new IllegalArgumentException("Invalid hex formatting code");
                        }

                        try {
                            int colour = Integer.parseInt(String.valueOf(hexChars), 16);
                            currentStyle = currentStyle.withColor(colour);
                        } catch (NumberFormatException exception) {
                            throw new IllegalArgumentException("Invalid hex formatting code",
                                    exception);
                        }

                        wasFormatting = true;
                    } else if (formatting) {
                        formatting = false;

                        Formatting newStyle = Formatting.byCode(current);
                        if (newStyle == null) {
                            throw new IllegalArgumentException("Invalid formatting code");
                        }

                        if (newStyle.isColor() || newStyle == Formatting.RESET || !wasFormatting) {
                            if (!currentText.isEmpty()) {
                                complete.append(Text.literal(currentText.toString()).setStyle(currentStyle));
                            }

                            currentText = new StringBuilder();
                            currentStyle = Style.EMPTY;
                            if (forceItalics) {
                                currentStyle = currentStyle.withItalic(false);
                            }
                        }
                        wasFormatting = true;
                        currentStyle = currentStyle.withFormatting(newStyle);
                    } else {
                        wasFormatting = false;
                        currentText.append(current);
                    }

                    c = argumentReader.read();
                }
            } catch (IOException exception) {
                throw new AssertionError(exception);
            }

            if (!currentText.isEmpty()) {
                complete.append(Text.literal(currentText.toString()).setStyle(currentStyle));
            }
            return complete;
        }
        return Text.of(argument);
    }

    public static void updateListName(ServerPlayerEntity player) {
        assert player.getServer() != null;
        player.getServer().getPlayerManager()
                .sendToAll(new PlayerListS2CPacket(Action.UPDATE_DISPLAY_NAME, player));
    }

    public static CustomNameConfig getConfig() {
        return config;
    }
}
