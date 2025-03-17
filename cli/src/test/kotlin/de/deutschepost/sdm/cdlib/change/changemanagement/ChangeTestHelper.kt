package de.deutschepost.sdm.cdlib.change.changemanagement

import de.deutschepost.sdm.cdlib.change.ChangeCommand
import de.deutschepost.sdm.cdlib.change.changemanagement.model.Change
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.Category
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldDefaults
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ImpactClass
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.Labels
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName
import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import mu.KLogging
import toArgsArray
import java.time.ZonedDateTime

@Singleton
class ChangeTestHelper(
    @Value("\${change-management-token}") val token: String
) {
    private val now = ZonedDateTime.now()
    fun changeDetailsWithDefaults() = ChangeCommand.CreateCommand.ChangeDetails().also {
        it.startOpt = now
        it.endOpt = now.plusHours(4)
        it.summary = resolveEnvByName(Names.CDLIB_RELEASE_NAME_UNIQUE)
        it.description = resolveEnvByName(Names.CDLIB_JOB_URL)
        it.category = Category.ROLLOUT
        it.impact = FieldDefaults.IMPACT
        it.impactClass = ImpactClass.NONE
        it.target = FieldDefaults.TARGET
        it.fallback = FieldDefaults.FALLBACK
        it.implementationRisk = FieldDefaults.IMPLEMENTATION_RISK
        it.omissionRisk = FieldDefaults.OMISSION_RISK
        it.labels = "${Labels.CHANGE_CDLIB},${Labels.CHANGE_TEST}"
        it.urgency = JiraConstants.Urgency.LOW
    }

    fun changeWithFullDefaults() = Change(
        id = "1234",
        key = "key",
        self = "change url",
        type = JiraConstants.ChangeType.PREAUTHORIZED,
        project = FieldDefaults.TEST_PROJECT,
        issueType = FieldDefaults.ISSUE_TYPE,
        summary = resolveEnvByName(Names.CDLIB_RELEASE_NAME_UNIQUE),
        description = resolveEnvByName(Names.CDLIB_JOB_URL),
        labels = listOf("cdlib", "test"),
        category = Category.ROLLOUT,
        start = now,
        end = now.plusHours(4),
        response = now.plusMinutes(30),
        target = FieldDefaults.TARGET,
        impact = FieldDefaults.IMPACT,
        impactClass = ImpactClass.NONE,
        implementationRisk = FieldDefaults.IMPLEMENTATION_RISK,
        omissionRisk = FieldDefaults.OMISSION_RISK,
        fallback = FieldDefaults.FALLBACK,
        referenceTwo = resolveEnvByName(Names.CDLIB_PIPELINE_URL),
        commercialReference = "DI-22839",
        status = JiraConstants.ChangeStatus.OPEN,
        approvalStatus = JiraConstants.ApprovalStatus.PREAUTHORIZED,
        urgency = JiraConstants.Urgency.LOW,
        completionCode = JiraConstants.CompletionCode.SUCCESS,
        executionGroup = "Terrible-Systems",
        affectedItSystems = listOf("die Rente!!")
    )

    fun changeWithDefaults() = Change(
        project = FieldDefaults.TEST_PROJECT,
        issueType = FieldDefaults.ISSUE_TYPE,
        summary = resolveEnvByName(Names.CDLIB_RELEASE_NAME_UNIQUE),
        description = resolveEnvByName(Names.CDLIB_JOB_URL),
        labels = listOf("cdlib", "test"),
        category = Category.ROLLOUT,
        start = now,
        end = now.plusHours(4),
        response = now.plusMinutes(30),
        target = FieldDefaults.TARGET,
        impact = FieldDefaults.IMPACT,
        impactClass = ImpactClass.NONE,
        implementationRisk = FieldDefaults.IMPLEMENTATION_RISK,
        omissionRisk = FieldDefaults.OMISSION_RISK,
        fallback = FieldDefaults.FALLBACK,
        referenceTwo = resolveEnvByName(Names.CDLIB_PIPELINE_URL),
        commercialReference = "DI-22839",
        status = JiraConstants.ChangeStatus.OPEN,
        approvalStatus = JiraConstants.ApprovalStatus.PREAUTHORIZED,
        urgency = JiraConstants.Urgency.LOW,
        completionCode = JiraConstants.CompletionCode.SUCCESS,
        executionGroup = "Terrible-Systems",
        affectedItSystems = listOf("die Rente!!")
    )

    fun closeChangeRequest(token: String, commercialReference: String) {
        val arguments: Array<String> =
            "--jira-token $token --commercial-reference $commercialReference --status SUCCESS --test".toArgsArray()
        PicocliRunner.call(ChangeCommand.CloseCommand::class.java, *arguments)
    }

    companion object : KLogging()
}
