plugins {
    alias(libs.plugins.multimod)
}

multimod.common()

dependencies {
    add("compileOnlyApi", libs.luckperms)
}
