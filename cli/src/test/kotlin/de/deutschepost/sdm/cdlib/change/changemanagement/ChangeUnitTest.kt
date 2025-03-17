package de.deutschepost.sdm.cdlib.change.changemanagement

import de.deutschepost.sdm.cdlib.change.changemanagement.model.JiraConstants
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest
import java.time.ZonedDateTime

@RequiresTag("UnitTest")
@Tags("UnitTest")
@MicronautTest
class ChangeUnitTest(private val changeTestHelper: ChangeTestHelper) : FunSpec() {
    override fun listeners(): List<TestListener> {
        return listOf(
            getSystemEnvironmentTestListenerWithOverrides()
        )
    }

    init {
        context("Change class is ") {

            val now = ZonedDateTime.now()
            val isTest = true
            val enforceFrozenZone = false
            val isFrozenZone = when {
                isTest && enforceFrozenZone -> true
                isTest -> false
                else -> now in ZonedDateTime.parse("2000-12-05T00:00:00+01:00")
                    .rangeTo(ZonedDateTime.parse("2000-12-24T00:00:00+01:00"))
            }

            val commercialReferenceKey = "DI-22839"
            val change = changeTestHelper.changeWithDefaults()
                .updateType(JiraConstants.ChangeType.PREAUTHORIZED)

            test("initiated properly") {
                change.commercialReference shouldBe commercialReferenceKey
            }
        }
    }
}
