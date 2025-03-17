package de.deutschepost.sdm.cdlib.change.changemanagement.api

import com.fasterxml.jackson.databind.DeserializationFeature
import de.deutschepost.sdm.cdlib.change.changemanagement.model.*
import de.deutschepost.sdm.cdlib.change.changemanagement.model.GetChangesResponse.Issue
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.JIRA_API_URL_PROD
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client
import io.micronaut.jackson.annotation.JacksonFeatures
import io.micronaut.retry.annotation.Retryable

@JacksonFeatures(disabledDeserializationFeatures = [DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES])
@Header(name = HttpHeaders.CONTENT_TYPE, value = "application/json;charset=utf-8")
@Client(JIRA_API_URL_PROD)
@Retryable
interface JiraApiClient {

    @Post("/jira1/rest/api/latest/issue/")
    fun createChange(
        fields: CreateChangeRequest,
        @Header("authorization") authorization: String
    ): HttpResponse<CreateChangeResponse>

    @Get("/jira1/rest/insight/1.0/iql/objects?schemaId=9&includeAttributesDeep=2&iql={commercialReference}")
    fun getItSystem(
        @QueryValue("commercialReference") commercialReference: String,
        @Header("authorization") authorization: String
    ): HttpResponse<ItSystemResponse>

    @Get("/jira1/rest/api/latest/issue/{changeId}")
    fun getChange(
        @QueryValue("changeId") changeId: String,
        @Header("authorization") authorization: String
    ): HttpResponse<Issue>

    @Post("/jira1/rest/api/2/search")
    fun getChanges(
        @Header("authorization") authorization: String,
        @Body getChangesRequest: GetChangesRequest,
    ): HttpResponse<GetChangesResponse>

    @Put("/jira1/rest/api/latest/issue/{changeId}")
    fun updateChangeRequest(
        @QueryValue("changeId") changeId: String,
        fields: UpdateChangeRequest,
        @Header("authorization") authorization: String
    ): HttpResponse<String>

    @Post("/jira1/rest/api/2/issue/{changeId}/transitions")
    fun updateChangePhase(
        @QueryValue("changeId") changeId: String,
        @Body updateChangePhaseRequest: UpdateChangePhaseRequest,
        @Header("authorization") authorization: String
    ): HttpResponse<String>

    @Post("/jira1/rest/api/2/issue/{changeId}/comment")
    fun addChangeComment(
        @QueryValue("changeId") changeId: String,
        @Body addChangeCommentRequest: AddChangeCommentRequest,
        @Header("authorization") auth: String
    ): HttpResponse<Any>

    @Get("/jira1/rest/api/2/issue/{changeId}/comment")
    fun getChangeComments(
        @QueryValue("changeId") changeId: String,
        @Header("authorization") authorization: String
    ): HttpResponse<GetChangeCommentsRequest>
}
