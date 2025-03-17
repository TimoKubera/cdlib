package de.deutschepost.sdm.cdlib.utils


import com.fasterxml.jackson.databind.JsonNode
import java.io.File

interface Jsonable {

    val canonicalFilename: String

    fun toPrettyString(): String? {
        return defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
    }

    fun toJsonNode(): JsonNode? {
        return defaultObjectMapper.valueToTree(this)
    }

    fun writeJson(outfile: String) {
        defaultObjectMapper.writeValue(File(outfile), this)
    }

}
