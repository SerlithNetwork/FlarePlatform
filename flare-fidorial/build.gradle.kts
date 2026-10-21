import co.technove.flareplatform.gradle.github.githubReleases
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.runpaper.task.RunServer
import xyz.jpenilla.runtask.task.AbstractRun
import java.util.regex.Pattern

plugins {
    id("co.technove.flareplatform.gradle.build-conventions")
    id("xyz.jpenilla.run-paper")
    alias(libs.plugins.lombok)
    alias(libs.plugins.blossom)
}

repositories {
    maven("https://repo.euphyllia.moe/repository/maven-public/")
    maven("https://repo.opencollab.dev/maven-snapshots") // geyser's configurate fork
}

dependencies {
    implementation(projects.flareCommon)
    implementation(libs.oshi.core)
    compileOnly(libs.fidorial.api)
    compileOnly(libs.jspecify)
}

tasks.withType<JavaCompile>().configureEach {
    // explicitly opt-in to 25 as fidorial didn't exist for Java <25
    options.release = 25
}

tasks.withType<ShadowJar>().configureEach {
    configureRelocation()
}

sourceSets.main {
    blossom.resources {
        property("version", project.version.toString())
    }
}

fun ShadowJar.configureRelocation() {
    val prefix = "co.technove.flareplatform.libs"
    mapOf(
        "oshi" to "oshi",
        "co.technove.flare." to "flare",
    ).forEach { pack ->
        relocate(pack.key, "$prefix.${pack.value}")
    }
    rename(Pattern.compile("^oshi.*"), $$"$$prefix.$0")
}

val downloadsService = githubReleases(
    repository = "Fidorial/Fidorial",
    assetPattern = "Fidorial-*.jar",
    tagFormat = "dev-*",
    displayName = "Fidorial"
)

tasks.register<RunServer>("runFidorial") {
    downloadsApiService.set(downloadsService)
    version.set("0.1.0")
    pluginJars.from(tasks.shadowJar.flatMap { it.archiveFile })
    displayName.set("Fidorial")
}

tasks.withType<AbstractRun>().configureEach {
    jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints")
}

tasks.runServer {
    enabled = false
}
