package xyz.eclipseisoffline.eclipsescustomname;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.function.Predicate;

@FunctionalInterface
public interface CustomNamePermissions {

    String ROOT_PERMISSION_NODE = "customname";

    default Predicate<CommandSourceStack> permissionCheck(String permission) {
        return source -> checkPermission(source, permission);
    }

    default boolean checkPermission(CommandSourceStack source, String permission) {
        if (CustomName.getConfig().requirePermissions()) {
            return hasPermission(source, getPermissionNode(permission), PermissionLevel.GAMEMASTERS);
        }
        return true;
    }

    default boolean hasPermission(CommandSourceStack source, String permission, PermissionLevel fallback) {
        return hasPermission(source, permission) || source.permissions().hasPermission(new Permission.HasCommandLevel(fallback));
    }

    boolean hasPermission(CommandSourceStack source, String permission);

    static String getPermissionNode(String node) {
        return ROOT_PERMISSION_NODE + "." + node;
    }
}
