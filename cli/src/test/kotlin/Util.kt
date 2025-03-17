import de.deutschepost.sdm.cdlib.change.changemanagement.api.ChangeHandler
import de.deutschepost.sdm.cdlib.utils.mockChangeHandler
import de.deutschepost.sdm.cdlib.utils.mockCosmosDBClient
import de.deutschepost.sdm.cdlib.utils.mockCosmosDBVersionInfo
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.SystemEnvironmentTestListener
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import org.jfrog.artifactory.client.ItemHandle
import org.jfrog.artifactory.client.model.Item
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.io.PrintStream
import java.util.*

private val systemOut = System.out
private val systemErr = System.err

fun String.toArgsArray(): Array<String> {
    val list = split(" ")
    return Array(list.size) { idx ->
        list[idx]
    }
}


fun ItemHandle.exists(): Boolean {
    return try {
        info<Item>()
        true
    } catch (e: IOException) {
        false
    }
}

fun <R> withMockedVersionInfo(changeHandler: ChangeHandler, block: () -> R): R {
    mockCosmosDBVersionInfo()
    mockCosmosDBClient()
    mockChangeHandler(changeHandler)
    val result = block()
    unmockkAll()
    unmockkObject(changeHandler)
    return result
}

class TeeOutputStream : OutputStream() {
    private val outputStream = ByteArrayOutputStream()

    override fun write(b: Int) {
        outputStream.write(b)
        systemOut.write(b)
    }

    fun getText(): String {
        return outputStream.toString()
    }
}

fun <R> withStandardOutput(function: () -> R): Pair<R, String> {
    val teeOutputStream = TeeOutputStream()
    val printedTeeStream = PrintStream(teeOutputStream)
    System.setOut(printedTeeStream)
    val result = function()
    System.setOut(systemOut)
    return Pair(result, teeOutputStream.getText())
}

fun <R> withErrorOutput(function: () -> R): Pair<R, String> {
    val teeOutputStream = TeeOutputStream()
    val printedTeeStream = PrintStream(teeOutputStream)
    System.setErr(printedTeeStream)
    val result = function()
    System.setErr(systemErr)
    return Pair(result, teeOutputStream.getText())
}

fun getSystemEnvironmentTestListenerWithOverrides(overrides: Map<String, String> = emptyMap()): SystemEnvironmentTestListener {
    val defaultEnvs = mapOf(
        "CDLIB_RELEASE_NAME_UNIQUE" to "cli-integration-test-${System.currentTimeMillis()}",
        "CDLIB_RELEASE_NAME" to "cli-integration-test",
        "CDLIB_APP_NAME" to "cli",
        "CDLIB_EFFECTIVE_BRANCH_NAME" to "integration-test-branch",
        "CDLIB_JOB_URL" to "https://integration-test-url.jenkuns.example.com/foo/bar/job/1337",
        "CDLIB_PM_GIT_MAIL" to "integration-test-git-mail",
        "CDLIB_PM_GIT_NAME" to "integration-test-git-author",
        "CDLIB_PM_GIT_ID" to "integration-test-git-id",
        "CDLIB_PM_GIT_LINK" to "integration-test-git-link",
        "CDLIB_PM_GIT_MESSAGE" to "integration-test-git-message",
        "CDLIB_PM_GIT_ORIGIN" to "integration-test-git-origin",
        "CDLIB_CICD_PLATFORM" to "integration-test-platform",
        "CDLIB_PIPELINE_URL" to UUID.randomUUID().toString()
    )
    return SystemEnvironmentTestListener(defaultEnvs + overrides, OverrideMode.SetOrOverride)
}
