package de.deutschepost.sdm.cdlib.names

import jakarta.inject.Singleton

@Singleton
class NameProviderAzure(override val resolver: NameResolverAzure) : NameProvider {
    override val platformType: PlatformType = PlatformType.AZURE_DEVOPS

    override fun provideName(name: Names, value: String): String {
        // Use Azure DevOps logging command syntax to create pipeline variables
        // cf. https://docs.microsoft.com/en-us/azure/devops/pipelines/scripts/logging-commands
        return "##vso[task.setvariable variable=$name;isOutput=true]${value.trim()}"
    }
}
