package xyz.eclipseisoffline.eclipsescustomname.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;

public class CustomNameFabric extends CustomName implements ModInitializer {

    @Override
    public void onInitialize() {
        initialize();
        CommandRegistrationCallback.EVENT.register((dispatcher, _, _) -> registerCommands(dispatcher));
    }

    @Override
    protected String getVersion() {
        return String.valueOf(FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().getMetadata().getVersion());
    }

    @Override
    protected boolean hasPermission(CommandSourceStack source, String permission, PermissionLevel fallback) {
        //return Permissions.check(source, permission, fallback);
        return source.permissions().hasPermission(new Permission.HasCommandLevel(fallback));
        return super.hasPermission(source, permission, fallback);
    }
}
