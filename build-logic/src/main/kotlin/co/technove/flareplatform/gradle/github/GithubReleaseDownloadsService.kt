/*
 * This file is based off of DownloadsAPIServiceImpl,
 * a part of Run Task Gradle Plugins, adapted for GitHub releases
 *
 * Run Task Gradle Plugins
 * Copyright (c) 2024 Jason Penilla
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package co.technove.flareplatform.gradle.github

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.internal.logging.progress.ProgressLoggerFactory
import org.gradle.kotlin.dsl.registerIfAbsent
import xyz.jpenilla.runtask.service.DownloadsAPIService
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.DigestInputStream
import java.security.MessageDigest
import java.time.Duration
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.inputStream
import kotlin.io.path.isRegularFile
import kotlin.io.path.moveTo
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText

abstract class GithubReleaseDownloadsService : BuildService<GithubReleaseDownloadsService.Params>, DownloadsAPIService {
    interface Params : BuildServiceParameters {
        val repository: Property<String>

        val assetPattern: Property<String>

        val tagFormat: Property<String>

        val displayName: Property<String>
        val cacheDirectory: DirectoryProperty
        val refreshDependencies: Property<Boolean>
        val offlineMode: Property<Boolean>
        val updateCheckFrequency: Property<Duration>
    }

    private val client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val displayName: String
        get() = parameters.displayName.get()

    @Synchronized
    override fun resolveBuild(
        progressLoggerFactory: ProgressLoggerFactory,
        version: String,
        build: DownloadsAPIService.Build,
    ): Path {
        if (build !is DownloadsAPIService.Build.Latest) {
            throw InvalidUserDataException("GitHub releases have no build numbers; set the version to select the tag")
        }
        val tag = parameters.tagFormat.get().format(version)
        val directory = parameters.cacheDirectory.get().asFile.toPath().resolve(tag.replace(UNSAFE_PATH_CHARS, "_"))
        val jar = directory.resolve("server.jar")
        val recordFile = directory.resolve("asset.json")
        val local = readRecord(recordFile)?.takeIf { verifyLocal(jar, it, tag) }

        if (parameters.offlineMode.get()) {
            local ?: error("Offline mode is enabled and could not locate a locally cached build of $displayName $tag.")
            LOGGER.lifecycle("Located {} {} in local cache.", displayName, local.tag)
            return jar
        }

        val refresh = parameters.refreshDependencies.get()
        if (local != null && !refresh &&
            System.currentTimeMillis() - local.lastCheck <= parameters.updateCheckFrequency.get().toMillis()
        ) {
            LOGGER.lifecycle("Located {} {} in local cache.", displayName, local.tag)
            return jar
        }

        val asset = try {
            LOGGER.lifecycle("Fetching {} release {}...", displayName, tag)
            queryAsset(tag)
        } catch (ex: Exception) {
            local ?: throw IllegalStateException("Failed to fetch $displayName release $tag and no local build is cached: ${ex.message}", ex)
            LOGGER.lifecycle("Failed to check release {}, using the locally cached build.", tag, ex)
            return jar
        }

        if (local != null && !refresh && local.sameAsset(asset)) {
            writeRecord(recordFile, local.copy(lastCheck = System.currentTimeMillis()))
            LOGGER.lifecycle("Located {} {} in local cache.", displayName, local.tag)
            return jar
        }

        LOGGER.lifecycle("Downloading {} {} ({})...", displayName, asset.tag, asset.name)
        directory.createDirectories()
        val start = System.currentTimeMillis()
        val sha256 = download(progressLoggerFactory, asset, jar)
        LOGGER.lifecycle("Done downloading {}, took {}s.", displayName, (System.currentTimeMillis() - start) / 1000.0)

        writeRecord(recordFile, Record(asset.id, asset.tag, asset.updatedAt, sha256, System.currentTimeMillis()))
        return jar
    }

    private fun verifyLocal(jar: Path, record: Record, tag: String): Boolean {
        if (!jar.isRegularFile()) {
            return false
        }
        val actual = jar.inputStream().use { sha256(it) }
        if (actual.equals(record.sha256, ignoreCase = true)) {
            return true
        }
        LOGGER.lifecycle("Invalid SHA256 hash for locally cached {} {}, invalidating.", displayName, tag)
        logExpectedActual(record.sha256, actual)
        jar.deleteIfExists()
        return false
    }

    private fun queryAsset(tag: String): Asset {
        val repository = parameters.repository.get()
        val release = if ('*' in tag) latestRelease(repository, tag) else release(repository, tag)

        val glob = parameters.assetPattern.get()
        val pattern = globRegex(glob)
        val asset = release["assets"].asJsonArray
            .map { it.asJsonObject }
            .firstOrNull { pattern.matches(it["name"].asString) }
            ?: error("No asset matching '$glob' in $repository ${release["tag_name"].asString}")
        return Asset(
            asset["id"].asLong,
            release["tag_name"].asString,
            asset["name"].asString,
            asset["browser_download_url"].asString,
            asset["updated_at"].asString,
            asset["digest"]?.takeUnless { it.isJsonNull }?.asString?.removePrefix("sha256:"),
        )
    }

    private fun release(repository: String, tag: String): JsonObject =
        JsonParser.parseString(get("repos/$repository/releases/tags/$tag", "No release for tag '$tag' in $repository"))
            .asJsonObject

    private fun latestRelease(repository: String, glob: String): JsonObject {
        val pattern = globRegex(glob)
        val release = JsonParser.parseString(get("repos/$repository/releases?per_page=100", "No repository $repository"))
            .asJsonArray
            .map { it.asJsonObject }
            .filter { pattern.matches(it["tag_name"].asString) && !it["published_at"].isJsonNull }
            .maxByOrNull { it["published_at"].asString }
            ?: error("No release matching '$glob' in $repository")
        LOGGER.lifecycle("Latest release matching {} is {}.", glob, release["tag_name"].asString)
        return release
    }

    private fun get(path: String, notFound: String): String {
        val response = client.send(
            HttpRequest.newBuilder(URI.create("https://api.github.com/$path"))
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        return when (response.statusCode()) {
            200 -> response.body()
            404 -> error(notFound)
            403, 429 -> error("GitHub rate limit reached")
            else -> error("GitHub returned ${response.statusCode()}: ${response.body()}")
        }
    }

    private fun download(progressLoggerFactory: ProgressLoggerFactory, asset: Asset, target: Path): String {
        val response = client.send(
            HttpRequest.newBuilder(URI.create(asset.url)).GET().build(),
            HttpResponse.BodyHandlers.ofInputStream(),
        )
        if (response.statusCode() !in 200..299) {
            response.body().close()
            error("Failed to download $displayName: GitHub returned ${response.statusCode()}")
        }
        val expectedSize = response.headers().firstValueAsLong("Content-Length").orElse(-1L)

        val temp = target.resolveSibling("${target.fileName}.part")
        val digest = MessageDigest.getInstance("SHA-256")
        val progress = progressLoggerFactory.newOperation("github-release:${parameters.repository.get()}")
        progress.start("Downloading $displayName", "Downloading $displayName: ")
        try {
            DigestInputStream(response.body(), digest).use { input ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var lastUpdate = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) {
                            break
                        }
                        output.write(buffer, 0, read)
                        downloaded += read
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= PROGRESS_INTERVAL_MS) {
                            lastUpdate = now
                            progress.progress("Downloading $displayName: ${formatProgress(downloaded, expectedSize)}")
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            temp.deleteIfExists()
            throw IllegalStateException("Failed to download $displayName.", ex)
        } finally {
            progress.completed()
        }

        val actual = digest.digest().toHex()
        if (asset.sha256 != null) {
            if (!actual.equals(asset.sha256, ignoreCase = true)) {
                temp.deleteIfExists()
                LOGGER.lifecycle("Invalid SHA256 hash for downloaded file: '{}', deleting.", asset.name)
                logExpectedActual(asset.sha256, actual)
                error("Failed to verify SHA256 hash of downloaded file.")
            }
            LOGGER.lifecycle("Verified SHA256 hash of downloaded jar.")
        }
        temp.moveTo(target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        return actual
    }

    private fun readRecord(file: Path): Record? {
        if (!file.isRegularFile()) {
            return null
        }
        return try {
            val json = JsonParser.parseString(file.readText()).asJsonObject
            Record(json["id"].asLong, json["tag"].asString, json["updatedAt"].asString, json["sha256"].asString, json["lastCheck"].asLong)
        } catch (ex: Exception) {
            LOGGER.warn("Ignoring unreadable cache record {}: {}", file, ex.toString())
            null
        }
    }

    private fun writeRecord(file: Path, record: Record) {
        file.parent.createDirectories()
        file.writeText(
            JsonObject().apply {
                addProperty("id", record.id)
                addProperty("tag", record.tag)
                addProperty("updatedAt", record.updatedAt)
                addProperty("sha256", record.sha256)
                addProperty("lastCheck", record.lastCheck)
            }.toString(),
        )
    }

    private fun logExpectedActual(expected: String, actual: String) {
        LOGGER.lifecycle(" > Expected: {}", expected)
        LOGGER.lifecycle(" > Actual: {}", actual)
    }

    private data class Asset(val id: Long, val tag: String, val name: String, val url: String, val updatedAt: String, val sha256: String?)

    private data class Record(val id: Long, val tag: String, val updatedAt: String, val sha256: String, val lastCheck: Long) {
        fun sameAsset(asset: Asset): Boolean = id == asset.id && updatedAt == asset.updatedAt &&
            (asset.sha256 == null || sha256.equals(asset.sha256, ignoreCase = true))
    }

    private companion object {
        val LOGGER = Logging.getLogger(GithubReleaseDownloadsService::class.java)
        const val PROGRESS_INTERVAL_MS = 100L
        val UNSAFE_PATH_CHARS = Regex("[^A-Za-z0-9._-]")

        fun sha256(input: java.io.InputStream): String {
            val digest = MessageDigest.getInstance("SHA-256")
            DigestInputStream(input, digest).use { it.transferTo(java.io.OutputStream.nullOutputStream()) }
            return digest.digest().toHex()
        }

        fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

        fun formatProgress(downloaded: Long, expected: Long): String {
            val mib = { bytes: Long -> "%.1f".format(bytes / 1048576.0) }
            if (expected <= 0) {
                return "${mib(downloaded)} MiB"
            }
            return "${mib(downloaded)}/${mib(expected)} MiB (${downloaded * 100 / expected}%)"
        }

        fun globRegex(glob: String): Regex = Regex(glob.split('*').joinToString(".*") { Regex.escape(it) })
    }
}

fun Project.githubReleases(
    repository: String,
    assetPattern: String,
    tagFormat: String = "%s",
    displayName: String = repository.substringAfter('/'),
): Provider<GithubReleaseDownloadsService> {
    val serviceName = "github-${repository.replace('/', '-')}-${assetPattern.hashCode().toUInt().toString(16)}"
    return gradle.sharedServices.registerIfAbsent(serviceName, GithubReleaseDownloadsService::class) {
        parameters.repository.set(repository)
        parameters.assetPattern.set(assetPattern)
        parameters.tagFormat.set(tagFormat)
        parameters.displayName.set(displayName)
        parameters.cacheDirectory.set(gradle.gradleUserHomeDir.resolve("caches/run-task-jars/user/$serviceName"))
        parameters.refreshDependencies.set(gradle.startParameter.isRefreshDependencies)
        parameters.offlineMode.set(gradle.startParameter.isOffline)
        parameters.updateCheckFrequency.set(
            providers.gradleProperty("xyz.jpenilla.run-task.updateCheckFrequency").map(::parseDuration)
                .orElse(Duration.ofHours(1)),
        )
    }
}

private fun parseDuration(value: String): Duration {
    val parts = Regex("(\\d+)([wdhms])").findAll(value.trim())
    if (parts.joinToString("") { it.value } != value.trim() || parts.none()) {
        throw InvalidUserDataException("Unable to parse value for property 'xyz.jpenilla.run-task.updateCheckFrequency': '$value'")
    }
    return parts.fold(Duration.ZERO) { total, part ->
        val amount = part.groupValues[1].toLong()
        total + when (part.groupValues[2]) {
            "w" -> Duration.ofDays(amount * 7)
            "d" -> Duration.ofDays(amount)
            "h" -> Duration.ofHours(amount)
            "m" -> Duration.ofMinutes(amount)
            else -> Duration.ofSeconds(amount)
        }
    }
}
