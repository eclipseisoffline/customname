plugins {
    alias(libs.plugins.multimod)
}

multimod.common()

dependencies {
    add("compileOnly", libs.luckperms)
}
