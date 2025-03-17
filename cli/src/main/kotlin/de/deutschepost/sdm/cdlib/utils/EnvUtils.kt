package de.deutschepost.sdm.cdlib.utils

import de.deutschepost.sdm.cdlib.names.Names
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun resolveEnvByName(name: Names): String {
    val ret = System.getenv(name.toString())
    if (ret.isNullOrBlank()) {
        val errorStr = "Failed to retrieve environment variable $name."
        logger.error {
            "$errorStr Did you run cdlib names create?"
        }
        throw RuntimeException(errorStr)
    }
    return ret
}

fun resolveEnvByNameSanitized(name: Names): String {
    val env = resolveEnvByName(name)
    val ret = env.replace(Regex("\\W+"), "-")
    logger.debug {
        if (env != ret) {
            "Sanitized $env to $ret!"
        } else {
            "$env was not sanitized."
        }
    }
    return ret
}
