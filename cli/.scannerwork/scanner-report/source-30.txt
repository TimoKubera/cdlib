package de.deutschepost.sdm.cdlib.archive

import de.deutschepost.sdm.cdlib.SubcommandWithHelp
import de.deutschepost.sdm.cdlib.mixins.FilesMixin
import de.deutschepost.sdm.cdlib.mixins.artifactory.ArtifactoryMixinLight
import de.deutschepost.sdm.cdlib.mixins.artifactory.UploadMixin
import de.deutschepost.sdm.cdlib.utils.klogSelf
import mu.KLogging
import picocli.CommandLine.Command
import picocli.CommandLine.Mixin
import java.io.File
import java.util.concurrent.Callable

@Command(
    name = "archive",
    description = ["Lets you interact with artifacts."],
    subcommands = [ArchiveCommand.UploadCommand::class]
)
class ArchiveCommand : SubcommandWithHelp() {

    @Command(name = "upload", description = ["Uploads an artifact to Artifactory."])
    class UploadCommand : SubcommandWithHelp(), Callable<Int> {
        @Mixin
        lateinit var uploadMixin: UploadMixin

        @Mixin
        lateinit var filesMixin: FilesMixin

        @Suppress("UNUSED")
        @Mixin
        lateinit var artifactoryMixinLight: ArtifactoryMixinLight

        override fun call(): Int = runCatching {
            enableDebugIfOptionIsSet()
            val files: List<File> = filesMixin.getFiles()
            uploadMixin.upload(files)
            0
        }.getOrElse {
            it.klogSelf(logger)
            -1
        }
    }

    companion object : KLogging()
}
