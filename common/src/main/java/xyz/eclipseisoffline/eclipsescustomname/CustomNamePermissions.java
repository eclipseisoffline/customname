package xyz.eclipseisoffline.eclipsescustomname;

import net.minecraft.resources.Identifier;
import xyz.eclipseisoffline.commonpermissionsapi.api.CommonPermissionNode;
import xyz.eclipseisoffline.commonpermissionsapi.api.CommonPermissions;

public interface CustomNamePermissions {

    CommonPermissionNode PREFIX = createNode("prefix");
    CommonPermissionNode SUFFIX = createNode("suffix");
    CommonPermissionNode NICKNAME = createNode("nickname");
    CommonPermissionNode ITEM_NAME = createNode("itemname");
    CommonPermissionNode ITEM_LORE = createNode("itemlore");
    CommonPermissionNode OTHER = createNode("other");
    CommonPermissionNode BYPASS_RESTRICTIONS = createNode("bypass_restrictions");

    String ROOT_PERMISSION_NODE = "customname";

    static CommonPermissionNode createNode(String name) {
        return CommonPermissions.node(Identifier.fromNamespaceAndPath(ROOT_PERMISSION_NODE, name));
    }

    static void bootstrap() {}
}
