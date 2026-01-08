package xyz.eclipseisoffline.eclipsescustomname.fabric;

import net.minecraft.commands.CommandSourceStack;
import xyz.eclipseisoffline.eclipsescustomname.CustomNamePermissions;

public final class CustomNameFabricPermissions implements CustomNamePermissions {

    @Override
    public boolean hasPermission(CommandSourceStack source, String permission) {
        return false;
        //return Permissions.check(source, permission); - not for deobfuscated versions yet
    }
}
