import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml
plugins {
    id("xyz.jpenilla.run-paper") version "3.0.0"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.3.0"
    alias(libs.plugins.shadow)
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

repositories {
    maven(paperMavenPublicUrl)
}

dependencies {
    compileOnly(libs.folia.api)
    compileOnly(libs.jspecify)
    compileOnly(libs.flare)
    compileOnly(libs.oshi.core)
    implementation(projects.common)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

runPaper.folia.registerTask() // run folia

paperPluginYaml {
    name = "FlarePlatformPaper"
    main = "co.technove.flareplatform.paper.FlarePlatformPaper"
    loader = "co.technove.flareplatform.paper.FlarePlatformPaperLoader"
    description = "Profile your server with Flare"
    apiVersion = "1.21"
    authors.add("PaulBGD, SerlithNetwork")
    foliaSupported = true
}

tasks.runServer {
    minecraftVersion("1.21.8")
}

tasks.jar {
    manifest {
        attributes(
            "paperweight-mappings-namespace" to "mojang",
        )
    }
}