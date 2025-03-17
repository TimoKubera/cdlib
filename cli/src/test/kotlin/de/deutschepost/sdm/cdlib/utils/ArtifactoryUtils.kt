package de.deutschepost.sdm.cdlib.utils

import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryClient
import de.deutschepost.sdm.cdlib.artifactory.ArtifactoryFolderSuffix
import java.io.File
import java.time.ZonedDateTime


data class ReportFolder(
    var fortifyName: String = "",
    var owaspName: String = "",
    var genericReportName: String = "",
    var zapName: String = "",
    var ccaName: String = "",
    var oslcFNCIName: String = "",
    var oslcMavenPluginName: String = "",
    var oslcGradlePluginName: String = "",
)

fun getFileName(filePath: String): String {
    return filePath.substringAfterLast(File.separator)
}


fun uploadReportsToArtifactory(
    artifactoryClient: ArtifactoryClient,
    repoName: String,
    fortifyName: String = "",
    owaspName: String = "",
    genericReportName: String = "",
    zapName: String = "",
    ccaName: String = "",
    oslcFNCIName: String = "",
    oslcMavenPluginName: String = "",
    oslcGradlePluginName: String = "",
    datetime: ZonedDateTime = ZonedDateTime.now()
): ReportFolder {
    val externalPref = "src/test/resources/passing"
    val reportPref = "src/test/resources/testresults"
    val fortifyReport = changeFortifyReportDate("$externalPref/fortify.fpr", datetime)
    val odcReport = changeOdcReportDate("$externalPref/dependency-check-report.json", datetime)
    val zapReport = changeZapReportDate("src/test/resources/zap/zap-report_all_suppressed.json", datetime)
    val ccaReport = changeSecurityTestReportDate("src/test/resources/trivy/cca-trivy.json", datetime)
    val genericReport = changeSecurityTestReportDate("src/test/resources/generic.json", datetime)

    val oslcFNCI = File("$externalPref/oslc-fnci-report-cli_5065.json")
    val oslcMaven = File("$externalPref/oslc-maven-plugin-report.json")
    val oslcGradle = File("$externalPref/oslc-gradle-plugin-report.json")

    val files = mapOf(
        "fortify" to listOf(
            fortifyReport,
            generateFreshCDlibReport("cdlib_report_SAST_fortify.json", fortifyReport)
        ),
        "owasp" to listOf(
            odcReport,
            generateFreshCDlibReport("cdlib_report_SCA_dependency-check-report.json", odcReport)
        ),
        "zap" to listOf(
            zapReport,
            generateFreshCDlibReport("cdlib_report_DAST_zap-report_all_suppressed.json", zapReport)
        ),
        "cca" to listOf(
            ccaReport,
            generateFreshCDlibReport("cdlib_report_CCA_cca-trivy.json", ccaReport)
        ),
        "generic" to listOf(
            genericReport,
            generateFreshCDlibReport("cdlib_report_OTHER_generic.json", genericReport, listOf(genericReport.name))
        ),
        "oslc-fnci" to listOf(
            oslcFNCI,
            File("$externalPref/oslc-fnci-report-cli_5065.zip"),
            generateFreshCDlibReport("cdlib_report_OSLC_19C2_ICTO-3339_SDM_SockShop.json", oslcFNCI)
        ),
        "oslc-maven-plugin" to listOf(
            generateFreshCDlibReport("cdlib_report_OSLC_carts.json", oslcMaven),
            oslcMaven,
        ),
        "oslc-gradle-plugin" to listOf(
            generateFreshCDlibReport("cdlib_report_OSLC_shipping.json", oslcGradle),
            oslcGradle,
        ),
    )
    val reportFolder = ReportFolder()
    if (fortifyName.isNotEmpty()) {
        reportFolder.fortifyName = artifactoryClient.createFolder(repoName, fortifyName, ArtifactoryFolderSuffix.BUILD)
            ?: throw Exception("Failed to create $fortifyName")
        artifactoryClient.uploadFiles(repoName, reportFolder.fortifyName, false, files.getValue("fortify"))
    }
    if (owaspName.isNotEmpty()) {
        reportFolder.owaspName = artifactoryClient.createFolder(repoName, owaspName, ArtifactoryFolderSuffix.BUILD)
            ?: throw Exception("Failed to create $owaspName")
        artifactoryClient.uploadFiles(repoName, reportFolder.owaspName, false, files.getValue("owasp"))
    }
    if (genericReportName.isNotEmpty()) {
        reportFolder.genericReportName =
            artifactoryClient.createFolder(repoName, genericReportName, ArtifactoryFolderSuffix.BUILD)
                ?: throw Exception("Failed to create $genericReportName")
        artifactoryClient.uploadFiles(repoName, reportFolder.genericReportName, false, files.getValue("generic"))
    }
    if (zapName.isNotEmpty()) {
        reportFolder.zapName = artifactoryClient.createFolder(repoName, zapName, ArtifactoryFolderSuffix.RELEASE)
            ?: throw Exception("Failed to create $zapName")
        artifactoryClient.uploadFiles(repoName, reportFolder.zapName, false, files.getValue("zap"))
    }
    if (ccaName.isNotEmpty()) {
        reportFolder.ccaName = artifactoryClient.createFolder(repoName, ccaName, ArtifactoryFolderSuffix.BUILD)
            ?: throw Exception("Failed to create $ccaName")
        artifactoryClient.uploadFiles(repoName, reportFolder.ccaName, false, files.getValue("cca"))
    }
    if (oslcFNCIName.isNotEmpty()) {
        reportFolder.oslcFNCIName =
            artifactoryClient.createFolder(repoName, oslcFNCIName, ArtifactoryFolderSuffix.BUILD)
                ?: throw Exception("Failed to create $oslcFNCIName")
        artifactoryClient.uploadFiles(repoName, reportFolder.oslcFNCIName, false, files.getValue("oslc-fnci"))
    }
    if (oslcMavenPluginName.isNotEmpty()) {
        reportFolder.oslcMavenPluginName =
            artifactoryClient.createFolder(repoName, oslcMavenPluginName, ArtifactoryFolderSuffix.BUILD)
                ?: throw Exception("Failed to create $oslcMavenPluginName")
        artifactoryClient.uploadFiles(
            repoName,
            reportFolder.oslcMavenPluginName,
            false,
            files.getValue("oslc-maven-plugin")
        )
    }
    if (oslcGradlePluginName.isNotEmpty()) {
        reportFolder.oslcGradlePluginName =
            artifactoryClient.createFolder(repoName, oslcGradlePluginName, ArtifactoryFolderSuffix.BUILD)
                ?: throw Exception("Failed to create $oslcGradlePluginName")
        artifactoryClient.uploadFiles(
            repoName,
            reportFolder.oslcGradlePluginName,
            false,
            files.getValue("oslc-gradle-plugin")
        )
    }
    return reportFolder
}
