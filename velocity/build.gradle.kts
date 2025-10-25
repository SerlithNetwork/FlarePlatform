import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.resourcefactory.velocity.VelocityPluginJson

plugins {
    id("xyz.jpenilla.run-velocity") version "3.0.0"
    alias(libs.plugins.shadow)
    id("xyz.jpenilla.resource-factory-velocity-convention") version "1.3.0"
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

repositories {
    maven(paperMavenPublicUrl)
}

dependencies {
    implementation(projects.common)
    implementation(libs.flare)
    implementation(libs.oshi.core)
    compileOnly(libs.jspecify)
    compileOnly(libs.velocity.api)
}

tasks.runVelocity {
    velocityVersion(libs.versions.velocity.api.get())
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