import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
    java
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0"
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.run-paper") version "3.0.0"
}

group = "co.technove"
version = "2.0.0"

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven(paperMavenPublicUrl)
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jspecify:jspecify:1.0.0")
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")

    implementation("net.serlith:Flare:4.0.4")
    implementation("com.github.oshi:oshi-core:6.6.5")
}

bukkitPluginYaml {
    main = "co.technove.flareplugin.FlarePlugin"
    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
    apiVersion = "1.21"
    authors.add("PaulBGD, SerlithNetwork")
/*
    commands {
        register("flare") {
            description = "Flare profiling command"
            aliases = listOf("profiler", "sampler")
            permission = "flareplugin.command"
            usage = "/flare"
        }
    }
*/
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    runServer {
        minecraftVersion("1.21.10")
    }

    jar {
        manifest {
            attributes(
                "paperweight-mappings-namespace" to "mojang",
            )
        }
    }

}

val shadowJar by tasks.existing(ShadowJar::class) {
    archiveClassifier.set(null as String?)
    val prefix = "co.technove.flareplugin.lib"
    listOf(
        "com.github.oshi",
        "co.technove.flare",
        "org.jspecify",
    ).forEach { pack ->
        relocate(pack, "$prefix.$pack")
    }
}