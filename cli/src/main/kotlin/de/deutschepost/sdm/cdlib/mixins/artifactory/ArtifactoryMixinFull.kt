package de.deutschepost.sdm.cdlib.mixins.artifactory

import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryClient
import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryInstanceSection
import de.deutschepost.sdm.cdlib.change.metrics.REPORT_PREFIX
import de.deutschepost.sdm.cdlib.change.metrics.model.Report
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.release.report.internal.ReportType
import de.deutschepost.sdm.cdlib.release.report.internal.TestResult
import de.deutschepost.sdm.cdlib.utils.Jsonable
import de.deutschepost.sdm.cdlib.utils.defaultObjectMapper
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName
import de.deutschepost.sdm.cdlib.utils.sha256sum
import mu.KLogging
import picocli.CommandLine.*
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Webapproval specific interface for artifactory.
 * Since some files are uploaded to the immutable folder directly,
 * this doesn't reuse the UploadMixin.
 */
class ArtifactoryMixinFull {
    @Spec(Spec.Target.MIXEE)
    lateinit var mixee: Model.CommandSpec

    @ArgGroup(
        validate = true,
        exclusive = false,
        heading = "Options for Artifactory Options for Artifactory (required in case you don't have --no-oslc --no-tqs --no-webapproval):%n",
        multiplicity = "0..1",
        order = 8
    )
    var artifactorySection: ArtifactorySection = ArtifactorySection()

    class ArtifactorySection {
        @Option(
            names = ["--api-key", "--artifactory-api-key", "--artifactory-identity-token"],
            description = ["Artifactory Identity Token according to CDlib tutorial"]
        )
        lateinit var artifactoryIdentityToken: String

        @Option(names = ["--repo-name"], description = ["Name of the repository inside Artifactory."])
        lateinit var repoName: String

        @Option(
            names = ["--immutable-repo-name"],
            description = ["Name of the repository inside Artifactory, where your security reports of production releases will be copied to for eternity."]
        )
        var immutableRepoName: String = ""
            get() {
                require(field.isNotBlank()) {
                    "--immutable-repo-name is required for release verification."
                }
                return field
            }

        @Option(
            names = ["--folder-name"],
            description = ["Name of the folder inside Artifactory repository containing your reports.", "Can be specified multiple times."]
        )
        var folderNames: List<String> = emptyList()
            get() {
                require(field.isNotEmpty()) {
                    "At least one --folder-name is required for release verification."
                }
                return field
            }

        @ArgGroup(
            validate = true,
            exclusive = true,
            heading = "Options for Artifactory Instances",
            multiplicity = "0..1",
            order = 8
        )
        var artifactoryInstanceSection = ArtifactoryInstanceSection()


    }

    val client by lazy {
        ArtifactoryClient(
            apiKeyOrIdentityToken = artifactorySection.artifactoryIdentityToken,
            artifactoryUrl = artifactoryUrl
        )
    }

    private val artifactoryUrl by lazy {
        artifactorySection.artifactoryInstanceSection.selectArtifactoryUrl()
    }

    private val immutableFolderName: String by lazy {
        buildString {
            append(resolveEnvByName(Names.CDLIB_RELEASE_NAME_UNIQUE))
            if (mixee.findOption("--test").getValue()) {
                append("_TEST")
            }
        }
    }

    fun uploadJsonable(data: Jsonable) = uploadToImmutable(
        defaultObjectMapper.writeValueAsBytes(data),
        "$immutableFolderName/${data.canonicalFilename}",
        countUpIfExisting = true
    )

    fun uploadSuppressions(data: Map<*, *>) = uploadToImmutable(
        defaultObjectMapper.writeValueAsBytes(data),
        "$immutableFolderName/security_suppressions.json"
    )

