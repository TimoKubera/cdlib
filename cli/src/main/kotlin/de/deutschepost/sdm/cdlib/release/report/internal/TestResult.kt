package de.deutschepost.sdm.cdlib.release.report.internal

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import de.deutschepost.sdm.cdlib.change.metrics.TEST_PREFIX
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcTestPreResult
import de.deutschepost.sdm.cdlib.utils.Jsonable
import de.deutschepost.sdm.cdlib.utils.defaultObjectMapper
import java.time.ZonedDateTime
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

@JsonDeserialize(using = TestResultDeserializer::class)
abstract class TestResult : Jsonable {
    abstract val uri: String
    abstract val date: ZonedDateTime

    @get:JsonAlias("testResultType")
    abstract val reportType: ReportType
    abstract val tool: Tool

    @get:JsonIgnore
    val info: String
        get() = "[${reportType} - ${uri.substringAfterLast('/').substringAfterLast('\\')}]"

    @get:JsonIgnore
    override val canonicalFilename: String
        get() = "${TEST_PREFIX}_${reportType}_${canonicalIdentifier}.json"


    @get:JsonIgnore
    open val canonicalIdentifier
        get() = uri.substringAfterLast("/").substringBeforeLast(".")
}

data class Tool(val name: String, val version: String, val vendor: String, val ruleVersion: String = "") {
    companion object {
        const val OSLC_MAVEN_PLUGIN_NAME: String = "OSLC Maven Plugin"
        const val OSLC_GRADLE_PLUGIN_NAME: String = "OSLC Gradle Plugin"
        const val OSLC_NPM_PLUGIN_NAME: String = "OSLC NPM Plugin"
        const val OSLC_FNCI_NAME: String = "CodeInsight"
    }
}

class TestResultDeserializer : StdDeserializer<TestResult>(TestResult::class.java) {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TestResult {
        val tree = p.readValueAsTree<ObjectNode>()
        val rtNode = tree.get(TestResult::reportType.name)
        val rtReader = defaultObjectMapper.readerFor(ReportType::class.java)
        val reportType =
            rtReader.readValue<ReportType>(rtNode)
                ?: throw RuntimeException("Cannot parse ${TestResult::reportType.name}")

        val constructor = when (reportType) {
            ReportType.SAST -> SecurityTestResult::class.primaryConstructor
            ReportType.CCA -> SecurityTestResult::class.primaryConstructor
            ReportType.SCA -> SecurityTestResult::class.primaryConstructor
            ReportType.DAST -> SecurityTestResult::class.primaryConstructor
            ReportType.OTHER -> SecurityTestResult::class.primaryConstructor
            ReportType.OSLC -> OslcTestResult::class.primaryConstructor
            ReportType.OSLC_PRE -> OslcTestPreResult::class.primaryConstructor
        } ?: throw RuntimeException("Found null for primary constructor of TestResult.")

        val args = mutableMapOf<KParameter, Any?>()

        for (param in constructor.parameters) {
            if (!tree.has(param.name)) {
                if (param.isOptional) {
                    continue
                }
                throw RuntimeException("Missing required field: ${param.name}")
            }

            val node = tree.get(param.name)
            if (node == null && !param.type.isMarkedNullable) {
                throw RuntimeException("Got null value for non-nullable field: ${param.name}")
            }

            val javaType = ctxt.typeFactory.constructType(param.type.javaType)
            val reader = defaultObjectMapper.readerFor(javaType)

            args[param] = reader.readValue<Any?>(node)
        }

        return constructor.callBy(args)
    }
}
