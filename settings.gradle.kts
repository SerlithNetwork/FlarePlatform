pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

rootProject.name = "FlarePlatform"
// modules
include("common")
include("paper")
include("velocity")