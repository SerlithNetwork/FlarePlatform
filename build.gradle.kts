import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy

plugins {
    java
    alias(libs.plugins.shadow)
}

subprojects {
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
        duplicatesStrategy = DuplicatesStrategy.FAIL
        filesMatching("META-INF/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}