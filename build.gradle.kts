import me.modmuss50.mpp.ReleaseType

plugins {
    alias(libs.plugins.multimod)
}

group = providers.gradleProperty("maven_group").get()
version = providers.gradleProperty("version").get()
multimod {
    id = providers.gradleProperty("mod_id")
    name = providers.gradleProperty("mod_name")
    description = providers.gradleProperty("mod_description")

    archivesBaseName = providers.gradleProperty("archives_base_name")
    settings {
        repositories {
            maven {
                name = "eclipseisoffline"
                url = uri("https://maven.eclipseisoffline.xyz/releases")
            }
            maven {
                name = "Nucleoid"
                url = uri("https://maven.nucleoid.xyz")
            }
        }
    }

    minecraft {
        minecraft = libs.minecraft
    }
    resourceConfiguration.defaults()

    fabricApi = libs.fabric.api
    neoForgeVersion = libs.versions.neoforge

    sharedDependencies {
        multiModInclude(multiModImplementation(libs.commonpermissionsapi))
        multiModInclude(multiModImplementation(libs.filefixutils))
    }

    modPublishing {
        base {
            changelog = file("CHANGELOG.md").readText()
            type = providers.gradleProperty("release_type").map { ReleaseType.of(it) }
        }

        modrinth {
            accessToken = providers.gradleProperty("MODRINTH_API_TOKEN")
            projectId = providers.gradleProperty("modrinth_project_id")
            minecraftVersions.addAll(libs.versions.minecraft.release.get().split(","))
        }

        github {
            accessToken = providers.gradleProperty("GITHUB_API_PUBLISH_TOKEN")
            repository = providers.gradleProperty("github_repository")
            commitish = providers.gradleProperty("git_branch")
        }
    }

    publishing {
        maven {
            name = "eclipseisoffline"
            url = uri("https://maven.eclipseisoffline.xyz/releases")
            credentials(PasswordCredentials::class)
        }
    }
}
