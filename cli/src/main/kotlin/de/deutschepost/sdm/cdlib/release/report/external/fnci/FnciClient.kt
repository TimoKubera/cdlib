package de.deutschepost.sdm.cdlib.release.report.external.fnci

import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client


@Client("https://fnci.deutschepost.de/codeinsight")
interface FnciClient {

    @Get("/api/project/id?projectName={name}")
    fun getId(name: String, @Header("Authorization") token: String): Map<String, Int>

    @Get("/api/project/inventory/{projectId}?size=20000")
    @Header(name = HttpHeaders.CONTENT_TYPE, value = "application/json;charset=utf-8")
    fun getProjectInventory(
        projectId: Int, @Header("Authorization") token: String,
        @QueryValue reviewStatus: String
    ): FnciProjectInventory

    @Post("/api/projects/{projectId}/reports/{reportId}/generate")
    fun generateReport(projectId: Int, reportId: Int, @Header("Authorization") token: String): TaskIdWrapper

    @Get("/api/projects/{projectId}/reports/{reportId}/download")
    fun downloadReport(
        projectId: Int,
        reportId: Int,
        @QueryValue taskId: Int,
        @Header("Authorization") token: String
    ): HttpResponse<ByteArray>

    @Get("/api/projects/{projectId}")
    fun getProjectInformation(projectId: Int, @Header("Authorization") token: String): FnciProjectInfoWrapper

}

data class FnciProjectInfoWrapper(
    val data: FnciProjectInfo
)

data class FnciProjectInfo(
    val id: Int,
    val name: String,
    val policyProfileName: String
)

data class FnciProjectInventory(
    val projectId: Int,
    val projectName: String,
    val inventoryItems: List<FnciInventoryItem>
)

data class FnciInventoryItem(
    val name: String, val inventoryReviewStatus: String, val selectedLicenseName: String,
    val type: String, val filePaths: List<String>?
)

data class TaskIdWrapper(
    val data: TaskIdData
) {
    data class TaskIdData(
        val taskId: Int
    )
}

data class FnciMessageWrapper(val data: List<FnciMessage>) {
    data class FnciMessage(val message: String)
}
