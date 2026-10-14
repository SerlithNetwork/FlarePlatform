import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.regex.Pattern

plugins {
    id("co.technove.flareplatform.gradle.build-conventions")
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
