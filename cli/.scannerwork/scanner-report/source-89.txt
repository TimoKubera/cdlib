package de.deutschepost.sdm.cdlib.names

interface NameProvider {
    val resolver: NameResolver
    val platformType: PlatformType

    fun provideNames() = buildString {
        Names.values()
            .associateWith { resolver[it] }
            .toSortedMap()
            .forEach { (k, v) -> appendLine(provideName(k, v)) }
    }

    fun provideName(name: Names, value: String): String
}
