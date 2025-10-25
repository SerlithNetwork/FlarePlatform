import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("xyz.jpenilla.run-velocity") version "3.0.0"
    id("com.gradleup.shadow") version "9.2.2"
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
    annotationProcessor("com.velocitypowered:velocity-api:$velocityApiVersion")
}

tasks.runVelocity {
    velocityVersion("3.4.0-SNAPSHOT")
}

val shadowJar by tasks.existing(ShadowJar::class) {
    archiveClassifier.set(null as String?)
    val prefix = "co.technove.flareplatform.lib"
    listOf(
        "oshi",
        "co.technove.flare.",
        "one", // included in the flare dep
    ).forEach { pack ->
        relocate(pack, "$prefix.$pack")
    }
}