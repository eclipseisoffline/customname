package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.Consumer;

public abstract class CustomName {
    public static final String MOD_ID = "eclipsescustomname";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean initialized = false;
    private static CustomNameConfig config;
    private static CustomNamePermissions permissions;

    public void initialize() {
        if (initialized) {
            throw new IllegalStateException("Tried to initialise Custom Names twice!");
        }
        initialized = true;

        LOGGER.info("Custom Names {} initialising", getVersion());
        LOGGER.info("Reading config");
        config = CustomNameConfig.readOrCreate(getConfigDir());
        permissions = createPermissions();

        registerCommands(CustomNameCommands::register);
    }

    protected abstract String getVersion();

    protected abstract Path getConfigDir();

    protected abstract CustomNamePermissions createPermissions();

    protected abstract void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> registerer);

    public static CustomNameConfig getConfig() {
        return config;
    }

    public static CustomNamePermissions getPermissions() {
        return permissions;
    }
}
