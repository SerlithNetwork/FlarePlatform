import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.resourcefactory.velocity.VelocityPluginJson

plugins {
    id("xyz.jpenilla.run-velocity") version "3.0.0"
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.resource-factory-velocity-convention") version "1.3.0"
}

val flareVersion = providers.gradleProperty("flareVersion").get()
val oshiVersion = providers.gradleProperty("oshiVersion").get()
val velocityApiVersion = providers.gradleProperty("velocityApiVersion").get()

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

repositories {
    maven(paperMavenPublicUrl)
}

dependencies {
    implementation(project(":common"))
    implementation("net.serlith:Flare:$flareVersion")
    implementation("com.github.oshi:oshi-core:$oshiVersion")
    compileOnly("com.velocitypowered:velocity-api:$velocityApiVersion")
}

tasks.runVelocity {
    velocityVersion("3.4.0-SNAPSHOT")
}

velocityPluginJson {
    id = "flareplatformvelocity"
    name = "FlarePlatformVelocity"
    description = "Profile your proxy with Flare"
    main = "co.technove.flareplatform.velocity.FlarePlatformVelocity"
    authors.add("PaulBGD, SerlithNetwork")
}

val shadowJar by tasks.existing(ShadowJar::class) {
    minimize()
    archiveClassifier.set(null as String?)
    val prefix = "co.technove.flareplatform.lib"
    listOf(
        //"oshi",
        "co.technove.flare.",
        "one", // included in the flare dep
    ).forEach { pack ->
        relocate(pack, "$prefix.$pack")
    }
}