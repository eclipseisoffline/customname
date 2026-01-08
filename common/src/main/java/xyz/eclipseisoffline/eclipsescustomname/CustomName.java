package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Action;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO clean this up, commands to different class
public abstract class CustomName {

    public static final String MOD_ID = "eclipsescustomname";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier CLEAR_NAME_EVENT = Identifier.fromNamespaceAndPath(MOD_ID, "clear_name");

    private static final String NAME_COMMAND_ROOT = "name";
    private static final char FORMATTING_CODE = '&';
    private static final char HEX_CODE = '#';

    private static boolean initialized = false;
    private static CustomNameConfig config;
    private static CustomNamePermissions permissions;

    public void initialize() {
        if (initialized) {
            throw new IllegalStateException("Tried to initialise CustomName twice!");
        }
        initialized = true;

        LOGGER.info("Custom Names {} initialising", getVersion());
        LOGGER.info("Reading config");
        config = CustomNameConfig.readOrCreate();
    }

    protected void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(NAME_COMMAND_ROOT)
                        .then(Commands.literal("other")
                                .requires(permissionCheck("customname.other"))
                                .then(otherPlayerNameCommand(NameType.PREFIX))
                                .then(otherPlayerNameCommand(NameType.SUFFIX))
                                .then(otherPlayerNameCommand(NameType.NICKNAME))
                        )
                        .then(playerNameCommand(NameType.PREFIX))
                        .then(playerNameCommand(NameType.SUFFIX))
                        .then(playerNameCommand(NameType.NICKNAME))
        );

        dispatcher.register(
                Commands.literal("itemname")
                        .requires(permissionCheck("customname.itemname").and(CommandSourceStack::isPlayer).and(source -> config.formattingEnabled()))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    ItemStack holding = player.getItemInHand(InteractionHand.MAIN_HAND);
                                    if (holding.isEmpty()) {
                                        throw new SimpleCommandExceptionType(Component.literal("Must hold an item to name")).create();
                                    }

                                    Component argument;
                                    try {
                                        argument = argumentToComponent(StringArgumentType.getString(context, "name"),
                                                true, true, true);
                                    } catch (IllegalArgumentException exception) {
                                        throw new SimpleCommandExceptionType(Component.literal(exception.getMessage())).create();
                                    }
                                    if (ChatFormatting.stripFormatting(argument.getString()).isEmpty()) {
                                        throw new SimpleCommandExceptionType(Component.literal("Invalid item name")).create();
                                    }

                                    holding.set(DataComponents.CUSTOM_NAME, argument);
                                    player.setItemInHand(InteractionHand.MAIN_HAND, holding);
                                    context.getSource().sendSuccess(() -> Component.literal("Set item name to ").append(argument), true);

                                    return 0;
                                })
                        )
                        .executes(resetItemDataComponent(DataComponents.CUSTOM_NAME, "item name"))
        );

        dispatcher.register(
                Commands.literal("itemlore")
                        .requires(permissionCheck("customname.itemlore").and(CommandSourceStack::isPlayer).and(source -> config.formattingEnabled()))
                        .then(Commands.argument("lore", StringArgumentType.greedyString())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    ItemStack holding = player.getItemInHand(InteractionHand.MAIN_HAND);

                                    if (holding.isEmpty()) {
                                        throw new SimpleCommandExceptionType(Component.nullToEmpty("Must hold an item to set lore of")).create();
                                    }

                                    List<Component> arguments = new ArrayList<>();
                                    try {
                                        List<String> lines = splitArgument(StringArgumentType.getString(context, "lore"));
                                        for (String line : lines) {
                                            Component argument = argumentToComponent(line, true, true, true);

                                            if (ChatFormatting.stripFormatting(argument.getString()).isEmpty()) {
                                                throw new SimpleCommandExceptionType(Component.nullToEmpty("Invalid item lore")).create();
                                            }
                                            arguments.add(argument);
                                        }
                                    } catch (IllegalArgumentException exception) {
                                        throw new SimpleCommandExceptionType(Component.nullToEmpty(exception.getMessage())).create();
                                    }

                                    holding.set(DataComponents.LORE, new ItemLore(arguments));
                                    player.setItemInHand(InteractionHand.MAIN_HAND, holding);
                                    context.getSource().sendSuccess(() -> {
                                        if (arguments.size() == 1) {
                                            return Component.literal("Set item lore to ").append(arguments.getFirst());
                                        } else {
                                            return Component.literal("Updated item lore");
                                        }
                                    }, true);

                                    return 0;
                                })
                        )
                        .executes(resetItemDataComponent(DataComponents.LORE, "item lore"))
        );
    }

    protected abstract String getVersion();

    private LiteralArgumentBuilder<CommandSourceStack> playerNameCommand(NameType nameType) {
        return Commands.literal(nameType.getSerializedName())
                .requires(permissionCheck(nameType.getPermission())
                        .or(config.groups().partOfGroup(nameType))
                        .and(CommandSourceStack::isPlayer))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(config.groups().createSuggestionsProvider(nameType))
                        .executes(updatePlayerName(nameType, false))
                )
                .executes(menuOrClearPlayerName(nameType, false));
    }

    private LiteralArgumentBuilder<CommandSourceStack> otherPlayerNameCommand(NameType nameType) {
        return Commands.literal(nameType.getSerializedName())
                .requires(permissionCheck(nameType.getPermission()).and(permissionCheck("customname.other")))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(updatePlayerName(nameType, true))
                        )
                        .executes(menuOrClearPlayerName(nameType, true))
                );
    }

    private Command<CommandSourceStack> updatePlayerName(NameType nameType, boolean other) {
        return context -> {
            ServerPlayer player = other ? EntityArgument.getPlayer(context, "player") : context.getSource().getPlayerOrException();
            Component name;

            boolean bypassRestrictions = config.operatorsBypassRestrictions() && checkPermission(context.getSource(), "customname.bypass_restrictions");
            try {
                name = playerNameArgumentToComponent(StringArgumentType.getString(context, "name"), bypassRestrictions);
            } catch (IllegalArgumentException exception) {
                throw new SimpleCommandExceptionType(Component.nullToEmpty(exception.getMessage())).create();
            }

            if (invalidNameArgument(context.getSource(), nameType, name, bypassRestrictions)) {
                throw new SimpleCommandExceptionType(Component.nullToEmpty("That name is invalid")).create();
            }

            PlayerNameManager.getPlayerNameManager(context.getSource().getServer(), config)
                    .updatePlayerName(player, name, nameType);

            context.getSource().sendSuccess(
                    () -> Component.literal(nameType.getDisplayName() + " set to ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(name), true);
            updateListName(player);
            return 0;
        };
    }

    private Command<CommandSourceStack> menuOrClearPlayerName(NameType nameType, boolean other) {
        return context -> {
            if (!other && config.groups().partOfGroup(nameType).test(context.getSource())) {
                displayNameMenu(context.getSource(), nameType);
                return 0;
            }

            ServerPlayer player = other ? EntityArgument.getPlayer(context, "player") : context.getSource().getPlayerOrException();
            clearPlayerName(context.getSource(), player, nameType);
            return 0;
        };
    }

    private void displayNameMenu(CommandSourceStack source, NameType nameType) throws CommandSyntaxException {
        List<MutableComponent> menu = new ArrayList<>();
        menu.add(Component.literal("You have the following " + nameType.getPlural() + " available to you:"));

        PlayerNameManager manager = PlayerNameManager.getPlayerNameManager(source.getServer(), config);
        List<ParsedPlayerName> available = config.groups().validNames(source, nameType);
        for (ParsedPlayerName name : available) {
            menu.add(Component.literal("- ")
                    .append(name.parsed())
                    .append(name.parsed().equals(manager.getPlayerName(source.getPlayerOrException(), nameType)) ? " (current)" : "")
                    .withStyle(style -> style.withClickEvent(
                            new ClickEvent.RunCommand("/" + NAME_COMMAND_ROOT + " " + nameType.getSerializedName() + " " + name.raw()))));
        }
        menu.add(Component.empty());
        menu.add(Component.literal("Click a listed " + nameType.getSerializedName() + " to apply it"));
        menu.add(Component.literal("Click here to clear your " + nameType.getSerializedName())
                .withStyle(style -> style.withClickEvent(
                        new ClickEvent.Custom(CLEAR_NAME_EVENT, Optional.of(NameType.CODEC.encodeStart(NbtOps.INSTANCE, nameType).getOrThrow())))));

        menu.stream()
                .map(line -> line.withStyle(style -> style.withColor(ChatFormatting.GOLD)))
                .forEach(line -> source.sendSuccess(() -> line, false));
    }

    private Command<CommandSourceStack> resetItemDataComponent(DataComponentType<?> component, String name) {
        return context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ItemStack holding = player.getItemInHand(InteractionHand.MAIN_HAND);

            if (holding.isEmpty()) {
                throw new SimpleCommandExceptionType(Component.nullToEmpty("Must hold an item to reset " + name + " of")).create();
            }

            holding.remove(component);
            player.setItemInHand(InteractionHand.MAIN_HAND, holding);
            context.getSource().sendSuccess(() -> Component.literal("Reset " + name + " of item"), true);
            return 0;
        };
    }

    private boolean invalidNameArgument(CommandSourceStack source, NameType nameType, Component argument, boolean bypassRestrictions) {
        if (!checkPermission(source, nameType.getPermission()) && !config.groups().validName(source, nameType, argument)) {
            return true;
        }

        String name = ChatFormatting.stripFormatting(argument.getString());
        return name.isEmpty() || (!bypassRestrictions && (config.nameBlacklisted(name) || name.length() > config.maxNameLength()));
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

    public static void clearPlayerName(CommandSourceStack source, ServerPlayer player, NameType nameType) {
        if (source == null) {
            source = player.createCommandSourceStack();
        }
        PlayerNameManager.getPlayerNameManager(source.getServer(), config).updatePlayerName(player, null, nameType);

        source.sendSuccess(() -> Component.literal(nameType.getDisplayName() + " cleared").withStyle(ChatFormatting.GOLD), true);
        updateListName(player);
    }

    public static Component playerNameArgumentToComponent(String argument, boolean spaceAllowed) {
        return argumentToComponent(argument, config.formattingEnabled(), spaceAllowed, false);
    }

    public static Component argumentToComponent(String argument, boolean formattingEnabled,
                                                boolean spaceAllowed, boolean forceItalics) {
        if (!spaceAllowed) {
            argument = argument.split(" ")[0];
        }
        if (formattingEnabled) {
            MutableComponent complete = Component.empty();

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
                            complete.append(Component.literal(currentText.toString()).setStyle(currentStyle));
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

                        ChatFormatting newStyle = ChatFormatting.getByCode(current);
                        if (newStyle == null) {
                            throw new IllegalArgumentException("Invalid formatting code");
                        }

                        if (newStyle.isColor() || newStyle == ChatFormatting.RESET || !wasFormatting) {
                            if (!currentText.isEmpty()) {
                                complete.append(Component.literal(currentText.toString()).setStyle(currentStyle));
                            }

                            currentText = new StringBuilder();
                            currentStyle = Style.EMPTY;
                            if (forceItalics) {
                                currentStyle = currentStyle.withItalic(false);
                            }
                        }
                        wasFormatting = true;
                        currentStyle = currentStyle.applyFormat(newStyle);
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
                complete.append(Component.literal(currentText.toString()).setStyle(currentStyle));
            }
            return complete;
        }
        return Component.nullToEmpty(argument);
    }

    public static void updateListName(ServerPlayer player) {
        player.level().getServer().getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(Action.UPDATE_DISPLAY_NAME, player));
    }

    public static CustomNameConfig getConfig() {
        return config;
    }

    public static CustomNamePermissions getPermissions() {
        return permissions;
    }
}
