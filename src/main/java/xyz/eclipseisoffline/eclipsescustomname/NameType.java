package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

public enum NameType implements StringIdentifiable {
    PREFIX("prefix", "customname.prefix", "Prefix"),
    SUFFIX("suffix", "customname.suffix", "Suffix"),
    NICKNAME("nickname", "customname.nick", "Nickname");

    public static final Codec<NameType> CODEC = StringIdentifiable.createCodec(NameType::values);
    
    private final String name;
    private final String permission;
    private final String displayName;

    NameType(String name, String permission, String displayName) {
        this.name = name;
        this.permission = permission;
        this.displayName = displayName;
    }

    @Override
    public String asString() {
        return name;
    }

    @Deprecated(forRemoval = true)
    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public String getDisplayName() {
        return displayName;
    }
}
