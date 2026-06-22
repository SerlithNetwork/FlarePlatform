import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.regex.Pattern

plugins {
    alias(libs.plugins.run.velocity)
    alias(libs.plugins.resource.factory.velocity)
    alias(libs.plugins.lombok)
}

dependencies {
    implementation(projects.flareCommon)
    implementation(libs.oshi.core)
    compileOnly(libs.jspecify)
    compileOnly(libs.velocity.api)
}

velocityPluginJson {
    id = "flare"
    name = "Flare"
    description = "Profile your proxy with Flare!"
    url = "https://serlith.net"
    authors.add("PaulBGD, SerlithNetwork")
    main = "co.technove.flareplatform.velocity.FlarePlatformVelocity"
}

tasks {
    runVelocity {
        velocityVersion(libs.versions.velocity.api.get())
    }
    shadowJar {
        configureRelocation()
    }
    withType(xyz.jpenilla.runtask.task.AbstractRun::class).configureEach {
        jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints")
    }
}

fun ShadowJar.configureRelocation() {
    val prefix = "co.technove.flareplatform.libs"
    mapOf(
        "oshi" to "oshi",
        "co.technove.flare" to "flare",
    ).forEach { pack ->
        relocate(pack.key, "$prefix.${pack.value}")
    }
    // we have to rename them to match the new package for some reason (rename, not relocate)
    rename(Pattern.compile("^oshi.*"), $$"$$prefix.$0")
}
