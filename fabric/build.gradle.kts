plugins {
    alias(libs.plugins.multimod)
}

multimod.modPublishing {
    modrinth {
        // Fabric API
        requires {
            slug = "P7dR8mSH"
        }
    }
}

multimod.fabric(project(":common"))

dependencies {
    // add("implementation", libs.fabric.permissions.api)
    // add("include", libs.fabric.permissions.api) FIXME in FMJ too
}
