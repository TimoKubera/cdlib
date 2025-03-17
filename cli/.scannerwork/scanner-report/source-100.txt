package de.deutschepost.sdm.cdlib.names

enum class PlatformType {
    AZURE_DEVOPS,
    JENKINS,
    UNKNOWN
}

val currentPlatformType: PlatformType
    get() =
        when {
            isAzureDevOps() -> PlatformType.AZURE_DEVOPS
            isJenkins() -> PlatformType.JENKINS
            else -> PlatformType.UNKNOWN
        }

private fun isAzureDevOps(): Boolean {
    val systemVar = System.getenv("TF_BUILD")
    return systemVar != null && systemVar.equals("True")
}

private fun isJenkins(): Boolean {
    val systemVarHome = System.getenv("JENKINS_HOME")
    val systemVarUrl = System.getenv("JENKINS_URL")

    return (systemVarHome != null && systemVarHome.isNotBlank()) or (systemVarUrl != null && systemVarUrl.isNotBlank())
}
