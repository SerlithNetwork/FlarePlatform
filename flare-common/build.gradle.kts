plugins {
    id("co.technove.flareplatform.gradle.build-conventions")
}

dependencies {
    api(libs.config.api.core)
    api(libs.config.api.adventure)
    api(libs.flare)

    compileOnly(libs.jspecify)
    compileOnly(libs.oshi.core)
    compileOnly(libs.guava)
}
