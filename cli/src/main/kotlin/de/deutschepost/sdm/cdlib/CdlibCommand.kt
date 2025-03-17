package de.deutschepost.sdm.cdlib

import de.deutschepost.sdm.cdlib.archive.ArchiveCommand
import de.deutschepost.sdm.cdlib.build.BuildCommand
import de.deutschepost.sdm.cdlib.change.ChangeCommand
import de.deutschepost.sdm.cdlib.names.NamesCommand
import de.deutschepost.sdm.cdlib.release.ReportCommand
import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import picocli.CommandLine
import picocli.CommandLine.*
import kotlin.system.exitProcess

@Command(
    name = "cdlib",
    description = ["The CDlib cli will aid automate the build, release and change flow in conjunction with the ISHP (Information Security Hardening Program)"],
    subcommands = [
        ArchiveCommand::class, BuildCommand::class, NamesCommand::class, ReportCommand::class, ChangeCommand::class
    ]
)
class CdlibCommand : Runnable, SubcommandWithHelp() {


    override fun run() {
        enableDebugIfOptionIsSet()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {

            val context = ApplicationContext.builder(CdlibCommand::class.java, Environment.CLI).start()
            printVersionInfo()
            exitProcess(
                CommandLine(CdlibCommand::class.java, MicronautFactory(context))
                    .setUsageHelpWidth(140)
                    .execute(*args)
            )
        }
    }
}
