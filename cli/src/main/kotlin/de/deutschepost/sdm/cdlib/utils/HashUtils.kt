package de.deutschepost.sdm.cdlib.utils

import java.io.File
import java.security.MessageDigest

object Constants {
    const val SHA_256_ALGORITHM = "SHA-256"
}

fun String.sha256sum(): String = MessageDigest
    .getInstance(Constants.SHA_256_ALGORITHM)
    .digest(this.toByteArray())
    .fold("") { str, it -> str + "%02x".format(it) }

fun File.sha256sum(): String = MessageDigest
    .getInstance(Constants.SHA_256_ALGORITHM)
    .digest(this.readBytes())
    .fold("") { str, it -> str + "%02x".format(it) }

fun ByteArray.sha256sum(): String = MessageDigest
    .getInstance(Constants.SHA_256_ALGORITHM)
    .digest(this)
    .fold("") { str, it -> str + "%02x".format(it) }
