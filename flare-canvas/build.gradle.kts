repositories {
    maven("https://maven.canvasmc.io/releases")
}

dependencies {
    compileOnly(projects.flareCommon)
    compileOnly(libs.canvas.api)
}

java.disableAutoTargetJvm()

extensions.configure<JavaPluginExtension> {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
