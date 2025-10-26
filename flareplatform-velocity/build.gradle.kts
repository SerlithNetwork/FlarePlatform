import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.regex.Pattern

plugins {
    id("xyz.jpenilla.run-velocity") version libs.versions.run.task
    id("xyz.jpenilla.resource-factory-velocity-convention") version libs.versions.resource.factory
}

dependencies {
    implementation(projects.flareplatformCommon)
    implementation(libs.flare)
    implementation(libs.oshi.core)
    compileOnly(libs.jspecify)
    compileOnly(libs.velocity.api)
}

tasks.runVelocity {
    velocityVersion(libs.versions.velocity.api.get())
}

velocityPluginJson {
    id = "flareplatform"
    name = "FlarePlatform"
    description = "Profile your proxy with Flare!"
    url = "https://serlith.net"
    authors.add("PaulBGD, SerlithNetwork")
    main = "co.technove.flareplatform.velocity.FlarePlatform"
}

tasks.withType<ShadowJar>().configureEach {
    configureRelocation()
}

fun ShadowJar.configureRelocation() {
    val prefix = "co.technove.flareplatform.lib"
    listOf(
        "oshi",
        //"co.technove.flare.", we cant relocate flare nor async profiler
        //"one", // included in the flare dep
    ).forEach { pack ->
        relocate(pack, "$prefix.$pack")
    }
    // we have to rename them to match the new package for some reason (rename, not relocate)
    rename(Pattern.compile("^oshi.*"), "co.technove.flareplatform.lib.\$0")
}