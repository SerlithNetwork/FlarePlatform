repositories {
    maven("https://maven.canvasmc.io/releases")
}

dependencies {
    compileOnly(projects.flareCommon)
    compileOnly(files("libs/canvas-api-26.2.local-SNAPSHOT.jar"))
    compileOnly("net.kyori:adventure-api:5.2.0")
    compileOnly("org.jetbrains:annotations:26.1.0")
}

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// dirty hack
configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
}
