package de.deutschepost.sdm.cdlib.change.changemanagement.api

import de.deutschepost.sdm.cdlib.change.ChangeCommand.CreateCommand.ChangeDetails
import de.deutschepost.sdm.cdlib.change.changemanagement.model.*
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ChangeStatus
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.Criticality.FROZEN_ZONE
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldDefaults
import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.FieldDefaults.APPROVAL_CHECK_TIMEOUT_IN_MINUTES
import de.deutschepost.sdm.cdlib.change.metrics.client.CosmosDashboardClient
import de.deutschepost.sdm.cdlib.change.metrics.client.CosmosDashboardRepository
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.utils.klogSelf
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import mu.KLogging
import java.time.ZonedDateTime
import kotlin.properties.Delegates
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
class ChangeHandler(
    @Value("\${change-frozenzone-start}") val frozenZoneStart: ZonedDateTime,
    @Value("\${change-frozenzone-end}") val frozenZoneEnd: ZonedDateTime,
    private val changeManagementRepository: ChangeManagementRepository,
    private val cosmosDashboardRepository: CosmosDashboardRepository
) {

    private val changes: MutableList<Change> = mutableListOf()
    private var isTest: Boolean by Delegates.notNull()
    private var skipApprovalWait: Boolean by Delegates.notNull()
    private var performWebapproval: Boolean by Delegates.notNull()
    private var performOslc: Boolean by Delegates.notNull()
    private var isFrozenZone: Boolean by Delegates.notNull()

    private lateinit var auth: String
    private lateinit var itSystem: ItSystem
    private lateinit var change: Change
    private lateinit var getExternalReferenceTwo: () -> String

    fun readVersionInfo(): CosmosDashboardClient.VersionInfo = cosmosDashboardRepository.versionInfo

    fun initialise(
        authToken: String,
        isTestFlag: Boolean,
        skipApprovalWaitFlag: Boolean,
        enforceFrozenZoneFlag: Boolean,
        performWebapprovalFlag: Boolean,
        performOslcFlag: Boolean,
        gitopsFlag: Boolean
    ): ChangeHandler {
        auth = authToken
        isTest = isTestFlag
        skipApprovalWait = skipApprovalWaitFlag
        performWebapproval = performWebapprovalFlag
        performOslc = performOslcFlag
        isFrozenZone = when {
            enforceFrozenZoneFlag -> true
            !isTestFlag -> {
                val now = ZonedDateTime.now()
                now.isAfter(frozenZoneStart) && now.isBefore(frozenZoneEnd)
            }

            else -> false
        }
        getExternalReferenceTwo = {
            if (gitopsFlag) resolveEnvByName(Names.CDLIB_PM_GIT_ORIGIN) else resolveEnvByName(Names.CDLIB_PIPELINE_URL)
        }

        return this
    }

    fun findItSystem(commercialReference: String): ChangeHandler {
        require(::auth.isInitialized) { "Missing required authentication token." }

        itSystem = runCatching {
            changeManagementRepository.getItSystem(commercialReference, auth)
        }.onSuccess {
            logger.info {
                """
                    Retrieving IT system information:
                        CommercialReference: ${it.commercialReferenceNumber}
                        Name: ${it.name}
                        JiraKey: ${it.key}
                        ALM-ID: ${it.almId}
                        Criticality: ${it.criticality}
                """.trimIndent()
            }
        }.onFailure {
            logger.error { "Failed to get commercial reference: $commercialReference\n Check if your token is correct. \nError: ${it.message}" }
        }.getOrThrow()

        return this
    }

    fun findExisting(): ChangeHandler {
        require(::auth.isInitialized) { "Missing required authentication token." }
        require(::itSystem.isInitialized) { "Missing required IT system information." }

        val request = GetChangesRequest(getExternalReferenceTwo(), itSystem.key, isTest)

        changes.clear()
        changes += runCatching {
            changeManagementRepository.getChangeRequestsIssues(auth, request)
        }.onSuccess {
            logger.info { "Searching existing changes for the current pipeline: \n  '${request.jql}'" }
        }.onFailure {
            it.klogSelf(logger)
        }.getOrThrow()
        if (changes.isEmpty()) {
            logger.info { "Could not find changes to close nor resume for the current pipeline." }
        }

        return this
    }

    fun findResumable(): ChangeHandler {
        changes.sortedByDescending { it.created }

        val changeToResume = changes.first()
        if (ZonedDateTime.now() in changeToResume.start.rangeTo(changeToResume.end)) {
            change = changes.removeFirst()
        } else {
            logger.error {
                """Change window has expired (${changeToResume.start} --> ${changeToResume.end}) therefore cannot
                    |resume and closing this change, please create a new change by re-triggering this run.
                    |Aborting pipeline...""".trimMargin()
            }
            throw UnsupportedOperationException("Change window expired.")
        }
        return this
    }

    fun closeExisting(): ChangeHandler {
        require(::auth.isInitialized) { "Missing required authentication token." }

        changes.forEach { changeToClose ->
            logger.info { "Closing change: ${changeToClose.self}" }
            requireNotNull(changeToClose.id) {
                "Closing changes: A change id is null, aborting..."
            }

            logger.info { "Adding abort comment to change..." }

            changeManagementRepository.addComment(
                changeToClose.id,
                "Change wurde abgebrochen, weil ein neuer Pipeline-Lauf (${resolveEnvByName(Names.CDLIB_JOB_URL)}) einen neuen Change angelegt hat.",
                auth
            )

            when (changeToClose.status) {
                ChangeStatus.AWAITING_IMPLEMENTATION -> {
                    logger.debug { "Rescheduling..." }
                    changeManagementRepository.transitionChangePhase(
                        changeToClose.id,
                        JiraConstants.ChangePhaseId.IMPLEMENTATION_TO_OPEN.value,
                        auth
                    )

                    logger.debug { "Cancelling..." }
                    changeManagementRepository.transitionChangePhase(
                        changeToClose.id,
                        JiraConstants.ChangePhaseId.OPEN_TO_CANCEL.value,
                        auth
                    )

                    logger.debug { "Closing..." }
                    changeManagementRepository.transitionChangePhase(
                        changeToClose.id,
                        JiraConstants.ChangePhaseId.CANCEL_TO_CLOSE.value,
                        auth
                    )
                }

                ChangeStatus.WAITING_FOR_APPROVAL -> {
                    logger.debug { "Rejecting..." }
                    changeManagementRepository.transitionChangePhase(
                        changeToClose.id,
                        JiraConstants.ChangePhaseId.APPROVAL_TO_REJECT.value,
                        auth
                    )

                    logger.debug { "Closing..." }
                    changeManagementRepository.transitionChangePhase(
                        changeToClose.id,
                        JiraConstants.ChangePhaseId.REJECT_TO_CLOSE.value,
                        auth
                    )
                }

                else -> throw UnsupportedOperationException("Could not process change to close.")
            }
        }

        return this
    }

    fun post(changeDetails: ChangeDetails): ChangeHandler {
        require(::auth.isInitialized) { "Missing required authentication token." }
        require(::itSystem.isInitialized) { "Missing required IT system information." }

        val labels = buildCustomLabels(changeDetails.labels)
        val (start, end, response) = buildChangeWindow(changeDetails)
        change = Change(
            project = if (isTest) FieldDefaults.TEST_PROJECT else FieldDefaults.PROD_PROJECT,
            issueType = FieldDefaults.ISSUE_TYPE,
            summary = changeDetails.summary,
            description = changeDetails.description,
            labels = labels,
            category = changeDetails.category,
            start = start,
            end = end,
            response = response,
            target = changeDetails.target,
            impact = changeDetails.impact,
            impactClass = changeDetails.impactClass,
            implementationRisk = changeDetails.implementationRisk,
            omissionRisk = changeDetails.omissionRisk,
            fallback = changeDetails.fallback,
            referenceTwo = getExternalReferenceTwo(),
            commercialReference = itSystem.commercialReferenceNumber,
            status = ChangeStatus.OPEN,
            urgency = changeDetails.urgency,
        )

        change = runCatching {
            changeManagementRepository.postChangeRequest(change, auth)
        }.onSuccess {
            logger.info { "Posting change request: ${it.self}" }
            val versionInfo = readVersionInfo()
            if (!versionInfo.isLatest) {
                changeManagementRepository.addComment(it.id, "Es ist eine neuere CDLib version verfügbar.", auth)
            }
            if (!versionInfo.isSupported) {
                changeManagementRepository.addComment(
                    it.id,
                    "CDLib version ${versionInfo.version} ist nicht länger unterstützt. Bitte aktualisieren sie die CDLib in ihrer Pipeline. Pre-authorization ist nicht mehr möglich.",
                    auth
                )
            }
        }.onFailure {
            it.klogSelf(logger)
        }.getOrThrow()
        logger.debug { "Change request key: ${change.key} --" }

        return this
    }

    fun preauthorize(): ChangeHandler {
        companion object {
            const val MISSING_CHANGE_INFO_MESSAGE = "Missing required change information."
        }
        
        fun preauthorize(): ChangeHandler {
            require(::change.isInitialized) { MISSING_CHANGE_INFO_MESSAGE }
            logger.info { "Determining whether change can be preauthorized." }
            val changeType = determineChangeType()
            logTypeResults(changeType)
            change = change.updateType(changeType)
        
            runCatching {
                logger.info { "Updating change request type: ${change.type?.name}" }
                changeManagementRepository.updateChange(change, auth)
            }.getOrElse {
                logger.error { "Could not update change request type. \nError: ${it.message}" }
            }
            return this
        }
        
        fun resume(): ChangeHandler {
            require(::auth.isInitialized) { "Missing required authentication token." }
            require(::change.isInitialized) { MISSING_CHANGE_INFO_MESSAGE }
        
            logger.info { "Resuming change: ${change.self}" }
            logger.info { "Adding resume comment to change..." }
        
            changeManagementRepository.addComment(
                change.id,
                "Change wird mit anderer Pipeline (${resolveEnvByName(Names.CDLIB_JOB_URL)}) fortgesetzt.",
                auth
            )
        
            logger.info { "Adding new job url to change description..." }
            change = change.updateDescription(change.description + "\n" + resolveEnvByName(Names.CDLIB_JOB_URL))
        
            changeManagementRepository.updateChange(change, auth)
        
            return this
        }
        
        fun transition(phase: JiraConstants.ChangePhaseId): ChangeHandler {
            require(::auth.isInitialized) { "Missing required authentication token." }
            require(::change.isInitialized) { MISSING_CHANGE_INFO_MESSAGE }
        
            runCatching {
                logger.info { "Transitioning change request phase: ${phase.name}" }
                changeManagementRepository.transitionChangePhase(
                    changeId = change.id,
                    phaseId = phase.value,
                    auth = auth
                )
            }.onFailure {
                it.klogSelf(logger)
            }.getOrThrow()
        
            return this
        }
        
        fun monitor(approvalCheckInterval: Int): ChangeHandler {
            require(::auth.isInitialized) { "Missing required authentication token." }
            require(::change.isInitialized) { MISSING_CHANGE_INFO_MESSAGE }
        
            val numberOfApprovalChecks = (APPROVAL_CHECK_TIMEOUT_IN_MINUTES / approvalCheckInterval)
            logger.info { "Checking change request status for approval every ${approvalCheckInterval}m." }
        
            for (i in 1..numberOfApprovalChecks) {
                val changeRequest = runCatching {
                    changeManagementRepository.getChangeRequest(change.id, auth)
                }.onFailure {
                    it.klogSelf(logger)
                }.getOrThrow()
        
                logChangeRequestStatus(changeRequest)
        
                if (changeRequest.status == ChangeStatus.AWAITING_IMPLEMENTATION) {
                    return this
                }
                waitBeforeNextCheck(approvalCheckInterval)
            }
            throw Exception("Change request was not approved after $APPROVAL_CHECK_TIMEOUT_IN_MINUTES minutes.")
        }
        
        fun getItSystem(): ItSystem {
            require(::itSystem.isInitialized) {
                "Missing required IT system information."
            }
            return itSystem
        }
        
        fun getChange(): Change {
            require(::change.isInitialized) { MISSING_CHANGE_INFO_MESSAGE }
            return change
        }

    fun comment(comment: String): ChangeHandler {
        require(::change.isInitialized) { "Missing required change information." }
        if (comment.isNotEmpty()) {
            logger.info { "Adding custom comment to change." }
            changeManagementRepository.addComment(id = change.id, comment = comment, auth = auth)
        }
        return this
    }

    fun getComments(changeId: String): List<ChangeComment> {
        return changeManagementRepository.getComments(changeId, auth)
    }

    fun getUrl(): String {
        require(::change.isInitialized) { "Missing required change information." }
        val url = change.self
        requireNotNull(url)
        return url
    }

    private fun logChangeRequestStatus(changeRequest: Change) {
        requireNotNull(changeRequest.status) {
            "Change status was null."
        }
        logger.info { "Checked current change request status: ${changeRequest.status.name}" }
    }

    private fun waitBeforeNextCheck(approvalCheckInterval: Int) {
        if (!skipApprovalWait) {
            logger.info { "Next check in ${approvalCheckInterval}m." }
            Thread.sleep(approvalCheckInterval.toDuration(DurationUnit.MINUTES).inWholeMilliseconds)
        }
    }

    private fun buildCustomLabels(customLabels: String?): List<String> {
        val internalLabels = mapOf(
            JiraConstants.Labels.CHANGE_CDLIB to true,
            JiraConstants.Labels.CHANGE_TEST to isTest,
            JiraConstants.Labels.CHANGE_WEBAPPROVAL to performWebapproval,
            JiraConstants.Labels.CHANGE_OSLC to performOslc
        ).mapNotNull { (label, isRequested) -> label.takeIf { isRequested } }

        return buildList {
            addAll(internalLabels)
            if (!customLabels.isNullOrEmpty()) {
                addAll(customLabels.split(","))
            }
        }
    }

    private fun buildChangeWindow(changeDetails: ChangeDetails): Triple<ZonedDateTime, ZonedDateTime, ZonedDateTime> {
        val now = ZonedDateTime.now()
        val start = changeDetails.startOpt
        val end = changeDetails.endOpt

        return when {
            isFrozenZone -> {
                logger.warn { "Frozen zone active: Ignoring set custom change window." }

                Triple(
                    now,                       // start
                    now.plusDays(4),      // end
                    now.plusDays(2)       // response
                )
            }

            // TODO: Check if this can be done via picocli validaiton
            else -> {
                require(end.isAfter(start)) {
                    "End date needs to be later than the start date."
                }
                val yesterday = now.minusDays(1)
                require(!start.isBefore(yesterday)) {
                    "Start date cannot be more than 24 hours in the past."
                }

                Triple(
                    start,                      // start
                    end,                        // end
                    now.plusMinutes(30) // response
                )
            }
        }
    }

    private fun determineChangeType(): JiraConstants.ChangeType {
        val criticality = if (isFrozenZone) {
            FROZEN_ZONE
        } else {
            itSystem.criticality
        }

        val changeType = JiraConstants.ChangeType.determine(criticality, change.impactClass)
        if (changeType == JiraConstants.ChangeType.PREAUTHORIZED && !readVersionInfo().isSupported) {
            runCatching {
                changeManagementRepository.addComment(
                    change.id,
                    "Der Change konnte nicht preauthorized werden, da die genutzte Version der CDLib nicht mehr unterstützt wird.",
                    auth
                )
            }
            return JiraConstants.ChangeType.MINOR
        }

        return changeType
    }

    private fun logTypeResults(changeType: JiraConstants.ChangeType) {
        val log = buildString {
            appendLine("\n  CDLib version is supported: ${readVersionInfo().isSupported}")
            appendLine("  Impact Class: ${change.impactClass}")
            appendLine("  Business Criticality: ${itSystem.criticality}")
            if (isFrozenZone) {
                appendLine("  Special conditions:")
                appendLine("    Frozen Zone (i.e. Starkverkehr) is active from $frozenZoneStart to $frozenZoneEnd.")
            }
            appendLine("  Determined change type --> $changeType")
        }

        logger.info { "  $log" }
    }

    companion object : KLogging()
}
