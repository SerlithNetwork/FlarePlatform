plugins {
    java
    alias(libs.plugins.shadow)
}

group = "co.technove"
version = providers.gradleProperty("version").get()

dependencies {
    implementation(projects.paper)
    implementation(projects.velocity)
}

val paperMavenPublicUrl = "https://repo.papermc.io/repository/maven-public/"

allprojects {
    apply(plugin = "java")

    group = rootProject.group
    version = rootProject.version

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
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")
        dependsOn(
            project(":velocity").tasks.shadowJar,
            project(":paper").tasks.shadowJar
        )
    }
}