package de.deutschepost.sdm.cdlib.utils

import picocli.CommandLine.Model.CommandSpec

inline fun <reified T> findMixinByType(commandSpec: CommandSpec): T {
    return commandSpec.mixins().values.first { it.userObject() is T }.userObject() as T
}
