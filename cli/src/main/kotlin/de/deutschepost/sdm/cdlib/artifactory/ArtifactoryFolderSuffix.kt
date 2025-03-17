package de.deutschepost.sdm.cdlib.artifactory

enum class ArtifactoryFolderSuffix {
    BUILD,
    RELEASE,
    CHANGE,
    ;

    override fun toString(): String {
        return super.toString().lowercase()
    }
}
