import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy
import java.util.regex.Pattern

plugins {
    java
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(projects.common)
    implementation(projects.flareplatformPaper)
    implementation(projects.flareplatformVelocity)
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")

    val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
        maven("https://jitpack.io")
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
        options.isFork = true
    }
    tasks.withType<ProcessResources>().configureEach {
        filteringCharset = Charsets.UTF_8.name()
    }
    tasks.withType<ShadowJar>().configureEach {
        minimize()
        mergeServiceFiles()
        configureStandard()
        duplicatesStrategy = DuplicatesStrategy.FAIL
        filesMatching("META-INF/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveClassifier.set("")
}

fun ShadowJar.configureStandard() {
    val prefix = "co.technove.flareplatform.lib"
    listOf(
        "oshi",
        "co.technove.flare.",
        "one", // included in the flare dep
    ).forEach { pack ->
        relocate(pack, "$prefix.$pack")
    }
    // we have to rename them to match the new package for some reason (rename, not relocate)
    rename(Pattern.compile("^oshi.*"), "co.technove.flareplatform.lib.\$0")
    manifest {
        attributes(
            "paperweight-mappings-namespace" to "mojang",
        )
    }
}