package de.deutschepost.sdm.cdlib.release.report.internal

enum class Severity {
    UNKNOWN, NONE, LOW, MEDIUM, HIGH, CRITICAL;

    override fun toString(): String {
        if (this == NONE) {
            return "NONE/INFO"
        }
        return super.toString()
    }

    companion object {
        private val mediumAliases = arrayOf("MEDIUM", "MODERATE")

        @Throws(IllegalArgumentException::class)
        fun resilientValueOf(value: String): Severity {
            return when (value.uppercase()) {
                "UNKNOWN" -> UNKNOWN
                "NONE" -> NONE
                "LOW" -> LOW
                in mediumAliases -> MEDIUM
                "HIGH" -> HIGH
                "CRITICAL" -> CRITICAL
                else ->
                    throw IllegalArgumentException(
                        "$value cannot be translated to enum Severity." +
                            " Please notify the CDLib team if it should be a legal value."
                    )
            }
        }
    }
}
