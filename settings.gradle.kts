plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "FlarePlatform"

// modules
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(
    "flare-common",
    "flare-paper",
    "flare-velocity"
)

include("flare-fish")
include("flare-canvas")

gradle.lifecycle.beforeProject {
    val majorVersion = providers.gradleProperty("major_version").get().trim()
    val build = providers.environmentVariable("BUILD_NUMBER").orNull?.trim()?.toInt()
    val versionString = if (build == null) {
        "$majorVersion.local"
    } else {
        "$majorVersion.$build"
    }
    version = versionString
}
