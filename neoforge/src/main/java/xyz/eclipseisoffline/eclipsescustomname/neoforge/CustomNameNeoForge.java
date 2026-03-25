package xyz.eclipseisoffline.eclipsescustomname.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;

import java.nio.file.Path;
import java.util.function.Consumer;

@Mod(CustomName.MOD_ID)
public class CustomNameNeoForge extends CustomName {
    private final String version;

    public CustomNameNeoForge(ModContainer mod) {
        version = mod.getModInfo().getVersion().getQualifier();

        initialize();
    }

    @Override
    protected String getVersion() {
        return version;
    }

    @Override
    protected Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    protected void registerCommands(Consumer<CommandDispatcher<CommandSourceStack>> registerer) {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> registerer.accept(event.getDispatcher()));
    }
}
