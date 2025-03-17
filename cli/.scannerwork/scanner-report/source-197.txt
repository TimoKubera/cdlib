package de.deutschepost.sdm.cdlib.utils

import de.deutschepost.sdm.cdlib.names.Names
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.system.OverrideMode
import io.kotest.extensions.system.withEnvironment
import io.kotest.matchers.shouldBe

@RequiresTag("UnitTest")
@Tags("UnitTest")
class EnvUtilsKtTest : FunSpec({

    val regex = Regex("\\W+")

    listOf(
        "RELEASE", "123_RELEASE", "256RELEASE1345erawrg"
    ).forEach {
        test("$it is not sanitized") {
            withEnvironment(
                "CDLIB_RELEASE_NAME" to it,
                OverrideMode.SetOrOverride
            ) {
                resolveEnvByName(Names.CDLIB_RELEASE_NAME) shouldBe it
                resolveEnvByNameSanitized(Names.CDLIB_RELEASE_NAME) shouldBe it
            }
        }
    }

    listOf(
        "ä", "/.,/;", "#$^&%$&!#$", "RELEASE!@245!2%øµ$(),"
    ).forEach {
        test("$it is sanitized with Regex") {
            withEnvironment(
                "CDLIB_RELEASE_NAME" to it,
                OverrideMode.SetOrOverride
            ) {
                resolveEnvByName(Names.CDLIB_RELEASE_NAME) shouldBe it
                resolveEnvByNameSanitized(Names.CDLIB_RELEASE_NAME) shouldBe it.replace(regex, "-")
            }
        }
    }
})
