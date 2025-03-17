package de.deutschepost.sdm.cdlib.change.changemanagement.model

import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants.ImpactClass.*
import java.security.InvalidParameterException

object JiraConstants {
    const val DATETIME_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss.SSZ"
    const val JIRA_API_URL_PROD = "https://jira1.lcm.deutschepost.de"

    object FieldDefaults {
        const val PROD_PROJECT = "SMCHM"
        const val TEST_PROJECT = "SMCHMONB"
        const val ISSUE_TYPE = "Change"
        const val FALLBACK = "Rollback auf vorherige Version oder Fix-Forward je nach Fehlerbild."
        const val OMISSION_RISK = "Neue Funktionalität/Konfiguration/Fehlerbehebung kann nicht genutzt werden."
        const val IMPLEMENTATION_RISK = "Beeinträchtigung Produktion"
        const val IMPACT = "Keine Auswirkung durch Nutzung CI/CD-Pipeline."
        const val TARGET = "Bereitstellung neuer Funktionalität/Konfiguration/Fehlerbehebung."
        const val APPROVAL_CHECK_INTERVAL_IN_MINUTES = 5
        const val APPROVAL_CHECK_TIMEOUT_IN_MINUTES = 30
    }

    object Labels {
        const val CHANGE_TEST = "test"
        const val CHANGE_CDLIB = "cdlib"
        const val CHANGE_WEBAPPROVAL = "webapproval"
        const val CHANGE_TQS = "tqs"
        const val CHANGE_OSLC = "oslc"
    }

    object FieldNames {
        const val LABELS = "labels"
        const val STATUS = "status"
        const val CREATED = "created"
        const val DESCRIPTION = "description"
        const val PROJECT = "project"
        const val SUMMARY = "summary"
        const val ISSUE_TYPE = "issuetype"
        const val IT_SYSTEM = "customfield_13009"
        const val COMMERCIAL_REFERENCE = "customfield_13797"
        const val IMPACT_CLASS = "customfield_13798"
        const val CATEGORY = "customfield_13799"
        const val PLANNED_START_DATE = "customfield_13800"
        const val PLANNED_END_DATE = "customfield_13801"
        const val RESPONSE_DATE = "customfield_13802"
        const val REFERENCE_TWO = "customfield_13805"
        const val TARGET = "customfield_13807"
        const val IMPACT = "customfield_13808"
        const val COMPLETION_CODE = "customfield_13792"
        const val IMPLEMENTATION_START = "customfield_13795"
        const val IMPLEMENTATION_END = "customfield_13796"
        const val IMPLEMENTATION_RISK = "customfield_13811"
        const val DEVIATION = "customfield_13793"
        const val DEVIATION_CAUSE = "customfield_13794"
        const val OMISSION_RISK = "customfield_13812"
        const val FALLBACK = "customfield_13813"
        const val TYPE = "customfield_13828"
        const val APPROVAL_STATUS = "customfield_13825"
        const val URGENCY = "customfield_13803"
        const val EXECUTION_GROUP = "customfield_13830"
        const val AFFECTED_IT_SYSTEMS = "customfield_13810"
    }

    @Suppress("unused")
    enum class Category(val value: String) {
        ROLLOUT("Rollout"),
        NO_ROLLOUT("Kein Rollout"),
        AUTHORIZATIONS("Berechtigungen"),
        DATA_MAINTENANCE("Datenpflege"),
        TECHNICAL_REQUIREMENTS("Fachliche Anforderungen"),
        LEGAL_OR_CONTRACTUAL_REQUIREMENTS("Gesetzliche oder vertragliche Anforderungen"),
        HOUSEKEEPING("Housekeeping"),
        CAPACITY_ADJUSTMENTS("Kapazitätsanpassungen"),
        SECURITY("Sicherheit"),
        TROUBLESHOOTING("Störungsbehebung"),
        OTHER("Sonstiges")
        ;

        companion object {
            infix fun from(value: String): Category = entries.firstOrNull { it.value == value }
                ?: throw InvalidParameterException("Could not map json value for Category: $value")
        }
    }

    enum class ChangePhaseId(val value: Int) {
        IMPLEMENTATION_TO_OPEN(521), // reschedule
        OPEN_TO_CANCEL(191), // cancel
        CANCEL_TO_CLOSE(571), // close cancelled
        APPROVAL_TO_REJECT(331), // reject
        REJECT_TO_CLOSE(551), // close rejected
        OPEN_TO_IMPLEMENTATION(311), // start new
        IMPLEMENTATION_TO_IN_PROGRESS(71), // progress new
        REVIEW(381),
        ;
    }

    enum class ImpactClass(val value: String) {
        CRITICAL("IT-10"),
        HIGH("IT-11"),
        LOW("IT-12"),
        MEDIUM("IT-13"),
        NONE("IT-14"),
        ;

