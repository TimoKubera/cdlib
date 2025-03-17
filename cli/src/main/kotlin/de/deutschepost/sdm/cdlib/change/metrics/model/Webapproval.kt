package de.deutschepost.sdm.cdlib.change.metrics.model

import com.fasterxml.jackson.annotation.JsonIgnore
import de.deutschepost.sdm.cdlib.change.metrics.WEBAPPROVAL_PREFIX
import de.deutschepost.sdm.cdlib.names.Names
import de.deutschepost.sdm.cdlib.utils.Jsonable
import de.deutschepost.sdm.cdlib.utils.resolveEnvByNameSanitized


data class Webapproval(
    val url: String,
    val webapplication: Webapplication?,
) : Jsonable {

    data class Webapplication(
        val certification: Certification,
        val sharepointUrl: String,
    ) {
        data class Certification(
            val id: Int,
            val status: String,
        )
    }

    @get:JsonIgnore
    override val canonicalFilename: String = canonicalFilename()

    companion object {
        fun canonicalFilename(): String =
            "${WEBAPPROVAL_PREFIX}_${resolveEnvByNameSanitized(Names.CDLIB_RELEASE_NAME)}.json"
    }
}
