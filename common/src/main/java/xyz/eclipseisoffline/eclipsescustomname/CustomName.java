package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.Consumer;

public abstract class CustomName {
    public static final String MOD_ID = "eclipsescustomname";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean initialized = false;
    @Nullable
    private static CustomNameConfig config = null;

    public void initialize() {
        if (initialized) {
            throw new IllegalStateException("Tried to initialise Custom Names twice!");
        }
        initialized = true;

        LOGGER.info("Custom Names {} initialising", getVersion());
        CustomNamePermissions.bootstrap();
        LOGGER.info("Reading config");
        config = CustomNameConfig.readOrCreate(getConfigDir());

        registerCommands(CustomNameCommands::register);
    }

    protected abstract String getVersion();

    protected abstract Path getConfigDir();

    protected abstract void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> registerer);

    public static CustomNameConfig getConfig() {
        if (config == null) {
            throw new NullPointerException("CustomNameConfig was accessed before it was initialized!");
        }
        return config;
    }

    public static Identifier getModdedIdentifier(String path) {
        return Identifier.fromNamespaceAndPath(CustomName.MOD_ID, path);
    }
}
