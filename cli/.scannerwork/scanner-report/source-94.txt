package de.deutschepost.sdm.cdlib.names

import jakarta.inject.Singleton

@Singleton
class NameResolverAzure(namesConfigWithDefault: NamesConfigWithDefault) : NameResolverBase(namesConfigWithDefault) {

    override fun createAppName(): String = getAzureVariable(mapAzureVariable("Build.DefinitionName"))

    override fun createEffectiveBranchName(): String {
        val azureBranchNameVar = getAzureVariable(mapAzureVariable("Build.SourceBranchName"))
        return if (azureBranchNameVar != "merge") {
            azureBranchNameVar
        } else {
            val azurePRVar = getAzureVariable(mapAzureVariable("System.PullRequest.SourceBranch"))

            val pattern = "refs/(heads|tags|pull)/(.*)".toRegex()
            val matches = pattern.find(azurePRVar)
            if (matches != null) {
                matches.groupValues[2]
            } else {
                azureBranchNameVar
            }
        }
    }

    override fun createJobUrl(): String {
        val collectionUri = getAzureVariable(mapAzureVariable("System.CollectionUri"))
        val project = getAzureVariable(mapAzureVariable("System.TeamProject"))
        val buildId = getAzureVariable(mapAzureVariable("Build.BuildId"))
        return "$collectionUri$project/_build/results?buildId=$buildId"
    }

    //
    // Internal implementation helpers
    //
    override fun createBuildNumber(): String = getAzureVariable(mapAzureVariable("Build.BuildID"))

    override fun createCicdPlatform(): String = "AzureDevOps"

    override fun createPipelineUrl(): String {
        val collectionUri = getAzureVariable(mapAzureVariable("System.CollectionUri"))
        val project = getAzureVariable(mapAzureVariable("System.TeamProject"))
        val pipelineId = getAzureVariable(mapAzureVariable("System.DefinitionId"))
        val sourceBranchName = get(Names.CDLIB_EFFECTIVE_BRANCH_NAME)
        return "$collectionUri$project/_build?definitionId=$pipelineId&branchFilter=${sourceBranchName}"
    }

    private fun getAzureVariable(variable: String): String =
        System.getenv(variable) ?: throw IllegalArgumentException("Unknown variable $variable")

    private fun mapAzureVariable(variable: String): String {
        val parts = variable.split('.')
        return parts.joinToString(separator = "_") { it.uppercase() }
    }
}
