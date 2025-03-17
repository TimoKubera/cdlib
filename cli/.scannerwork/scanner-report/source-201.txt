package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.spec.SpecExecutionOrder
import io.micronaut.test.extensions.kotest5.MicronautKotest5Extension

object ProjectConfig : AbstractProjectConfig() {
    override fun extensions() = listOf(MicronautKotest5Extension)
    override val specExecutionOrder = SpecExecutionOrder.Lexicographic
}
