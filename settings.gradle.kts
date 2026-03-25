pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()

        maven {
            name = "eclipseisoffline"
            url = uri("https://maven.eclipseisoffline.xyz/releases")
        }

        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }

        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
        }
    }
}

rootProject.name = "CustomName"

include("common", "fabric", "neoforge")
