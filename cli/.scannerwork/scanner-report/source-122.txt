package de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker

data class VersionSpecification(
    val major: Int,
    val minor: Int,
    val bugfix: Int
) : Comparable<VersionSpecification> {
    override fun compareTo(other: VersionSpecification): Int {
        if (this.major != other.major) {
            return this.major - other.major
        }
        if (this.minor != other.minor) {
            return this.minor - other.minor
        }
        return this.bugfix - other.bugfix
    }

    companion object {
        fun fromString(input: String, defaultValue: Int = 0): VersionSpecification {
            if (input.isEmpty()) return VersionSpecification(defaultValue, defaultValue, defaultValue)
            val hierarchicalVersionSequence = input.splitToSequence(".")
            val major = hierarchicalVersionSequence.firstOrNull()?.toInt() ?: defaultValue
            val minor = hierarchicalVersionSequence.drop(1).firstOrNull()?.toInt() ?: defaultValue
            val bugfix = hierarchicalVersionSequence.drop(2).firstOrNull()?.toInt() ?: defaultValue
            return VersionSpecification(major, minor, bugfix)
        }

        fun rangeFromString(input: String): ClosedRange<VersionSpecification> {
            val versionsSplit = input.split('-')
            val (minVersion, maxVersion) = when (versionsSplit.size) {
                2 -> Pair(fromString(versionsSplit[0]), fromString(versionsSplit[1], Int.MAX_VALUE))
                1 -> Pair(fromString(versionsSplit[0]), fromString(versionsSplit[0], Int.MAX_VALUE))
                else -> throw RuntimeException("Malformed version specification in oslc accepted list entry")
            }
            return minVersion..maxVersion
        }
    }
}
