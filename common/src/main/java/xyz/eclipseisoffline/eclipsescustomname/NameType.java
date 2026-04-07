package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;
import xyz.eclipseisoffline.commonpermissionsapi.api.CommonPermissionNode;

public enum NameType implements StringRepresentable {
    PREFIX("prefix", "prefixes", CustomNamePermissions.PREFIX, "Prefix"),
    SUFFIX("suffix", "suffixes", CustomNamePermissions.SUFFIX, "Suffix"),
    NICKNAME("nickname", "nicknames", CustomNamePermissions.NICKNAME, "Nickname"),
    LUCKPERMS_PREFIX("luckperms_prefix"),
    LUCKPERMS_SUFFIX("luckperms_suffix");

    public static final Codec<NameType> CODEC = StringRepresentable.fromEnum(NameType::values);
    
    private final String name;
    private final @Nullable String plural;
    private final @Nullable CommonPermissionNode permission;
    private final boolean showInCommands;
    private final @Nullable String displayName;

    NameType(String name, String plural, CommonPermissionNode permission, String displayName) {
        this.name = name;
        this.plural = plural;
        this.permission = permission;
        this.showInCommands = true;
        this.displayName = displayName;
    }

    NameType(String name) {
        this.name = name;
        this.showInCommands = false;
        this.plural = null;
        this.permission = null;
        this.displayName = null;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String getPlural() {
        return ensureShownInCommands(plural);
    }

    public CommonPermissionNode getPermission() {
        return ensureShownInCommands(permission);
    }

    public String getDisplayName() {
        return ensureShownInCommands(displayName);
    }

    private <T> T ensureShownInCommands(@Nullable T value) {
        if (!showInCommands) {
            throw new IllegalStateException(this + " is not shown in commands");
        }
        assert value != null;
        return value;
    }
}
