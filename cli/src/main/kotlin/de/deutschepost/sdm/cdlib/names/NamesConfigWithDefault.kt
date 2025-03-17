package de.deutschepost.sdm.cdlib.names

import jakarta.inject.Singleton

@Singleton
class NamesConfigWithDefault {
    var repoPath = System.getProperty("user.dir") ?: "."
    var datetimePattern = "yyyyMMdd.HHmm.ss"
    var datetimeZone = "Europe/Berlin"
    var originOverride: String? = null
}
