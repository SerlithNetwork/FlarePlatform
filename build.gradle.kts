import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml

plugins {
    java
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.3.0"
    id("xyz.jpenilla.run-paper") version "3.0.0"
}

group = "co.technove"
version = "2.0.0"

val foliaApiVersion = providers.gradleProperty("foliaApiVersion").get()
val flareVersion = providers.gradleProperty("flareVersion").get()
val oshiVersion = providers.gradleProperty("oshiVersion").get()

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
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("dev.folia:folia-api:$foliaApiVersion")
    compileOnly("net.serlith:Flare:$flareVersion")
    compileOnly("com.github.oshi:oshi-core:$oshiVersion")
}

paperPluginYaml {
    main = "co.technove.flareplugin.FlarePlugin"
    bootstrapper = "co.technove.flareplugin.FlarePluginBootstrap"
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

runPaper.folia.registerTask() // run folia

tasks {
    runServer {
        minecraftVersion("1.21.8")
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    jar {
        manifest {
            attributes(
                "paperweight-mappings-namespace" to "mojang",
            )
        }
    }

    processResources {
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
}