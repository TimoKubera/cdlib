package de.deutschepost.sdm.cdlib.utils

import java.io.File
import java.security.MessageDigest

companion object {
    const val HASH_ALGORITHM = "SHA-256"
}

fun String.sha256sum(): String = MessageDigest
    .getInstance(HASH_ALGORITHM)
    .digest(this.toByteArray())
    .fold("") { str, it -> str + "%02x".format(it) }

fun File.sha256sum(): String = MessageDigest
    .getInstance(HASH_ALGORITHM)
    .digest(this.readBytes())
    .fold("") { str, it -> str + "%02x".format(it) }

fun ByteArray.sha256sum(): String = MessageDigest
    .getInstance(HASH_ALGORITHM)
    .digest(this)
    .fold("") { str, it -> str + "%02x".format(it) }
    .digest(this)
    .fold("") { str, it -> str + "%02x".format(it) }
