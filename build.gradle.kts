plugins {
    java
    alias(libs.plugins.shadow)
}

group = "co.technove"
version = "2.0.0"

dependencies {
    implementation(projects.paper)
    implementation(projects.velocity)
}

allprojects {
    apply(plugin = "java")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
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