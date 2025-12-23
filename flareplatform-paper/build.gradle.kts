import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml.PluginLoadOrder

plugins {
    alias(libs.plugins.runPaper)
    alias(libs.plugins.resourceFactoryPaper)
    alias(libs.plugins.blossom)
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
    main = "co.technove.flareplatform.paper.FlarePlatformPaper"
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
            "paperweight-mappings-namespace" to "mojang",
        )
    }
}

tasks.runServer {
    minecraftVersion("1.21.11")
}

sourceSets.main {
    blossom.javaSources {
        property("flare", libs.versions.flare.get())
        property("oshi", libs.versions.oshi.get())
    }
}
