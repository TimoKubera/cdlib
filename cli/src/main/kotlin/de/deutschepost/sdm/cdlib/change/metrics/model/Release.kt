package de.deutschepost.sdm.cdlib.change.metrics.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import de.deutschepost.sdm.cdlib.change.metrics.RELEASE_PREFIX
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.utils.Jsonable
import de.deutschepost.sdm.cdlib.utils.resolveEnvByName
import de.deutschepost.sdm.cdlib.utils.resolveEnvByNameSanitized
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@JsonIgnoreProperties(ignoreUnknown = true)
data class Release(
    val test: Report?,
    val builds: List<Report>?,
    val reportFolderUrl: String,
    val pipelineToolData: PipelineToolData,
    val releaseName: String,
    val id: String,
    val createDate: String,
    val cdlibData: CdlibVersionViewModel,
    val lastCommit: Commit
) : Jsonable {
    constructor(
        test: Report? = null,
        builds: List<Report>? = null,
        reportFolderUrl: String = "",
        cdlibVersionViewModel: CdlibVersionViewModel
    ) : this(
        test = test,
        builds = builds,
        reportFolderUrl = reportFolderUrl,
        pipelineToolData = PipelineToolData(),
        releaseName = resolveEnvByName(Names.CDLIB_RELEASE_NAME),
        id = resolveEnvByName(Names.CDLIB_RELEASE_NAME_UNIQUE),
        createDate = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT),
        cdlibData = cdlibVersionViewModel,
        lastCommit = Commit()
    )

    @get:JsonIgnore
    override val canonicalFilename: String
        get() = canonicalFilename()

    companion object {
        fun canonicalFilename(): String =
            "${RELEASE_PREFIX}_${resolveEnvByNameSanitized(Names.CDLIB_RELEASE_NAME)}.json"
    }
}
