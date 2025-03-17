package de.deutschepost.sdm.cdlib.utils


import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import de.deutschepost.sdm.cdlib.change.metrics.model.Report
import de.deutschepost.sdm.cdlib.release.report.TestResultParser
import de.deutschepost.sdm.cdlib.release.report.TestResultPrefixes
import de.deutschepost.sdm.cdlib.release.report.external.FortifyTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.OslcTestResult
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.OslcTestPreResult
import de.deutschepost.sdm.cdlib.release.report.internal.oslcComplianceChecker.from
import java.io.File
import java.io.FileOutputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


fun changeFortifyReportDate(filepath: String, datetime: ZonedDateTime): File {
    val tempFile = File.createTempFile(getFileNameWithoutExtension(filepath), ".fpr")
    val buffer = ByteArray(1024)

    ZipInputStream(File(filepath).inputStream()).use { zis ->
        ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val newEntry = ZipEntry(entry.name)
                zos.putNextEntry(newEntry)
                if (entry.name == "audit.fvdl") {
                    val xmlContent = zis.readBytes().toString(Charsets.UTF_8)
                    val oldTest = defaultXmlMapper.readValue(xmlContent, FortifyTestResult::class.java)
                    val newTest = oldTest.copy(
                        createdTimestamp = FortifyTestResult.Timestamp(
                            date = datetime.toLocalDate(),
                            time = datetime.toLocalTime()
                        )
                    )
                    val modifiedXmlContent = defaultXmlMapper.writeValueAsString(newTest)
                    zos.write(modifiedXmlContent.toByteArray(Charsets.UTF_8))
                } else {
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        zos.write(buffer, 0, len)
                    }
                }
                zos.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
    return tempFile
}

fun changeZapReportDate(filepath: String, datetime: ZonedDateTime): File {
    val tempFile = File.createTempFile(getFileNameWithoutExtension(filepath), ".json")
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss", Locale.ENGLISH)
    val file = File(filepath)

    val rootNode: JsonNode = defaultObjectMapper.readTree(file)
    if (rootNode is ObjectNode) {
        rootNode.put("@generated", datetime.format(formatter))
    }
    defaultObjectMapper.writeValue(tempFile, rootNode)

    return tempFile
}

fun changeOdcReportDate(filepath: String, datetime: ZonedDateTime): File {
    val tempFile = File.createTempFile(getFileNameWithoutExtension(filepath), ".json")
    val file = File(filepath)

    val rootNode: JsonNode = defaultObjectMapper.readTree(file)
    if (rootNode is ObjectNode) {
        val projectInfoNode = rootNode.path("projectInfo") as ObjectNode
        projectInfoNode.put("reportDate", datetime.toString())

        val scanInfoNode = rootNode.path("scanInfo") as ObjectNode
        val dataSourceArray = scanInfoNode.path("dataSource")
        if (dataSourceArray.isArray) {
            for (dataSourceNode in dataSourceArray) {
                if (dataSourceNode is ObjectNode) {
                    dataSourceNode.put("timestamp", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(datetime))
                }
            }
        }
    }
    defaultObjectMapper.writeValue(tempFile, rootNode)

    return tempFile
}

fun changeSecurityTestReportDate(filepath: String, datetime: ZonedDateTime): File {
    val tempFile = File.createTempFile(getFileNameWithoutExtension(filepath), ".json")
    val file = File(filepath)

    val rootNode: JsonNode = defaultObjectMapper.readTree(file)
    if (rootNode is ObjectNode) {
        rootNode.put("date", DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(datetime))
    }
    defaultObjectMapper.writeValue(tempFile, rootNode)

    return tempFile
}

fun generateFreshCDlibReport(filename: String, reportFile: File, substitutes: List<String> = emptyList()): File {
    val tempFile = File.createTempFile(getFileNameWithoutExtension(filename), ".json")
    TestResultParser.parse(
        reportFile,
        TestResultPrefixes(),
        substitutes
    )?.let { testResult ->
        val test = if (testResult is OslcTestPreResult) {
            OslcTestResult.from(
                testResult,
                false,
                File("src/test/resources/oslc/acceptedList.json")
            )
        } else {
            testResult
        }

        val report = Report(
            test = test,
            testHash = reportFile.sha256sum()
        )
        val byteArrayInputStream = defaultObjectMapper.writeValueAsBytes(report).inputStream()
        FileOutputStream(tempFile).use { fos ->
            byteArrayInputStream.copyTo(fos)
        }
    }
    return tempFile
}

fun getFileNameWithoutExtension(filePath: String): String {
    val filename = filePath.substringAfterLast(File.separator)
    return filename.substringBeforeLast(".")
}
