package de.deutschepost.sdm.cdlib.names

import jakarta.inject.Singleton

@Singleton
class NameResolverJenkins(namesConfigWithDefault: NamesConfigWithDefault) : NameResolverBase(namesConfigWithDefault) {

    override fun createAppName(): String {
        val job = getJenkinsVariable("JOB_NAME")
        return if (job.indexOf('/') > -1) {
            val branch = getOrNullJenkinsVariable("BRANCH_NAME")
            val parts = job.split('/')
            if (branch != null) {
                parts[parts.size - 2]
            } else {
                parts[parts.size - 1]
            }
        } else {
            job
        }
    }


    override fun createEffectiveBranchName(): String = (getOrNullJenkinsVariable("CHANGE_BRANCH") ?: getBranchTag())

    override fun createJobUrl(): String = getJenkinsVariable("RUN_DISPLAY_URL")

    //
// Internal implementation helpers
//
    override fun createBuildNumber(): String = getJenkinsVariable("BUILD_NUMBER")

    override fun createCicdPlatform(): String = "Jenkins"
    override fun createPipelineUrl(): String = getJenkinsVariable("JOB_DISPLAY_URL")

    private fun getBranchTag(): String {
        val defaultBranch = getOrNullJenkinsVariable("BRANCH_NAME")
        val gitBranch = getJenkinsVariable("GIT_BRANCH")

        return if (defaultBranch == null || !(gitBranch.contains(defaultBranch))) {
            gitBranch.replace("origin/", "")
        } else {
            defaultBranch
        }
    }

    private fun getJenkinsVariable(variable: String): String =
        System.getenv(variable) ?: throw IllegalArgumentException("Unknown variable $variable")

    private fun getOrNullJenkinsVariable(variable: String): String? = System.getenv(variable)
}
