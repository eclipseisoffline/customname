plugins {
    alias(libs.plugins.multimod)
}

dependencies {
    compileOnlyApi(libs.luckperms)
}

multimod.common()
