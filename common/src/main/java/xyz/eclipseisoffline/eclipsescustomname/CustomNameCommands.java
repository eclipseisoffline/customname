package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class CustomNameCommands {
    public static final Identifier CLEAR_NAME_EVENT = CustomName.getModdedIdentifier("clear_name");

    private static final String NAME_COMMAND_ROOT = "name";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal(NAME_COMMAND_ROOT)
                        .then(Commands.literal("other")
                                .requires(permissionCheck("other"))
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
                        .requires(permissionCheck("itemname")
                                .and(CommandSourceStack::isPlayer)
                                .and(_ -> CustomName.getConfig().formattingEnabled()))
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();

                                    ItemStack holding = player.getItemInHand(InteractionHand.MAIN_HAND);
                                    if (holding.isEmpty()) {
                                        throw new SimpleCommandExceptionType(Component.literal("Must hold an item to name")).create();
                                    }

                                    Component argument;
                                    try {
                                        argument = CustomNameUtil.nameArgumentToComponent(StringArgumentType.getString(context, "name"),
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
                        .requires(permissionCheck("itemlore")
                                .and(CommandSourceStack::isPlayer)
                                .and(_ -> CustomName.getConfig().formattingEnabled()))
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
                                            Component argument = CustomNameUtil.nameArgumentToComponent(line, true, true, true);

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

    private static LiteralArgumentBuilder<CommandSourceStack> playerNameCommand(NameType nameType) {
        return Commands.literal(nameType.getSerializedName())
                .requires(permissionCheck(nameType.getPermission())
                        .or(CustomName.getConfig().groups().partOfGroup(nameType))
                        .and(CommandSourceStack::isPlayer))
                .then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests(CustomName.getConfig().groups().createSuggestionsProvider(nameType))
                        .executes(updatePlayerName(nameType, false))
                )
                .executes(menuOrClearPlayerName(nameType, false));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> otherPlayerNameCommand(NameType nameType) {
        return Commands.literal(nameType.getSerializedName())
                .requires(permissionCheck(nameType.getPermission()).and(permissionCheck("other")))
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(updatePlayerName(nameType, true))
                        )
                        .executes(menuOrClearPlayerName(nameType, true))
                );
    }

    private static Command<CommandSourceStack> updatePlayerName(NameType nameType, boolean other) {
        return context -> {
            ServerPlayer player = other ? EntityArgument.getPlayer(context, "player") : context.getSource().getPlayerOrException();
            Component name;

            boolean bypassRestrictions = CustomName.getConfig().operatorsBypassRestrictions() && checkPermission(context.getSource(), "bypass_restrictions");
            try {
                name = CustomNameUtil.playerNameArgumentToComponent(StringArgumentType.getString(context, "name"), bypassRestrictions);
            } catch (IllegalArgumentException exception) {
                throw new SimpleCommandExceptionType(Component.nullToEmpty(exception.getMessage())).create();
            }

            if (invalidNameArgument(context.getSource(), nameType, name, bypassRestrictions)) {
                throw new SimpleCommandExceptionType(Component.nullToEmpty("That name is invalid")).create();
            }

            PlayerNameManager.getPlayerNameManager(context.getSource()).updatePlayerName(player, name, nameType);

            context.getSource().sendSuccess(
                    () -> Component.literal(nameType.getDisplayName() + " set to ")
                            .withStyle(ChatFormatting.GOLD)
                            .append(name), true);
            CustomNameUtil.updateListName(player);
            return 0;
        };
    }

    private static Command<CommandSourceStack> menuOrClearPlayerName(NameType nameType, boolean other) {
        return context -> {
            if (!other && CustomName.getConfig().groups().partOfGroup(nameType).test(context.getSource())) {
                displayNameMenu(context.getSource(), nameType);
                return 0;
            }

            ServerPlayer player = other ? EntityArgument.getPlayer(context, "player") : context.getSource().getPlayerOrException();
            CustomNameUtil.clearPlayerName(context.getSource(), player, nameType);
            return 0;
        };
    }

    private static void displayNameMenu(CommandSourceStack source, NameType nameType) throws CommandSyntaxException {
        List<MutableComponent> menu = new ArrayList<>();
        menu.add(Component.literal("You have the following " + nameType.getPlural() + " available to you:"));

        PlayerNameManager manager = PlayerNameManager.getPlayerNameManager(source);
        List<ParsedPlayerName> available = CustomName.getConfig().groups().validNames(source, nameType);
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

    private static Command<CommandSourceStack> resetItemDataComponent(DataComponentType<?> component, String name) {
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

    private static boolean invalidNameArgument(CommandSourceStack source, NameType nameType, Component argument, boolean bypassRestrictions) {
        if (!checkPermission(source, nameType.getPermission()) && !CustomName.getConfig().groups().validName(source, nameType, argument)) {
            return true;
        }

        String name = ChatFormatting.stripFormatting(argument.getString());
        return name.isEmpty() || (!bypassRestrictions && (CustomName.getConfig().nameBlacklisted(name) || name.length() > CustomName.getConfig().maxNameLength()));
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

    private static Predicate<CommandSourceStack> permissionCheck(String permission) {
        return CustomName.getPermissions().permissionCheck(permission);
    }

    private static boolean checkPermission(CommandSourceStack source, String permission) {
        return CustomName.getPermissions().checkPermission(source, permission);
    }
}
