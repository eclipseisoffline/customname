package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum NameType implements StringRepresentable {
    PREFIX("prefix", "prefixes", "prefix", "Prefix"),
    SUFFIX("suffix", "suffixes", "suffix", "Suffix"),
    NICKNAME("nickname", "nicknames", "nick", "Nickname");

    public static final Codec<NameType> CODEC = StringRepresentable.fromEnum(NameType::values);
    
    private final String name;
    private final String plural;
    private final String permission;
    private final String displayName;

    NameType(String name, String plural, String permission, String displayName) {
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

    public String getPermission() {
        return CustomNameUtil.getPermissionNode(permission);
    }

    public String getDisplayName() {
        return displayName;
    }
}
