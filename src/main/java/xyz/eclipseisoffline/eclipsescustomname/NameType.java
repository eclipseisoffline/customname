package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

public enum NameType implements StringIdentifiable {
    PREFIX("prefix", "prefixes", "prefix", "Prefix"),
    SUFFIX("suffix", "suffixes", "suffix", "Suffix"),
    NICKNAME("nickname", "nicknames", "nick", "Nickname");

    public static final Codec<NameType> CODEC = StringIdentifiable.createCodec(NameType::values);
    
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
    public String asString() {
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
