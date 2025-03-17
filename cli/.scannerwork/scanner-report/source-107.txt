package de.deutschepost.sdm.cdlib.release.report.external.fnci

import io.micronaut.http.HttpResponse
import io.micronaut.retry.annotation.Retryable
import jakarta.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import mu.KLogging

@Singleton
@Retryable(excludes = [CancellationException::class])
class FnciServiceRepository(private val client: FnciClient) {

    suspend fun getId(projectName: String, token: String): Map<String, Int> {
        currentCoroutineContext().ensureActive()
        return client.getId(projectName, addPrefix(token))
    }

    // ReviewStatus is an enum of:
    // [ UNUSED0, UNUSED1, UNUSED2, UNUSED3, DRAFT, READY_TO_REVIEW, APPROVED, REJECTED ]
    // ATTENTION: query param size paginates first, then status filter is applied! :-(
    suspend fun getProjectInventory(projectId: Int, token: String): List<FnciInventoryItem> {
        return listOf("DRAFT", "REJECTED", "APPROVED", "READY_TO_REVIEW").flatMap { reviewStatus ->
            currentCoroutineContext().ensureActive()
            getProjectInventoryForReviewStatus(projectId, token, reviewStatus).inventoryItems
        }
    }

    // When private, the annotation does not work
    @Retryable(attempts = "99")
    suspend fun getProjectInventoryForReviewStatus(
        projectId: Int,
        token: String,
        reviewStatus: String
    ): FnciProjectInventory {
        currentCoroutineContext().ensureActive()
        return client.getProjectInventory(projectId, addPrefix(token), reviewStatus)
    }

    suspend fun generateReport(projectId: Int, reportId: Int, token: String): Int = runCatching {
        currentCoroutineContext().ensureActive()
        client.generateReport(projectId, reportId, addPrefix(token)).data.taskId
    }.onFailure {
        logger.error { "Failed to trigger report generation. Is your token correct?" }
    }.getOrThrow()

    suspend fun downloadReport(projectId: Int, reportId: Int, taskId: Int, token: String): HttpResponse<ByteArray> {
        currentCoroutineContext().ensureActive()
        return client.downloadReport(projectId, reportId, taskId, addPrefix(token))
    }

    @Retryable(attempts = "99")
    suspend fun getProjectInformation(projectId: Int, token: String): FnciProjectInfo {
        currentCoroutineContext().ensureActive()
        return client.getProjectInformation(projectId, addPrefix(token)).data
    }

    private fun addPrefix(token: String): String = if (token.startsWith("Bearer")) token else "Bearer $token"

    companion object : KLogging()
}
