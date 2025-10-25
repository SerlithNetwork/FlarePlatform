plugins {
    java
}
group = "co.technove"
version = "2.0.0"

val flareVersion = providers.gradleProperty("flareVersion").get()
val oshiVersion = providers.gradleProperty("oshiVersion").get()

subprojects {
    apply(plugin = "java")
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