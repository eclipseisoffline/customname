package xyz.eclipseisoffline.eclipsescustomname;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;

public class CustomNameUtil {
    private static final char FORMATTING_CODE = '&';
    private static final char HEX_CODE = '#';

    public static void updateListName(ServerPlayer player) {
        player.level().getServer().getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME, player));
    }

    public static void clearPlayerName(@Nullable CommandSourceStack source, ServerPlayer player, NameType nameType) {
        if (source == null) {
            source = player.createCommandSourceStack();
        }
        PlayerNameManager.getPlayerNameManager(source).updatePlayerName(player, null, nameType);

        source.sendSuccess(() -> Component.literal(nameType.getDisplayName() + " cleared").withStyle(ChatFormatting.GOLD), true);
        CustomNameUtil.updateListName(player);
    }

    public static Component playerNameArgumentToComponent(String argument, boolean spaceAllowed) {
        return nameArgumentToComponent(argument, CustomName.getConfig().formattingEnabled(), spaceAllowed, false);
    }

    public static Component nameArgumentToComponent(String argument, boolean formattingEnabled,
                                                    boolean spaceAllowed, boolean forceDisableItalics) {
        if (!spaceAllowed) {
            argument = argument.split(" ")[0];
        }
        if (formattingEnabled) {
            MutableComponent complete = Component.empty();

            StringReader argumentReader = new StringReader(argument);
            StringBuilder currentText = new StringBuilder();
            Style currentStyle = Style.EMPTY;
            if (forceDisableItalics) {
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
                        if (forceDisableItalics) {
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
                            if (forceDisableItalics) {
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
}
