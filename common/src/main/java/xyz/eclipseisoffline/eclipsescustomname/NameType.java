package xyz.eclipseisoffline.eclipsescustomname;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.Nullable;
import xyz.eclipseisoffline.commonpermissionsapi.api.CommonPermissionNode;
import xyz.eclipseisoffline.eclipsescustomname.api.CustomNameType;

public enum NameType implements StringRepresentable {
    PREFIX(CustomNameType.PREFIX, "prefixes", CustomNamePermissions.PREFIX, "Prefix"),
    SUFFIX(CustomNameType.SUFFIX, "suffixes", CustomNamePermissions.SUFFIX, "Suffix"),
    NICKNAME(CustomNameType.NICKNAME, "nicknames", CustomNamePermissions.NICKNAME, "Nickname"),
    LUCKPERMS_PREFIX(CustomNameType.LUCKPERMS_PREFIX),
    LUCKPERMS_SUFFIX(CustomNameType.LUCKPERMS_SUFFIX);

    public static final Codec<NameType> CODEC = StringRepresentable.fromEnum(NameType::values);

    private final ResourceKey<CustomNameType> key;
    private final @Nullable String plural;
    private final @Nullable CommonPermissionNode permission;
    private final boolean showInCommands;
    private final @Nullable String displayName;

    NameType(ResourceKey<CustomNameType> key, String plural, CommonPermissionNode permission, String displayName) {
        this.key = key;
        this.plural = plural;
        this.permission = permission;
        this.showInCommands = true;
        this.displayName = displayName;
    }

    NameType(ResourceKey<CustomNameType> key) {
        this.key = key;
        this.showInCommands = false;
        this.plural = null;
        this.permission = null;
        this.displayName = null;
    }

    @Override
    public String getSerializedName() {
        return key.identifier().getPath();
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

    public static NameType fromKey(ResourceKey<CustomNameType> key) {
        for (NameType type : values()) {
            if (type.key == key) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported key: " + key);
    }
}
