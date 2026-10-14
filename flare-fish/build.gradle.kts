plugins {
    id("co.technove.flareplatform.gradle.build-conventions")
}

repositories {
    // for fish-api
    exclusiveContent {
        forRepository {
            ivy("https://github.com/") {
                patternLayout {
                    artifact("[organisation]/[module]/releases/download/[revision]/[artifact].[ext]")
                }
                metadataSources {
                    artifact()
                }
            }
        }
        filter {
            includeModule("Biquaternions", "Fish")
        }
    }
}

dependencies {
    compileOnly(projects.flareCommon)
    compileOnly("Biquaternions:Fish:ver-26.3") {
        artifact {
            name = "fish-api-26.3.build.3-alpha"
            extension = "jar"
        }
        isChanging = true
    }
    compileOnly("net.kyori:adventure-api:4.26.1")
    compileOnly("org.jetbrains:annotations:26.1.0")
}
