import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy

plugins {
    java
    `java-library`
    id("com.gradleup.shadow")
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

repositories {
    mavenCentral()
    maven(paperMavenPublicUrl)
    maven("https://jitpack.io")
    maven("https://repo.faststats.dev/releases")
}

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = Charsets.UTF_8.name()
    options.release = 21
    options.isFork = true
}

tasks.withType<ProcessResources>().configureEach {
    filteringCharset = Charsets.UTF_8.name()
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.withType<ShadowJar>().configureEach {
    archiveClassifier.set("")
    mergeServiceFiles()
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    configureRelocation()
}

fun ShadowJar.configureRelocation() {
    mapOf(
        "dev.dejvokep.boostedyaml." to "boostedyaml",
        "net.j4c0b3y.api.config." to "config",
    ).forEach {
        relocate(it.key, "co.technove.flareplatform.libs.${it.value}")
    }
}
