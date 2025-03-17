package de.deutschepost.sdm.cdlib.mixins

import io.micronaut.core.util.AntPathMatcher
import io.micronaut.core.util.PathMatcher
import mu.KLogging
import picocli.CommandLine.Option
import java.io.File

open class FilesMixin {
    @Option(
        names = ["-f", "--files"],
        description = ["Path to a file or an Ant Style Pattern:",
            "https://docs.micronaut.io/latest/api/io/micronaut/core/util/AntPathMatcher.html",
            "This parameter can be specified multiple times!"]
    )
    var fileOrPatterns: List<String> = emptyList()

    @Deprecated("Only for interim purposes. Will be removed in 7.0.")
    var hasTqs = false

    private val matcher: AntPathMatcher = PathMatcher.ANT

    fun getFiles(tqsPrefix: String = "TQS_Reports"): List<File> {
        return fileOrPatterns.flatMap { fileOrPattern ->
            if (fileOrPattern.contains(tqsPrefix)) {
                logger.warn {
                    """
                        DEPRECATION WARNING:
                        No reports were processed as only TQS files were found. Their processing via cdlib-cli is
                        currently deprecated and skipped. They will be removed from the cli in 7.0.
                        Moving to 7.0 without adjusting your TQS reporting will break your pipeline!
                        """.trimIndent()
                }
                hasTqs = true
                emptyList()
            } else if (matcher.isPattern(fileOrPattern)) {
                require(!fileOrPattern.startsWith(File.separatorChar)) {
                    "Ant style pattern is only supported for relative paths. Got: $fileOrPattern"
                }
                val pattern = fileOrPattern.substringAfterLast("..${File.separatorChar}")
                // The double File is a workaround. The absolutePath is correctly calculated but the exists tests fails (at least on Windows)
                val path = File(File(fileOrPattern.removeSuffix(pattern)).absolutePath)
                logger.info { "Got base path: ${path.absolutePath} and pattern: $pattern" }
                require(path.exists()) {
                    "Path ${path.absolutePath} does not exist."
                }
                require(path.absolutePath != "/") {
                    """Base path: '/' is not supported.
                            | You are probably using docker directly and mounting your test reports in the root directory.
                            | Instead of this:
                            | docker run -v ${'$'}(pwd):/docs cdlib-cli:6.latest cdlib ...
                            | Use this:
                            | docker run -v ${'$'}(pwd):/tmp/docs -w /tmp cdlib-cli:6.latest cdlib ...
                        """.trimMargin()
                }
                val paths = path.walk().filter { file ->
                    val relativePath = file.absolutePath
                        .removePrefix(path.absolutePath)
                        .removePrefix(File.separatorChar.toString())
                    matcher.matches(pattern, relativePath).also {
                        if (it) {
                            logger.info { "Matched file ${file.absolutePath}" }
                        }
                    }
                }.toList()
                check(paths.isNotEmpty()) {
                    "Could not find any files matching pattern: '$fileOrPattern.'"
                }
                paths
            } else {
                val file = File(fileOrPattern)
                require(file.exists()) {
                    "File ${file.absolutePath} does not exist."
                }
                if (logger.isDebugEnabled) {
                    logger.debug { "Found file: ${file.absolutePath}" }
                }
                listOf(file)
            }
        }
    }

    companion object : KLogging()
}
