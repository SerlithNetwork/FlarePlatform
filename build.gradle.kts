import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.file.DuplicatesStrategy

plugins {
    java
    alias(libs.plugins.shadow)
}

subprojects {
    apply {
        plugin("java")
        plugin("java-library")
        plugin("com.gradleup.shadow")
    }

    val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

    repositories {
        mavenCentral()
        maven(paperMavenPublicUrl)
        maven("https://jitpack.io")
        maven(("https://repo.faststats.dev/releases"))
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
    tasks.withType<ShadowJar>().configureEach {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.FAIL
        filesMatching("META-INF/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        mapOf(
            "dev.dejvokep.boostedyaml." to "boostedyaml",
            "net.j4c0b3y.api.config." to "config",
        ).forEach {
            relocate(it.key, "co.technove.flareplatform.libs.${it.value}")
        }

    }
}
