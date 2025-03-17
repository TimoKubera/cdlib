package de.deutschepost.sdm.cdlib.utils

import mu.KLogger

fun Throwable.klogSelf(logger: KLogger) {
    logger.error { message.toString() }
    logger.debug { stackTraceToString() }
}

fun Throwable.klogSelfWarn(logger: KLogger) {
    logger.warn { message.toString() }
    logger.debug { stackTraceToString() }
}
