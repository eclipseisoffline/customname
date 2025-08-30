package xyz.eclipseisoffline.eclipsescustomname;

public class CustomNameUtil {
    private static final String ROOT_PERMISSION_NODE = "customname";

    public static String getPermissionNode(String node) {
        return ROOT_PERMISSION_NODE + "." + node;
    }
}
