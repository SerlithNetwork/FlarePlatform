import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml
plugins {
    id("xyz.jpenilla.run-paper") version libs.versions.run.task
    id("xyz.jpenilla.resource-factory-paper-convention") version libs.versions.resource.factory
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
    name = "FlarePlatform"
    main = "co.technove.flareplatform.paper.FlarePlatform"
    loader = "co.technove.flareplatform.paper.FlarePlatformPaperLoader"
    description = "Profile your server with Flare!"
    apiVersion = "1.21"
    authors.add("PaulBGD, SerlithNetwork")
    foliaSupported = true
}

tasks.runServer {
    minecraftVersion("1.21.8")
}