        companion object {
            infix fun from(value: String): ImpactClass = entries.firstOrNull { it.value == value }
                ?: throw InvalidParameterException("Could not map json value for ImpactClass: $value")
        }
    }

    enum class Criticality(val value: String) {
        CORPORATION_CRITICAL("Konzernkritisch"),
        BUSINESS_CRITICAL("Geschäftskritisch"),
        BUSINESS_DECISIVE("Geschäftsentscheidend"),
        OPERATIONAL("Betrieblich"),
        NON_CRITICAL("Nicht kritisch/Archiv"),
        FROZEN_ZONE("Frozen zone")
        ;

        companion object {
            infix fun from(value: String): Criticality = entries.firstOrNull { it.value == value }
                ?: throw InvalidParameterException("Could not map json value for Criticality")
        }
    }

    enum class ChangeType(val value: String) {
        SIGNIFICANT("IT-9"), // double check this
        PREAUTHORIZED("IT-8"),
        MINOR("IT-7"),
        MAJOR("IT-6"),
        ;

        companion object {
            infix fun from(value: String): ChangeType? = entries.firstOrNull { it.value == value }
            fun determine(criticality: Criticality, impactClass: ImpactClass): ChangeType = when (criticality) {
                Criticality.FROZEN_ZONE -> MAJOR
                Criticality.CORPORATION_CRITICAL -> when (impactClass) {
                    NONE -> MINOR
                    LOW, MEDIUM -> SIGNIFICANT
                    HIGH, CRITICAL -> MAJOR
                }

                Criticality.BUSINESS_CRITICAL -> when (impactClass) {
                    NONE, LOW -> MINOR
                    MEDIUM, HIGH -> SIGNIFICANT
                    CRITICAL -> MAJOR
                }

                Criticality.BUSINESS_DECISIVE -> when (impactClass) {
                    NONE -> PREAUTHORIZED
                    LOW, MEDIUM -> MINOR
                    HIGH, CRITICAL -> SIGNIFICANT
                }

                Criticality.OPERATIONAL -> when (impactClass) {
                    NONE, LOW -> PREAUTHORIZED
                    MEDIUM, HIGH -> MINOR
                    CRITICAL -> SIGNIFICANT
                }

                Criticality.NON_CRITICAL -> when (impactClass) {
                    NONE, LOW, MEDIUM -> PREAUTHORIZED
                    HIGH, CRITICAL -> MINOR
                }
            }
        }
    }

    @Suppress("unused")
    enum class ChangeStatus(val value: String) {
        WAITING_FOR_APPROVAL("10905"),
        AWAITING_IMPLEMENTATION("10907"),
        OPEN("1"),
        CLOSED("6"),
        UNDER_REVIEW("10400")
        ;

        companion object {
            infix fun from(value: String): ChangeStatus = entries.firstOrNull { it.value == value }
                ?: throw InvalidParameterException("Could not map json value for ChangeStatus: $value")
        }
    }

    @Suppress("unused")
    enum class ApprovalStatus(val value: String) {
        OPEN("offen"),
        REQUESTED("Freigabe angefragt"),
        PREAUTHORIZED("preauthorized"),
        APPROVED("freigegeben"),
        APPROVED_EXCEPTION("Sonderfreigabe"),
        REJECTED("abgelehnt")
        ;

        companion object {
            infix fun from(value: String): ApprovalStatus = entries.firstOrNull { it.value == value }
                ?: throw InvalidParameterException("Could not map json value for ApprovalStatus: $value")
        }
    }

    @Suppress("unused")
    enum class Urgency(val value: String) {
        NONE("-1"),
        LOW("13686"),
        MEDIUM("13687"),
        HIGH("13688"),
        URGENT("13689"),
        EMERGENCY("13690"),
        ;

        companion object {
            infix fun from(value: String): Urgency = entries.firstOrNull { it.value == value }
                ?: throw InvalidParameterException("Could not map json value for Urgency: $value")
        }
    }

    @Suppress("unused")
    enum class CompletionCode(val value: String) { // TODO: enums should probably work by id
        SUCCESS("Erfolgreich implementiert"), // 13672
        SUCCESS_DEVIATION("Erfolgreich mit Abweichung"), // 13673
        REJECTED("Abgelehnt"), // 13674
        SUCCESS_FALLBACK("Erfolgreich mit Fallback"), // 13675
        FALLBACK_DEVIATION("Fallback mit Abweichung"), // 13676
        ABORTED("Abgebrochen"), // 13671
        NONE("None"), // -1
        ;

        companion object {
            infix fun from(issueField: GetChangesResponse.Issue.Fields.CompletionCodeField?): CompletionCode? =
                if (issueField != null) {
                    entries.firstOrNull { it.value == issueField.value }
                } else {
                    null
                }
        }
    }
}
