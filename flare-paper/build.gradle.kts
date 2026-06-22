import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml.PluginLoadOrder
import java.util.regex.Pattern

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
    apiVersion = "1.19"
    name = "Flare"
    main = "co.technove.flareplatform.paper.FlarePlatformPaper"
    loader = "co.technove.flareplatform.paper.FlarePlatformPaperLoader"
    description = "Profile your server with Flare!"
    load = PluginLoadOrder.STARTUP
    authors.add("PaulBGD, SerlithNetwork")
    foliaSupported = true
    website = "https://serlith.net"
}

tasks {
    runServer {
        minecraftVersion("1.21.11")
    }
    jar {
        archiveClassifier.set("dev")
    }
    shadowJar {
        archiveClassifier.set("")
        manifest {
            attributes(
                "paperweight-mappings-namespace" to "mojang",
            )
        }
        configureRelocation()
    }
    withType(xyz.jpenilla.runtask.task.AbstractRun::class).configureEach {
        jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints")
    }
}

sourceSets.main {
    blossom.javaSources {
        property("flare", libs.versions.flare.get())
        property("oshi", libs.versions.oshi.get())
    }
}

fun ShadowJar.configureRelocation() {
    val prefix = "co.technove.flareplatform.libs"
    mapOf(
        "co.technove.flare" to "flare",
    ).forEach { pack ->
        relocate(pack.key, "$prefix.${pack.value}")
    }
}
