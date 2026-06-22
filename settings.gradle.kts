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
