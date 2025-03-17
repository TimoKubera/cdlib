package de.deutschepost.sdm.cdlib.release.report.external.fortify

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty

data class FortifyVulnerability(
    @JacksonXmlProperty(localName = "ClassInfo")
    val classInfo: ClassInfo,
    @JacksonXmlProperty(localName = "InstanceInfo")
    val instanceInfo: InstanceInfo,
    @JacksonXmlProperty(localName = "AnalysisInfo")
    val analysisInfo: AnalysisInfo
) {
    val confidence: Double
        get() = this.instanceInfo.confidence

    val location: String
        get() = this.analysisInfo.unified.trace.primary.entry.node?.sourceLocation?.path ?: "Unknown"

    val snippet: String
        get() = this.analysisInfo.unified.trace.primary.entry.node?.sourceLocation?.snippet ?: "Unknown"

    data class ClassInfo(
        @JacksonXmlProperty(localName = "Kingdom")
        val kingdom: String,
        @JacksonXmlProperty(localName = "ClassID")
        val classID: String
    )

    data class InstanceInfo(
        @JacksonXmlProperty(localName = "InstanceID")
        val instanceID: String,
        @JacksonXmlProperty(localName = "InstanceSeverity")
        val severity: String,
        @JacksonXmlProperty(localName = "Confidence")
        val confidence: Double
    )

    //Use Jackson, they said. It will be fun, they said.
    data class AnalysisInfo(
        @JacksonXmlProperty(localName = "Unified")
        val unified: Unified
    ) {
        data class Unified(
            @JacksonXmlProperty(localName = "Trace")
            val trace: Trace
        ) {
            data class Trace(
                @JacksonXmlProperty(localName = "Primary")
                val primary: Primary
            ) {
                data class Primary(
                    @JacksonXmlProperty(localName = "Entry")
                    val entry: Entry
                ) {
                    data class Entry(
                        @JacksonXmlProperty(localName = "Node")
                        val node: Node? = null
                    ) {
                        data class Node(
                            @JacksonXmlProperty(localName = "SourceLocation")
                            val sourceLocation: SourceLocation? = null
                        ) {
                            data class SourceLocation(
                                @JacksonXmlProperty(localName = "path", isAttribute = true)
                                val path: String? = null,
                                @JacksonXmlProperty(localName = "snippet", isAttribute = true)
                                val snippet: String? = null
                            )
                        }
                    }
                }
            }
        }
    }
}
