package de.deutschepost.sdm.cdlib.release.report.internal

import com.fasterxml.jackson.annotation.JsonIgnore

data class Vulnerability(
    val id: String,
    val severity: Severity,
    val origin: String,
    @JsonIgnore
    val description: String = "",
    @JsonIgnore
    val additionalInfo: Map<String, Any> = mapOf()
)

data class Vulnerabilities(
    val scannedObjects: Int,
    val vulnerableObjects: Int,
    val open: List<Vulnerability> = listOf(),
    val openCount: Int = open.size,
    val suppressed: List<Any> = listOf(),
    val suppressedCount: Int = suppressed.size,
    val severityCounts: SeverityCounts = SeverityCounts(open)
) {
    data class SeverityCounts(
        val critical: Int,
        val high: Int,
        val medium: Int,
        val low: Int,
        val none: Int,
        val unknown: Int,
    ) {
        constructor(vulnerabilities: List<Vulnerability>) : this(
            critical = vulnerabilities.count { it.severity == Severity.CRITICAL },
            high = vulnerabilities.count { it.severity == Severity.HIGH },
            medium = vulnerabilities.count { it.severity == Severity.MEDIUM },
            low = vulnerabilities.count { it.severity == Severity.LOW },
            none = vulnerabilities.count { it.severity == Severity.NONE },
            unknown = vulnerabilities.count { it.severity == Severity.UNKNOWN },
        )

        fun asMap(): Map<Severity, Int> {
            return mapOf(
                Severity.UNKNOWN to unknown,
                Severity.NONE to none,
                Severity.LOW to low,
                Severity.MEDIUM to medium,
                Severity.HIGH to high,
                Severity.CRITICAL to critical
            )
        }
    }
}
