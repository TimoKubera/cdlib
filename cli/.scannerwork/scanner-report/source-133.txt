package de.deutschepost.sdm.cdlib.utils

import java.io.File
import java.security.MessageDigest

fun String.sha256sum(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this.toByteArray())
    .fold("") { str, it -> str + "%02x".format(it) }

fun File.sha256sum(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this.readBytes())
    .fold("") { str, it -> str + "%02x".format(it) }

fun ByteArray.sha256sum(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this)
    .fold("") { str, it -> str + "%02x".format(it) }
