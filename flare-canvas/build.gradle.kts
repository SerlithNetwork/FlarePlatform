repositories {
    maven("https://maven.canvasmc.io/snapshots") {
        name = "canvas-snapshots"
    }
}

dependencies {
    compileOnly(projects.flareCommon)
    compileOnly(files("libs/canvas-api-26.2.local-SNAPSHOT.jar"))
}
