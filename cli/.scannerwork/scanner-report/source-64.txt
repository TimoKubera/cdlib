package de.deutschepost.sdm.cdlib.change.metrics.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import de.deutschepost.sdm.cdlib.change.metrics.REPORT_PREFIX
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.release.report.internal.TestResult
import de.deutschepost.sdm.cdlib.utils.Jsonable
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName
import java.time.ZonedDateTime


data class Report(
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    val date: ZonedDateTime = ZonedDateTime.now(),
    val name: String = resolveEnvByName(Names.CDLIB_APP_NAME),
    val test: TestResult,
    val testHash: String,
    val lastCommit: Commit = Commit(),
    val pipeline: PipelineToolData = PipelineToolData()
) : Jsonable {

    @JsonIgnore
    override val canonicalFilename =
        "${REPORT_PREFIX}_${test.reportType}_${test.canonicalIdentifier}.json"
}
