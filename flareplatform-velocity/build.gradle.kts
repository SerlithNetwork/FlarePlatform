import xyz.jpenilla.resourcefactory.velocity.VelocityPluginJson

plugins {
    id("xyz.jpenilla.run-velocity") version libs.versions.run.task
    id("xyz.jpenilla.resource-factory-velocity-convention") version libs.versions.resource.factory
}

dependencies {
    implementation(projects.common)
    implementation(libs.flare)
    implementation(libs.oshi.core)
    compileOnly(libs.jspecify)
    compileOnly(libs.velocity.api)
}

tasks.runVelocity {
    velocityVersion(libs.versions.velocity.api.get())
}

velocityPluginJson {
    id = "flareplatform"
    name = "FlarePlatform"
    description = "Profile your proxy with Flare!"
    main = "co.technove.flareplatform.velocity.FlarePlatform"
    authors.add("PaulBGD, SerlithNetwork")
}