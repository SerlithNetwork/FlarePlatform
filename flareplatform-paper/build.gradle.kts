import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml.PluginLoadOrder

plugins {
    id("xyz.jpenilla.run-paper") version libs.versions.run.task
    id("xyz.jpenilla.resource-factory-paper-convention") version libs.versions.resource.factory
}

dependencies {
    implementation(projects.flareplatformCommon)
    compileOnly(libs.folia.api)
    compileOnly(libs.jspecify)
    compileOnly(libs.flare)
    compileOnly(libs.oshi.core)
}

runPaper.folia.registerTask() // run folia

paperPluginYaml {
    apiVersion = "1.21"
    name = "FlarePlatform"
    main = "co.technove.flareplatform.paper.FlarePlatform"
    loader = "co.technove.flareplatform.paper.FlarePlatformPaperLoader"
    description = "Profile your server with Flare!"
    load = PluginLoadOrder.POSTWORLD
    authors.add("PaulBGD, SerlithNetwork")
    foliaSupported = true
    website = "https://serlith.net"
}

tasks.withType<ShadowJar>().configureEach {
    manifest {
        attributes(
            "flare-version" to libs.versions.flare.get(),
            "oshi-version" to libs.versions.oshi.get(),
            "paperweight-mappings-namespace" to "mojang",
        )
    }
}

tasks.runServer {
    minecraftVersion("1.21.8")
}