    fun uploadToImmutable(byteArray: ByteArray, path: String, countUpIfExisting: Boolean = false) {

        runCatching {
            val byteArrayInputStream = byteArray.inputStream()
            client.uploadInputStream(artifactorySection.immutableRepoName, path, byteArrayInputStream)
        }.getOrElse { exception ->
            if (exception is IllegalStateException) {
                val upHash = client.getSha256Sum(artifactorySection.immutableRepoName, path)
                val downHash = byteArray.sha256sum()
                when {
                    upHash == downHash -> {
                        logger.info("Artifact with same hash exists!")
                    }

                    countUpIfExisting -> {
                        val byteArrayInputStream = byteArray.inputStream()
                        client.uploadInputStreamNewVersion(
                            artifactorySection.immutableRepoName,
                            path,
                            byteArrayInputStream
                        )
                    }

                    else -> {
                        logger.error { "$path already exists but has a different hash!" }
                        throw IllegalStateException("$path already exists but has a different hash!")
                    }
                }
            } else {
                throw exception
            }
        }
    }

    fun copyFiles(): String {
        return checkNotNull(
            client.copyFiles(
                artifactorySection.repoName,
                artifactorySection.folderNames,
                artifactorySection.immutableRepoName,
                immutableFolderName
            )
        ) {
            "Unable to copy files to ${artifactorySection.immutableRepoName}!"
        }
    }

    val artifactoryReports: List<Report> by lazy {
        artifactorySection.folderNames.flatMap { folder ->
            logger.debug { folder }
            // TODO: Remove TQS filter with 7.0, workaround so in the case of old TQS reports being present with a new change, the TestResultParser doesn't break
            val filesWithoutTQS = client.getFolderChildren(artifactorySection.repoName, folder, childIsFolder = false)
                .filter { !it.startsWith("${REPORT_PREFIX}_TQS") }
            val filesPartition = filesWithoutTQS.partition {
                it.startsWith(REPORT_PREFIX)
            }.also { logger.debug { it } }
            val reports = filesPartition.first.map {
                defaultObjectMapper.readValue(
                    client.downloadFile(artifactorySection.repoName, "$folder/$it"), Report::class.java
                )
            }
            check(verifyHashes(reports, filesPartition.second, folder))
            reports
        }
    }

    val oslcReports: List<Report> by lazy {
        filterReportsByType(ReportType.OSLC)
    }

    val oslcReportsWithMetadata: List<TestResultWithMetadata> by lazy {
        createTestResultWithMetadata(oslcReports)
    }

    private fun filterReportsByType(reportType: ReportType) =
        artifactoryReports.filter { it.test.reportType == reportType }

    private fun createTestResultWithMetadata(reports: List<Report>) = reports.map {
        val path =
            client.findFirstByNameAndChecksum(
                artifactorySection.immutableRepoName,
                immutableFolderName,
                it.test.uri,
                it.testHash
            )
                ?: throw RuntimeException("Cannot find TQS Report with name: ${it.test.uri} and sha256: ${it.testHash}")
        val url = directDownloadUrl(artifactorySection.immutableRepoName, path)

        TestResultWithMetadata(appName = it.name, testResult = it.test, url = url)
    }

    private fun verifyHashes(
        reports: List<Report>,
        tests: List<String>,
        folder: String,
    ): Boolean {
        val strikes = mutableListOf<String>()
        reports.forEach { report ->
            val testFile = tests.find { it == report.test.uri }
            val sha256sum = client.getSha256Sum(artifactorySection.repoName, "$folder/$testFile")
            if ((testFile == null) or (sha256sum != report.testHash)) {
                strikes.add(report.test.uri)
            }
        }
        return strikes.isEmpty().also {
            if (!it) {
                logger.warn { "Couldn't find matching file for:" }
                strikes.forEach {
                    logger.warn { it }
                }
            }
        }
    }

    private fun directDownloadUrl(repo: String, filePath: String): String {
        val pathEncoded = URLEncoder.encode(filePath, StandardCharsets.UTF_8)
        return "$artifactoryUrl/ui/api/v1/download?repoKey=$repo&path=$pathEncoded&isNativeBrowsing=false"
    }

    companion object : KLogging()
}

data class TestResultWithMetadata(val testResult: TestResult, val url: String, val appName: String)
