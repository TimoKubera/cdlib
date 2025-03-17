package de.deutschepost.sdm.cdlib.change.changemanagement.api

import de.deutschepost.sdm.cdlib.change.changemanagement.model.*
import io.micronaut.http.HttpStatus
import jakarta.inject.Singleton
import mu.KLogging

@Singleton
class ChangeManagementRepository(
    private val client: JiraApiClient,
) {

    fun postChangeRequest(change: Change, auth: String): Change {
        val fields = CreateChangeRequest(change)
        val response = client.createChange(fields, auth)

        check(response.status == HttpStatus.CREATED) {
            "Returned wrong http status: ${response.status}\n Code: ${response.status.code}\n Body: ${response.body}"
        }

        logger.debug { "postChangeRequest status: ${response.status.code}" }
        logger.debug { "postChangeRequest body: ${response.body}" }

        return getChangeRequest(response.body.get().id, auth)
    }

    fun getItSystem(commercialReference: String, auth: String): ItSystem {
        val response = client.getItSystem("BTOID=\"$commercialReference\"", auth)

        check(response.status == HttpStatus.OK) {
            "Returned wrong http status: ${response.status}\n Code: ${response.status.code}\n Body: ${response.body}"
        }

        logger.debug { "getItSystem status: ${response.status.code}" }
        logger.debug { "getItSystem body: ${response.body}" }
        return response.body.get().mapItSystemInfo()
    }

    fun getChangeRequest(changeId: String?, auth: String): Change {
        requireNotNull(changeId)
        val response = client.getChange(changeId, auth)

        check(response.status == HttpStatus.OK) {
            "Returned wrong http status: ${response.status}\n Code: ${response.status.code}\n Body: ${response.body}"
        }

        logger.debug { "getChangeRequest status: ${response.status.code}" }
        logger.debug { "getChangeRequest body: ${response.body}" }
        val body = response.body.get()
        return Change(body)
    }

    fun getChangeRequestsIssues(auth: String, request: GetChangesRequest): List<Change> {
        val response = client.getChanges(auth, request)

        check(response.status == HttpStatus.OK) {
            "Returned wrong http status: ${response.status}\n Code: ${response.status.code}\n Body: ${response.body}"
        }

        val changesResponse = response.body.get()
        logger.debug { "getChangeRequestsIssues status: ${response.status.code}" }
        logger.debug { "getChangeRequestsIssues body: ${response.body}" }
        return changesResponse.issues
            .filter { it.fields.referenceTwo == request.referenceTwo }
            .map { Change(it) }
    }

    fun updateChange(change: Change, auth: String): Change {
        requireNotNull(change.id)

        val response = client.updateChangeRequest(change.id, UpdateChangeRequest(change), auth)

        logger.debug { "updateChange status: ${response.status.code}" }

        return getChangeRequest(change.id, auth)
    }

    fun transitionChangePhase(
        changeId: String?,
        phaseId: Int,
        auth: String,
        transitionFields: JiraTransitionFields? = null,
        comment: JiraTransitionUpdate? = null
    ) {
        requireNotNull(changeId)
        val response = client.updateChangePhase(
            changeId,
            UpdateChangePhaseRequest(
                transition = JiraTransition(phaseId),
                fields = transitionFields,
                update = comment
            ),
            auth
        )
        logger.debug { "transitionChangePhase status: ${response.status.code}" }
    }

    fun addComment(id: String?, comment: String, auth: String) {
        requireNotNull(id)
        val response = client.addChangeComment(id, AddChangeCommentRequest(comment), auth)

        check(response.status == HttpStatus.CREATED) {
            "Returned wrong http status: ${response.status}\n Code: ${response.status.code}\n Body: ${response.body}"
        }

        logger.debug { "addComment status: ${response.status.code}" }
    }

    fun getComments(id: String, auth: String): List<ChangeComment> {
        val response = client.getChangeComments(id, auth)

        check(response.status == HttpStatus.OK) {
            "Returned wrong http status: ${response.status}\n Code: ${response.status.code}\n Body: ${response.body}"
        }

        logger.debug { "getComment status: ${response.status.code}" }

        val changeComments = response.body.get().comments.map { comment ->
            val author = comment.author.name
            val commentBody = comment.body
            ChangeComment(author, commentBody)
        }

        return changeComments
    }

    companion object : KLogging()
}
