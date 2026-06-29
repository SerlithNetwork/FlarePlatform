dependencies {
    api(libs.config.api.core)
    api(libs.config.api.adventure)
    api(libs.flare)

    compileOnly(libs.jspecify)
    compileOnly(libs.oshi.core)
    compileOnly(libs.guava)
}

tasks {
    jar {
        archiveClassifier.set("dev")
    }
    shadowJar {
        archiveClassifier.set("")
    }
}
