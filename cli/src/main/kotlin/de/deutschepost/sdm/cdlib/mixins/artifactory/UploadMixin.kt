package de.deutschepost.sdm.cdlib.mixins.artifactory

import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryClient
import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryFolderSuffix
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.utils.*
import mu.KLogging
import picocli.CommandLine.Model.CommandSpec
import picocli.CommandLine.Option
import picocli.CommandLine.Spec
import java.io.File


class UploadMixin {
    @Option(
        names = ["--folder-name"],
        description = ["Folder name to be created/used inside the Artifactory repository.",
            "By default the environment variable CDLIB_RELEASE_NAME is used."]
    )
    var folderNameOption: String? = null

    @Option(
        names = ["--no-file-overwrite"],
        negatable = true,
        description = ["Whether present artifacts should be overridden.",
            "A timestamp is appended to the filename if the file is present and --no-file-overwrite is set.",
            "Default: --file-overwrite"]
    )
    var fileOverwrite = true

    @Deprecated("Remove with cdlib 7")
    @Option(
        names = ["--type"],
        description = ["Folder suffix that is added to --folder-name.",
            "Possible values are 'build' or 'release'. 'build' folders are expected to have SCA and SAST reports,",
            "while 'release' folders are expected to have a DAST report."]
    )
    lateinit var folderSuffixStr: String

    @Spec(Spec.Target.MIXEE)
    private lateinit var mixee: CommandSpec

    private val artifactoryMixinLight: ArtifactoryMixinLight by lazy {
        findMixinByType(mixee)
    }

    private val client: ArtifactoryClient by lazy {
        artifactoryMixinLight.client
    }

    private val repoName: String by lazy {
        artifactoryMixinLight.repoName
    }

    private val folder by lazy {
        val folderName = runCatching {
            (folderNameOption ?: resolveEnvByName(Names.CDLIB_RELEASE_NAME)).also {
                require(it.isNotBlank())
            }
        }.getOrElse {
            throw IllegalArgumentException("Cannot infer folderName either specify --folder-name option or environment variable CDLIB_RELEASE_NAME.")
        }

        val appName = System.getenv(Names.CDLIB_APP_NAME.name)
        if (appName.isNullOrBlank()) {
            logger.warn { "Environment variable ${Names.CDLIB_APP_NAME.name} is not set. Have you run cdlib names create?" }
        }

        checkNotNull(client.createFolder(repoName, folderName, artifactoryFolderSuffix())) {
            "Could not create folder."
        }
    }

    @JvmName("uploadJsonableList")
    fun upload(jsonables: List<Jsonable>) {
        jsonables.forEach { jsonable ->
            runCatching {
                val byteArrayInputStream = defaultObjectMapper.writeValueAsBytes(jsonable).inputStream()
                client.uploadInputStream(
                    repoName,
                    "$folder/${jsonable.canonicalFilename}",
                    byteArrayInputStream,
                    doOverwrites = fileOverwrite
                )
            }.getOrElse {
                it.klogSelf(logger)
                throw IllegalStateException("Error during upload of ${jsonable.canonicalFilename}")
            }
        }
    }

    @JvmName("uploadFileList")
    fun upload(files: List<File>) {
        check(client.uploadFiles(repoName, folder, fileOverwrite, files) == 0) {
            "Error during upload."
        }
    }


    private fun artifactoryFolderSuffix(): ArtifactoryFolderSuffix = runCatching {
        ArtifactoryFolderSuffix.valueOf(folderSuffixStr.uppercase())
    }.getOrElse {
        when (it) {
            is UninitializedPropertyAccessException -> logger.error { "Parameter --type has not been specified!" }
            is IllegalArgumentException -> logger.error { "Unsupported parameter for --type: $folderSuffixStr." }
            else -> logger.error { "ArtifactoryFolderSuffix creation has failed with Exception: ${it.message}" }
        }
        logger.error { "Supported values are: ${ArtifactoryFolderSuffix.values().joinToString()}" }
        throw it
    }

    companion object : KLogging()
}
