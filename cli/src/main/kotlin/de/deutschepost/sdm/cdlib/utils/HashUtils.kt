package de.deutschepost.sdm.cdlib.utils

import java.io.File
fun String.sha256sum(): String = MESSAGE_DIGEST_ALGORITHM
    .digest(this.toByteArray())
    .fold("") { str, it -> str + "%02x".format(it) }

fun File.sha256sum(): String = MESSAGE_DIGEST_ALGORITHM
    .digest(this.readBytes())
    .fold("") { str, it -> str + "%02x".format(it) }

fun ByteArray.sha256sum(): String = MESSAGE_DIGEST_ALGORITHM
    .digest(this)
    .fold("") { str, it -> str + "%02x".format(it) }

companion object {
    private const val MESSAGE_DIGEST_ALGORITHM: String = "SHA-256"
}
