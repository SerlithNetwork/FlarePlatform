import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml
plugins {
    id("xyz.jpenilla.run-paper") version "3.0.0"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.3.0"
    id("com.gradleup.shadow") version "9.2.2"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"
val foliaApiVersion = providers.gradleProperty("foliaApiVersion").get()

repositories {
    maven(paperMavenPublicUrl)
}

dependencies {
    compileOnly("dev.folia:folia-api:$foliaApiVersion")
    implementation(project(":common"))
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