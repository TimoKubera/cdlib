package de.deutschepost.sdm.cdlib.change.changemanagement

import de.deutschepost.sdm.cdlib.change.changemanagement.api.ChangeManagementRepository
import getSystemEnvironmentTestListenerWithOverrides
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.annotation.RequiresTag
import io.kotest.core.annotation.Tags
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.style.FunSpec
import io.micronaut.context.annotation.Value
import io.micronaut.test.extensions.kotest5.annotation.MicronautTest

@RequiresTag("IntegrationTest")
@Tags("IntegrationTest")
@MicronautTest
class ChangeManagementRepositoryIntegrationTest(
    @Value("\${change-management-token}") val token: String,
    private val changeManagementRepository: ChangeManagementRepository,
) : FunSpec() {
    override fun listeners(): List<TestListener> = listOf(
        getSystemEnvironmentTestListenerWithOverrides()
    )

    init {
        context("GetItSystem properly encodes url with...") {
            test("...obscure commercial reference") {
                shouldNotThrowAny {
                    changeManagementRepository.getItSystem("SDM Wartung/Betrieb", "Bearer $token")
                }
            }
            test("...regular numerical commercial reference") {
                shouldNotThrowAny {
                    changeManagementRepository.getItSystem("5296", "Bearer $token")
                }
            }
        }

        test("GetITSystem does not throw if multiple are returned for one BTOID") {
            shouldNotThrowAny {
                changeManagementRepository.getItSystem("19B1-5000_M7b_1", "Bearer $token")
            }
        }
    }
}
