repositories {
    maven("https://maven.canvasmc.io/releases")
}

dependencies {
    compileOnly(projects.flareCommon)
    compileOnly(libs.canvas.api)
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
