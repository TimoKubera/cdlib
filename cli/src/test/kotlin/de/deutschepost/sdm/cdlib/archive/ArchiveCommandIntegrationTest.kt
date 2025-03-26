package de.deutschepost.sdm.cdlib.archive

import de.deutschepost.sdm.cdlib.CdlibCommand
import de.deutschepost.sdm.cdlib.artifactory.AZURE_ARTIFACTORY_URL
import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryClient
import exists
import io.kotest.assertions.asClue
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import org.jfrog.artifactory.client.Artifactory
import org.jfrog.artifactory.client.model.File
import org.jfrog.artifactory.client.model.Folder
import toArgsArray
import java.io.File.separatorChar

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ArchiveCommandIntegrationTest(@Value("\${artifactory-azure-identity-token}") val artifactoryIdentityToken: String) :
    StringSpec() {
    private val repoName = "ICTO-3339_sdm_sockshop_release_reports"
    private val releaseName = "cdlib-cli-integration_42.1337.42"
    private val releaseName_build = releaseName + "_build"
    private val artifactoryClient = ArtifactoryClient(artifactoryIdentityToken, AZURE_ARTIFACTORY_URL)
    private val artifactory by lazy {
        val declaredField = artifactoryClient.javaClass.getDeclaredField("artifactory")
        declaredField.isAccessible = true
        declaredField.get(artifactoryClient) as Artifactory
    }

    init {
        "create folder with release name and upload file" {
            withEnvironment("CDLIB_RELEASE_NAME" to "", OverrideMode.SetOrOverride) {
                val repositoryHandle = artifactory.repository(repoName)
                repositoryHandle.folder(releaseName_build).exists() shouldBe false

                run {
                    val args =
                        "--repo-name $repoName --artifactory-azure-instance --artifactory-identity-token $artifactoryIdentityToken --folder-name $releaseName --type build --debug".toArgsArray()
                    val ret = PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)

                    ret shouldBeExactly 0
                    repositoryHandle.folder(releaseName_build).exists() shouldBe true
                }

                run {
                    val args =
                        "--repo-name $repoName --artifactory-azure-instance --artifactory-identity-token $artifactoryIdentityToken --files src/test/resources/blobs/kotlin.png --folder-name $releaseName --type build --debug".toArgsArray()
                    val ret = PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)

                    ret shouldBeExactly 0
                    repositoryHandle.folder(releaseName_build).exists() shouldBe true
                    repositoryHandle.file("$releaseName_build/kotlin.png").exists() shouldBe true
                }
            }
        }

        "Reuse folder and test file-overwrite" {
            withEnvironment("CDLIB_RELEASE_NAME" to releaseName, OverrideMode.SetOrOverride) {
                val repositoryHandle = artifactory.repository(repoName)
                repositoryHandle.folder(releaseName_build).exists() shouldBe false

                val originalModifiedDate = run {
                    val args =
                        "--repo-name $repoName --artifactory-azure-instance --artifactory-identity-token $artifactoryIdentityToken --files src/test/resources/blobs/kotlin.png --type build --debug".toArgsArray()
                    val ret = PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)
                    val file = repositoryHandle.file("$releaseName_build/kotlin.png")

                    ret shouldBeExactly 0
                    repositoryHandle.folder(releaseName_build).exists() shouldBe true
                    file.exists() shouldBe true

                    file.info<File>().lastModified
                }

                run {
                    val args =
                        "--repo-name $repoName --artifactory-azure-instance --artifactory-identity-token $artifactoryIdentityToken --files src/test/resources/blobs/kotlin.png --type build --file-overwrite --debug".toArgsArray()
                    val ret = PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)
                    val file = repositoryHandle.file("$releaseName_build/kotlin.png")

                    ret shouldBeExactly 0
                    repositoryHandle.folder(releaseName_build).exists() shouldBe true
                    file.exists() shouldBe true

                    file.info<File>().lastModified shouldNotBe originalModifiedDate
                }
            }
        }

        "Reuse folder and test no-file-overwrite" {
            withEnvironment("CDLIB_RELEASE_NAME" to releaseName, OverrideMode.SetOrOverride) {
                val repositoryHandle = artifactory.repository(repoName)
                repositoryHandle.folder(releaseName_build).exists() shouldBe false

                run {
                    val args =
                        "--repo-name $repoName --artifactory-azure-instance --artifactory-identity-token $artifactoryIdentityToken --files src/test/resources/blobs/kotlin.png --type build --debug".toArgsArray()
                    val ret = PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)

                    ret shouldBeExactly 0
                    repositoryHandle.folder(releaseName_build).exists() shouldBe true
                    repositoryHandle.file("$releaseName_build/kotlin.png").exists() shouldBe true
                    repositoryHandle.folder(releaseName_build).info<Folder>().children.size shouldBe 1


                }

                run {
                    val args =
                        "--repo-name $repoName --artifactory-azure-instance --artifactory-identity-token $artifactoryIdentityToken --files src/test/resources/blobs/kotlin.png --type build --no-file-overwrite --debug".toArgsArray()
                    val ret = PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)

                    ret shouldBeExactly 0
                    repositoryHandle.folder(releaseName_build).exists() shouldBe true
                    repositoryHandle.file("$releaseName_build/kotlin.png").exists() shouldBe true
                    repositoryHandle.folder(releaseName_build).info<Folder>().children.asClue {
                        it.size shouldBe 2
                        it.all { item -> item.name.contains("kotlin") } shouldBe true
                    }
                }
            }
        }

        "Upload multiple files to folder" {
            withEnvironment("CDLIB_RELEASE_NAME" to releaseName, OverrideMode.SetOrOverride) {
                val repositoryHandle = artifactory.repository(repoName)
                repositoryHandle.folder(releaseName_build).exists() shouldBe false

                run {
                    val args =
                        "--repo-name $repoName --artifactory-azure-instance --artifactory-identity-token $artifactoryIdentityToken --type build --debug --files src/test/resources/blobs/kotlin.png --files ..$separatorChar**${separatorChar}azure.png -f **${separatorChar}adr$separatorChar*use*.md".toArgsArray()
                    val ret = PicocliRunner.call(ArchiveCommand.UploadCommand::class.java, *args)

                    ret shouldBeExactly 0
                    repositoryHandle.folder(releaseName_build).exists() shouldBe true
                    repositoryHandle.file("$releaseName_build/kotlin.png").exists() shouldBe true
                    repositoryHandle.file("$releaseName_build/azure.png").exists() shouldBe true
                    repositoryHandle.folder(releaseName_build).info<Folder>().children.size shouldBe 6
                }
            }
        }

        "Upload file via full call" {
            withEnvironment("CDLIB_RELEASE_NAME" to releaseName, OverrideMode.SetOrOverride) {
                val repositoryHandle = artifactory.repository(repoName)
                repositoryHandle.folder(releaseName_build).exists() shouldBe false

                run {
                    val args =
                        "archive upload --repo-name $repoName --artifactory-azure-instance --artifactory-identity-token $artifactoryIdentityToken --type build --debug --files src/test/resources/blobs/kotlin.png".toArgsArray()
                    PicocliRunner.run(CdlibCommand::class.java, *args)
                    repositoryHandle.folder(releaseName_build).exists() shouldBe true
                    repositoryHandle.file("$releaseName_build/kotlin.png").exists() shouldBe true

                }
            }
        }
    }

    override suspend fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        val repository = artifactory.repository(repoName)
        try {
            repository.delete(releaseName_build)
        } catch (_: Exception) {
            // Exception intentionally ignored because deletion may fail if the release does not exist.
        }

    }
}
