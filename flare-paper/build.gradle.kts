import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml.PluginLoadOrder

plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.resource.factory.paper)
    alias(libs.plugins.blossom)
    alias(libs.plugins.lombok)
}

dependencies {
    implementation(projects.flareCommon)
    compileOnly(libs.folia.api)
    compileOnly(libs.jspecify)
    compileOnly(libs.oshi.core)
}

runPaper.folia.registerTask() // run folia

paperPluginYaml {
    apiVersion = "1.21"
    name = "Flare"
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
