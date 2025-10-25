val flareVersion = providers.gradleProperty("flareVersion").get()
val oshiVersion = providers.gradleProperty("oshiVersion").get()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.processResources {
    val flareVersion = flareVersion
    val oshiVersion = oshiVersion
    filteringCharset = Charsets.UTF_8.name()
    filesMatching("**/libraries.properties") {
        expand(
            "flareVersion" to flareVersion,
            "oshiVersion" to oshiVersion,
        )
    }
}