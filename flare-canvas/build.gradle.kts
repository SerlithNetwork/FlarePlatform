repositories {
    maven("https://maven.canvasmc.io/releases")
}

dependencies {
    compileOnly(projects.flareCommon)
    compileOnly(libs.canvas.api)
}

java.disableAutoTargetJvm()
