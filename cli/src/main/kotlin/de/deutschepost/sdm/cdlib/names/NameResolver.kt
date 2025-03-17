package de.deutschepost.sdm.cdlib.names

import mu.KotlinLogging

interface NameResolver : Map<Names, String> {

    override fun get(key: Names): String

    fun override(name: Names, value: String)
    fun seed(releaseName: String)

    companion object {
        val logger = KotlinLogging.logger {}
    }
}
