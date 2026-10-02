package xyz.eclipseisoffline.eclipsescustomname.api;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import xyz.eclipseisoffline.eclipsescustomname.CustomName;

/// Holds {@link ResourceKey}s for each name type managed by Custom Names.
///
/// Creating custom name types is not yet possible and unsupported.
public interface CustomNameType {
    ResourceKey<? extends Registry<CustomNameType>> ROOT_ID = ResourceKey.createRegistryKey(CustomName.getModdedIdentifier("name_type"));

    ResourceKey<CustomNameType> PREFIX = create("prefix");
    ResourceKey<CustomNameType> SUFFIX = create("suffix");
    ResourceKey<CustomNameType> NICKNAME = create("nickname");
    ResourceKey<CustomNameType> LUCKPERMS_PREFIX = create("luckperms_prefix");
    ResourceKey<CustomNameType> LUCKPERMS_SUFFIX = create("luckperms_suffix");

    private static ResourceKey<CustomNameType> create(String name) {
        return ResourceKey.create(ROOT_ID, CustomName.getModdedIdentifier(name));
    }
}
