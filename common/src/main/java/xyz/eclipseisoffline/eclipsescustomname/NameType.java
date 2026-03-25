package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import xyz.eclipseisoffline.commonpermissionsapi.api.CommonPermissionNode;

public enum NameType implements StringRepresentable {
    PREFIX("prefix", "prefixes", CustomNamePermissions.PREFIX, "Prefix"),
    SUFFIX("suffix", "suffixes", CustomNamePermissions.SUFFIX, "Suffix"),
    NICKNAME("nickname", "nicknames", CustomNamePermissions.NICKNAME, "Nickname");

    public static final Codec<NameType> CODEC = StringRepresentable.fromEnum(NameType::values);
    
    private final String name;
    private final String plural;
    private final CommonPermissionNode permission;
    private final String displayName;

    NameType(String name, String plural, CommonPermissionNode permission, String displayName) {
        this.name = name;
        this.plural = plural;
        this.permission = permission;
        this.displayName = displayName;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String getPlural() {
        return plural;
    }

    public CommonPermissionNode getPermission() {
        return permission;
    }

    public String getDisplayName() {
        return displayName;
    }
}
