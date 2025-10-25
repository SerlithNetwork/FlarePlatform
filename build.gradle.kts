plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

group = "co.technove"
version = "2.0.0"

val flareVersion = providers.gradleProperty("flareVersion").get()
val oshiVersion = providers.gradleProperty("oshiVersion").get()

dependencies {
    implementation(project(":paper"))
    implementation(project(":velocity"))
}

allprojects {
    apply(plugin = "java")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        compileOnly("org.jspecify:jspecify:1.0.0")
        compileOnly("net.serlith:Flare:$flareVersion")
        compileOnly("com.github.oshi:oshi-core:$oshiVersion")
        compileOnly("com.google.guava:guava:25.1-jre")
    }

    tasks {
        compileJava {
            options.encoding = Charsets.UTF_8.name()
            options.release.set(21)
        }
    }
}
tasks {
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")
        dependsOn(":velocity:shadowJar")
        dependsOn(":paper:shadowJar")
    }
}