package xyz.eclipseisoffline.eclipsescustomname.fabric;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;
import xyz.eclipseisoffline.eclipsescustomname.CustomNamePermissions;

import java.nio.file.Path;
import java.util.function.Consumer;

public class CustomNameFabric extends CustomName implements ModInitializer {

    @Override
    public void onInitialize() {
        initialize();
    }

    @Override
    protected String getVersion() {
        return String.valueOf(FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata().getVersion());
    }

    @Override
    protected Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    protected CustomNamePermissions createPermissions() {
        return (_, _) -> false; // FIXME when permissions API works again
    }

    @Override
    protected void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> registerer) {
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> registerer.accept(dispatcher));
    }
}
