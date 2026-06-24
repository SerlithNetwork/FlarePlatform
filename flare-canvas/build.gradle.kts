repositories {
    maven("https://maven.canvasmc.io/releases")
}

dependencies {
    compileOnly(projects.flareCommon)
    // compileOnly(libs.canvasApi)
    compileOnly(files("libs/canvas-api-26.2.local-SNAPSHOT.jar"))
}
