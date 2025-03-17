package de.deutschepost.sdm.cdlib.mixins.artifactory

import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryClient
import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryInstanceSection
import picocli.CommandLine.ArgGroup
import picocli.CommandLine.Option

class ArtifactoryMixinLight {
    @Option(
        names = ["--api-key", "--artifactory-api-key", "--artifactory-identity-token"],
        required = true,
        description = ["Artifactory Identity Token according to CDlib tutorial"]
    )
    lateinit var artifactoryIdentityToken: String

    @ArgGroup(
        validate = true,
        exclusive = true,
        heading = "Options for Artifactory Instances",
        multiplicity = "0..1",
        order = 8
    )
    var artifactoryInstanceSection = ArtifactoryInstanceSection()

    val client by lazy {
        ArtifactoryClient(
            apiKeyOrIdentityToken = artifactoryIdentityToken,
            artifactoryUrl = artifactoryInstanceSection.selectArtifactoryUrl()
        )
    }

    @Option(names = ["--repo-name"], required = true, description = ["Name of the repository inside Artifactory."])
    lateinit var repoName: String
}
