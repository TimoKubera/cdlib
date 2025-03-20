package de.deutschepost.sdm.cdlib.artifactory

import your.package.cdlib7
import io.micronaut.http.HttpHeaders
import mu.KLogging
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.ArtifactoryClientBuilder
import org.jfrog.artifactory.client.ItemHandle
import org.jfrog.artifactory.client.RepositoryHandle
import org.jfrog.artifactory.client.model.Folder
import org.jfrog.artifactory.client.model.Item
import org.jfrog.artifactory.client.model.SearchResult
import org.jfrog.artifactory.client.model.SearchResultImpl
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import org.jfrog.artifactory.client.model.File as ArtifactoryFile


class ArtifactoryClient(
    private val apiKeyOrIdentityToken: String,
    artifactoryUrl: String
) {
    private val artifactoryUrlGui: String = artifactoryUrl + ARTIFACTORY_GUI
    private val artifactoryUrlApi: String = artifactoryUrl + ARTIFACTORY_API

    private val artifactory: Artifactory = runCatching {
        val isApiKey = apiKeyOrIdentityToken.startsWith("AKC")
        if (isApiKey) {
            logger.warn { "Artifactory API-KEY is DEPRECATED and support will be removed in near future!" }
            logger.warn { "Use an Artifactory Identity Token instead!" }
        }

        ArtifactoryClientBuilder
            .create()
            .apply {
                url = artifactoryUrlApi
                connectionTimeout = 30_000
                socketTimeout = 30_000
                addInterceptorLast { request, _ ->
                    if (isApiKey) {
                        request.addHeader("X-JFrog-Art-Api", apiKeyOrIdentityToken)
                    } else {
                        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer $apiKeyOrIdentityToken")
                    }
                }
            }
            .build()
    }.onFailure {
        logger.error { "Failed to create ArtifactoryClient: ${it.message}" }
    }.getOrThrow()

    fun createFolder(
        repoName: String,
        baseFolderName: String,
        folderSuffix: ArtifactoryFolderSuffix
    ): String? =
        runCatching {
            val folderName = cdlib7.constructFolderName(baseFolderName, folderSuffix)
            val repoHandle = repositoryAndCheck(repoName)

            repoHandle.get().apply {
                logger.debug {
                    """Repository Information:
                        |Key: $key
                        |Description: $description
                    """.trimMargin()
                }
            }

            val folderHandle = repoHandle.folder(folderName)
            if (folderHandle.exists()) {
                logger.warn { "Folder $folderName already exists." }
            } else {
                folderHandle.create()
            }

            addProperty(
                repoName,
                folderName,
                ARTIFACTORY_ARCHIVE_TYPE_PROPERTY to folderSuffix.toString(),
            )
            folderHandle.info<Folder>().apply {
                logger.info {
                    """Folder Information:
                    |URI: ${artifactoryUrlGui}/$repoName/$folderName
                    |${toString()}
                """.trimMargin()
                }
            }

            return@runCatching folderName
        }.onFailure {
            logger.error { "Failed to create Folder in Artifactory: ${it.message}" }
        }.getOrNull()

    fun uploadFiles(repoName: String, folderName: String, overwriteFiles: Boolean, files: List<File>): Int =
        runCatching {
            val repoHandle = artifactory.repository(repoName)
            val timestamp by lazy {
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS))
            }
            val filesToPath =
                files.associate { file ->
                    val path = "$folderName/${file.name}"
                    if (repoHandle.file(path).exists()) {
                        if (overwriteFiles) {
                            logger.info { "File $path already exists. It will be overridden." }
                            file to path
                        } else {
                            val newPath = "$folderName/${file.nameWithoutExtension}_${timestamp}.${file.extension}"
                            logger.info { "File $path already exists. It will not be overridden. The new path is: $newPath" }
                            file to newPath
                        }
                    } else {
                        logger.debug { "File $path does not exist." }
                        file to path
                    }
                }

            filesToPath.forEach { (file, path) ->
                uploadInputStream(repoHandle, path, file.inputStream(), overwriteFiles)
            }

            return@runCatching 0
        }.onFailure {
            logger.error { "Failed to upload files to Artifactory: ${it.message}" }
        }.getOrDefault(-1)

    private fun uploadInputStream(
        repoHandle: RepositoryHandle,
        path: String,
        inputStream: InputStream,
        doOverwrites: Boolean
    ) {
        if (!doOverwrites && repoHandle.file(path).exists()) {
            throw IllegalStateException("File $path already exists and no Overwrite is allowed.")
        }

        val artifactoryFile = repoHandle
            .upload(path, inputStream)
            .doUpload() as ArtifactoryFile

        logger.debug {
            """Uploaded Artifact:
                |$artifactoryFile
                """.trimMargin()
        }
    }

    fun uploadInputStream(
        repoName: String,
        path: String,
        inputStream: InputStream,
        doOverwrites: Boolean = false
    ) {
        val repoHandle = artifactory.repository(repoName)
        uploadInputStream(repoHandle, path, inputStream, doOverwrites)
    }

    fun uploadInputStreamNewVersion(
        repoName: String,
        path: String,
        inputStream: InputStream
    ) {
        val repo = repositoryAndCheck(repoName)
        val file = repo.file(path).info<ArtifactoryFile>()
        val newVersion = repo.getNewestFileVersion(file.path) + 1
        val newPath = path.substringBeforeLast(file.name) + constructFileVersion(file.name, newVersion)

        logger.warn { "$path already exists but has a different hash! Uploading new version as: $newPath" }
        uploadInputStream(repoName, newPath, inputStream)
    }

    private fun getAllFilesInFolder(
        rootFolder: Folder, repoHandle: RepositoryHandle
    ): List<ArtifactoryFile> {
        val folders = mutableListOf(rootFolder)
        return sequence {
            while (folders.isNotEmpty()) {
                val folder = folders.removeFirst()
                folder.children.forEach { child ->
                    if (child.isFolder) {
                        folders.add(repoHandle.folder("${folder.path}/${child.name}").info<Folder>())
                    } else {
                        yield(repoHandle.file("${folder.path}/${child.name}").info<ArtifactoryFile>())
                    }
                }
            }
        }.toList()
    }

    private fun copyAndCheckFilesInFolder(
        srcRepoName: String,
        srcFolderName: String,
        dstRepoName: String,
        dstFolderName: String
    ) {
        val srcRepo = repositoryAndCheck(srcRepoName)
        val srcFolderHandle = srcRepo.folderAndCheck(srcFolderName)
        val srcFolderItem = srcFolderHandle.info<Folder>()

        val files = getAllFilesInFolder(srcFolderItem, srcRepo)

        val brokenFiles = sequence {
            files.forEach { file ->
                val relativePath = file.path.substringAfter(srcRepoName)
                val dstFilePath = "$dstFolderName$relativePath"
                logger.debug { "Copying file: ${file.path} to $dstFilePath" }
                runCatching {
                    val srcFileHandle = srcRepo.file(file.path)
                    val dstFilePathEncoded = URLEncoder.encode(dstFilePath, Charsets.UTF_8)

                    val dstRepo = repositoryAndCheck(dstRepoName)
                    if (dstRepo.file(dstFilePath).exists()) {
                        throw IllegalStateException("File $dstFilePath already exists.")
                    }
                    srcFileHandle.copy(dstRepoName, dstFilePathEncoded)
                }.getOrElse { exception ->
                    if (exception.message?.contains("File $dstFilePath already exists.") == true) {
                        logger.warn { "IllegalStateException during copying. Checking if artifact already exists..." }
                        if (getSha256Sum(srcRepoName, file.path) != getSha256Sum(dstRepoName, dstFilePath)) {
                            logger.error { "$dstFilePath does exist and checksum does not match" }
                            yield(dstFilePath)
                        } else {
                            logger.info("$dstFilePath does exist and checksum matches!")
                            null
                        }
                    } else {
                        throw exception
                    }
                }
            }
        }.toList()

        check(brokenFiles.isEmpty()) {
            "Copying files has failed. These files already exist in the target folder with different checksum: $brokenFiles"
        }
    }

    fun copyFiles(
        srcRepoName: String,
        srcFolderNames: List<String>,
        dstRepoName: String,
        dstFolderName: String,
    ): String? = runCatching {
        val dstRepoHandle = artifactory.repository(dstRepoName)
        val dstUrl = "${artifactoryUrlGui}/$dstRepoName/$dstFolderName"

        srcFolderNames.forEach { folderName ->
            copyAndCheckFilesInFolder(srcRepoName, folderName, dstRepoName, dstFolderName)
        }
        logger.info { "Successfully copied artifacts to immutable repository." }
        logger.info { "ArtifactsUrl: $dstUrl" }

        return dstUrl
    }.onFailure {
        logger.error { "Failed to copy files in Artifactory: ${it.message}" }
        throw it
    }.getOrNull()

    @Deprecated("Remove with cdlib 7")
    private fun addProperty(repoName: String, path: String, vararg property: Pair<String, String?>) {
        val repoHandle = repositoryAndCheck(repoName)
        // Setting properties works for files and folders as it implemented for ItemHandler
        repoHandle.file(path).properties().apply {
            property.forEach {
                if (it.second != null) {
                    logger.debug { "Setting property: ${it.first} to ${it.second}" }
                    addProperty(it.first, it.second)
                }
            }
            doSet()
        }
    }

    fun getFolderChildren(repoName: String, path: String, childIsFolder: Boolean): List<String> = runCatching {
        val repoHandle = repositoryAndCheck(repoName)
        val folder = repoHandle.folderAndCheck(path)

        return@runCatching folder.info<Folder>().children.mapNotNull { child ->
            if (child.isFolder == childIsFolder) {
                logger.info { "Found child ${child.name}." }
                child.name
            } else {
                null
            }
        }
    }.onFailure {
        logger.error { "Could not fetch children for $path! Did you upload your reports?" }
    }.getOrDefault(emptyList())

    fun downloadFile(repoName: String, path: String): InputStream? = runCatching {
        val repoHandle = repositoryAndCheck(repoName)
        repoHandle.download(path).doDownload()
    }.onFailure {
        logger.error { "Error downloading $path." }
    }.getOrNull()


    fun downloadLatestFile(repoName: String, path: String): InputStream? = runCatching {
        val repoHandle = repositoryAndCheck(repoName)
        val latestPath = constructFileVersion(path, repoHandle.getNewestFileVersion(path))
        logger.info { "Found file $latestPath for given path $path" }
        repoHandle.download(latestPath).doDownload()
    }.onFailure {
        logger.error { "Error downloading $path." }
    }.getOrNull()

    fun getSha256Sum(repoName: String, path: String, maxTries: Int = 3): String? {
        for (i in 1..maxTries) {
            logger.debug { "Trying to fetch checksum for $path... Attempt $i/$maxTries" }
            runCatching {
                val fileHandle = artifactory.repository(repoName).file(path)
                fileHandle.info<ArtifactoryFile>().checksums.sha256
            }.onFailure {
                logger.error { "Couldn't fetch checksum for $path" }
                Thread.sleep(200)
            }.onSuccess {
                return it
            }
        }
        return null
    }

    fun findFirstByNameAndChecksum(repoName: String, folderName: String, name: String, checksum: String): String? {
        val apiBase = Artifactory.API_BASE
        val searchResults = artifactory.get(
            "$apiBase/search/checksum?sha256=$checksum&repos=$repoName",
            SearchResultImpl::class.java,
            SearchResult::class.java
        )
        return searchResults.results.map {
            val fullPath = it.uri.split("$apiBase/storage/")[1]
            val repo = fullPath.substring(0, fullPath.indexOf('/'))
            val itemPath = fullPath.removePrefix("$repo/")
            itemPath
        }.firstNotNullOfOrNull { itemPath ->
            if (itemPath.substringBefore("/") == folderName && itemPath.endsWith(name)) {
                itemPath
            } else {
                logger.debug { "ItemPath: $itemPath doesn't match immutable folder $folderName and fileName $name" }
                null
            }
        }


    }


    companion object : KLogging()

    private fun repositoryAndCheck(repoName: String): RepositoryHandle =
        artifactory.repository(repoName).also {
            check(it.exists()) {
                logger.error("Repository $repoName doesn't exist.")
                logger.error { "Try logging in manually as your pipeline user, to enable API access again, if you think it should be there." }
            }
        }

    private fun RepositoryHandle.fileAndCheck(path: String): ItemHandle =
        file(path).also {
            check(it.exists()) {
                logger.error { "Could not find file: $path." }
            }
        }

    private fun RepositoryHandle.folderAndCheck(path: String): ItemHandle =
        folder(path).also {
            check(it.exists()) {
                logger.error { "Could not find folder: $path." }
            }
        }

}

fun ItemHandle.exists(): Boolean {
    return try {
        info<Item>()
        true
    } catch (e: IOException) {
        false
    }
}

private fun RepositoryHandle.getNewestFileVersion(path: String): Int {
    val versionSequence = generateSequence(0) { it + 1 }
    return versionSequence.takeWhile { file(constructFileVersion(path, it)).exists() }.last()
}

private fun constructFileVersion(path: String, version: Int): String {
    return if (version <= 0) {
        path
    } else {
        val ext = path.substringAfterLast('.', "")
        val fileName = path.substringBeforeLast('.')
        "${fileName}_${version.toString().padStart(3, '0')}.$ext"
    }
}
