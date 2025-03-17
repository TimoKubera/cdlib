package de.deutschepost.sdm.cdlib.names

import de.deutschepost.sdm.cdlib.SubcommandWithHelp
import jakarta.inject.Inject
import mu.KLogging
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.File
import java.io.PrintStream
import java.util.concurrent.Callable

@Command(
    name = "names",
    description = ["Contains subcommands for automatic name and ID creation in pipeline."],
    subcommands = [
        NamesCommand.CreateCommand::class,
        NamesCommand.DumpCommand::class
    ],
)
class NamesCommand : SubcommandWithHelp() {

    @Command(
        name = "create",
        description = ["Create canonical set of standard names and IDs for standard pipeline tasks."]
    )
    class CreateCommand : SubcommandWithHelp(), Callable<Int> {
        @Option(
            names = ["-f", "--outfile"], required = false,
            description = [
                "Name of optional output file",
                "Default: STDOUT is used"
            ]
        )
        var fileName: String? = null

        @Option(
            names = ["--from-release-name"], required = false,
            description = [
                "Release name to use for derived name creation (see below)",
                "Syntax: <component>_<version>_<buildid>_<revision>",
                "  (four elements separated by underscore)",
                "Sample: frontend_20211203.1827.54_25_a5c5bc3"
            ]
        )
        var fromReleaseName: String = ""

        @Option(
            names = ["-g", "--gitpath"], required = false,
            description = [
                "Path to git repository for current build",
                "Default: Current working directory"
            ]
        )
        var gitpath: String = ""

        @Option(
            names = ["--override-origin"], required = false,
            description = [
                "Url to be use as origin",
                "Default: Current origin from git"
            ]
        )
        var originOverride: String? = null

        @Inject
        lateinit var namesConfigWithDefault: NamesConfigWithDefault

        @Inject
        lateinit var factory: NameProviderFactory

        override fun call(): Int {
            enableDebugIfOptionIsSet()

            logger.info { "Current working directory: ${System.getProperty("user.dir")}" }

            val provider = factory.getProvider()
            logger.info { "Detected runtime platform: ${provider.platformType.name}" }

            // Overriding gitpath?
            if (gitpath.isNotBlank()) {
                logger.info("Using repository path  $gitpath...")
                namesConfigWithDefault.repoPath = gitpath
            }

            //override origin
            if (originOverride != null) {
                logger.info { "Using override origin url: $originOverride" }
                namesConfigWithDefault.originOverride = originOverride
            }

            // Overriding release name provided?
            if (fromReleaseName.isNotEmpty()) {
                logger.info("Using provided release ID $fromReleaseName ...")
                try {
                    provider.resolver.seed(fromReleaseName)
                } catch (exception: IllegalArgumentException) {
                    logger.error { exception.message }
                    return -1
                }
            }

            // Create names for target platform
            val result = provider.provideNames()
            val count = result.split("\n").filter { it.isNotBlank() }.size
            logger.info("Created $count names")

            // Create PrintStream for output
            withOutputStream(fileName) { ps ->
                ps.append(result)

                // Additional for Jenkins wrapper step as it cannot parse the exit code
                if (currentPlatformType == PlatformType.JENKINS) {
                    ps.append("CDLIB_NAMES_CREATE_SUCCESS=1")
                }
            }
            return 0
        }
    }

    @Command(
        name = "dump",
        description = ["Dumps the list of environment variables"],
    )
    class DumpCommand : SubcommandWithHelp(), Callable<Int> {
        @Option(
            names = ["-f", "--outfile"], required = false,
            description = [
                "Name of optional output file",
                "Default: STDOUT is used"
            ]
        )
        var fileName: String? = null

        override fun call(): Int {
            enableDebugIfOptionIsSet()

            logger.info { "Current working directory: ${System.getProperty("user.dir")}" }
            logger.info { "Detected runtime platform: ${currentPlatformType.name}" }

            withOutputStream(fileName) { ps ->
                System.getenv().forEach { (k, v) -> ps.println("$k = $v") }
            }
            return 0
        }
    }

    companion object : KLogging() {
        private fun withOutputStream(fileName: String?, ps: (PrintStream) -> Unit) {
            if (fileName != null) {
                logger.debug { "Using $fileName as output target" }
                val file = File(fileName)
                file.delete()
                file.createNewFile()
                PrintStream(file).use {
                    ps(it)
                }
            } else {
                logger.debug { "Using STDOUT as output target" }
                ps(System.out)
            }
        }
    }
}
