dependencies {
    compileOnly(libs.jspecify)
    compileOnly(libs.flare)
    compileOnly(libs.oshi.core)
    compileOnly(libs.guava)
    compileOnly(libs.configurate4.yaml)
}

tasks.processResources {
    val flareVersion = libs.versions.flare
    val oshiVersion = libs.versions.oshi
    filteringCharset = Charsets.UTF_8.name()
    filesMatching("**/libraries.properties") {
        expand(
            "flareVersion" to flareVersion,
            "oshiVersion" to oshiVersion,
        )
    }
}