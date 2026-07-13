import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml.PluginLoadOrder
import xyz.jpenilla.runpaper.task.RunServer
import com.google.gson.Gson
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import xyz.jpenilla.runtask.task.AbstractRun
import java.net.URI
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.system.measureNanoTime

plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.resource.factory.paper)
    alias(libs.plugins.blossom)
    alias(libs.plugins.lombok)
}

dependencies {
    implementation(projects.flareCommon)
    implementation(projects.flareCanvas)
    implementation(projects.flareFish)
    implementation(libs.faststats.bukkit)
    implementation(libs.bstats.bukkit)
    compileOnly(libs.folia.api)
    compileOnly(libs.jspecify)
    compileOnly(libs.oshi.core)
}

runPaper.folia.registerTask() // run folia

paperPluginYaml {
    apiVersion = "1.21.8"
    name = "Flare"
    main = "co.technove.flareplatform.paper.FlarePlatformPaper"
    loader = "co.technove.flareplatform.paper.FlarePlatformPaperLoader"
    description = "Profile your server with Flare!"
    load = PluginLoadOrder.STARTUP
    authors.add("PaulBGD, SerlithNetwork")
    foliaSupported = true
    website = "https://serlith.net"
}

tasks {
    runServer {
        minecraftVersion("1.21.11")
    }
    jar {
        archiveClassifier.set("dev")
    }
    shadowJar {
        archiveClassifier.set("")
        manifest {
            attributes(
                "paperweight-mappings-namespace" to "mojang",
            )
        }
        configureRelocation()
    }
    withType(AbstractRun::class).configureEach {
        jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+DebugNonSafepoints")
    }
}

val queryCanvasApi = tasks.register<QueryCanvasApi>("queryCanvasApi") {
    minecraftVersion.set("26.2")
    responseFile.set(layout.buildDirectory.file("downloaded/response.json"))
}

val downloadCanvasJar = tasks.register<DownloadCanvasJar>("downloadCanvasJar") {
    responseFile.set(queryCanvasApi.flatMap { it.responseFile })
    outputJar.set(layout.buildDirectory.file("downloaded/canvas.jar"))
}

tasks.register<RunServer>("runCanvas") {
    version.set(queryCanvasApi.flatMap { it.minecraftVersion })
    pluginJars.from(tasks.shadowJar.flatMap { it.archiveFile })
    displayName.set("Canvas")
    serverJar(downloadCanvasJar.flatMap { it.outputJar })
}

sourceSets.main {
    blossom.javaSources {
        property("oshi", libs.versions.oshi.get())
    }
}

fun ShadowJar.configureRelocation() {
    val prefix = "co.technove.flareplatform.libs"
    mapOf(
        "co.technove.flare." to "flare",
        "dev.faststats" to "faststats",
        "org.bstats" to "bstats",
    ).forEach { pack ->
        relocate(pack.key, "$prefix.${pack.value}")
    }
}

data class LatestBuildResponse(
    val downloadUrl: String,
    val buildNumber: String,
    val channelVersion: String,
    val channelName: String,
    val hash: String,
)

@DisableCachingByDefault(because = "Queries from a remote API")
abstract class QueryCanvasApi : DefaultTask() {

    @get:Input
    abstract val minecraftVersion: Property<String>

    @get:OutputFile
    abstract val responseFile: RegularFileProperty

    @TaskAction
    fun fetch() {
        val output = responseFile.get().asFile
        output.parentFile.mkdirs()
        output.delete()

        logger.lifecycle("Fetching latest Canvas build for version ${minecraftVersion.get()}...")
        output.writeText(
            URI.create("https://canvasmc.io/api/v2/builds/latest?project=canvas&experimental=true&channel=${minecraftVersion.get()}")
                .toURL()
                .readText()
        )
    }
}

@CacheableTask
abstract class DownloadCanvasJar : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val responseFile: RegularFileProperty

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @TaskAction
    fun download() {
        val output = outputJar.get().asFile
        output.parentFile.mkdirs()
        output.delete()

        val latest = Gson().fromJson(
            responseFile.get().asFile.readText(),
            LatestBuildResponse::class.java
        )

        val channel = latest.channelName
        if (channel == "stable") {
            logger.lifecycle("Latest build for ${latest.channelVersion} is ${latest.buildNumber}.")
        } else {
            logger.lifecycle("Latest build for ${latest.channelVersion} is ${latest.buildNumber} (${channel.uppercaseFirstChar()}).")
        }
        logger.lifecycle("Downloading Canvas ${latest.channelVersion} build ${latest.buildNumber}...")

        val digest = MessageDigest.getInstance("SHA-256")

        val downloadedIn = measureNanoTime {
            DigestInputStream(
                URI.create(latest.downloadUrl).toURL().openStream(),
                digest
            ).use { input ->
                output.outputStream().buffered().use { out ->
                    input.copyTo(out)
                }
            }
        }

        logger.lifecycle("Done downloading Canvas, took ${formatS(downloadedIn)}.")

        /* we dont have hashes yet
        val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
        if (actualHash.equals(latest.hash, ignoreCase = true)) {
            logger.lifecycle("Verified SHA-256 hash of downloaded jar.")
        } else {
            output.delete()
            logger.lifecycle("Invalid SHA256 hash for downloaded file: '{}', deleting.", output.name)
            error("Failed to verify SHA256 hash of downloaded file.")
        }
         */
    }
    private fun formatS(ns: Long): String {
        return "%.2fs".format(ns / 1_000_000_000.0)
    }
}
