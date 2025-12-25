dependencies {
    api(libs.config.api.core)
    api(libs.config.api.adventure)
    api(libs.flare)

    compileOnly(libs.jspecify)
    compileOnly(libs.oshi.core)
    compileOnly(libs.guava)
    compileOnly(libs.configurate4.yaml)
}
