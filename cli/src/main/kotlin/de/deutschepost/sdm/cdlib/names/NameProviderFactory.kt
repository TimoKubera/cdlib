package de.deutschepost.sdm.cdlib.names

import jakarta.inject.Singleton

fun interface NameProviderFactory {
    fun getProvider(providerType: PlatformType = currentPlatformType): NameProvider
}

@Singleton
class DefaultNameProviderFactory(providers: List<NameProvider>) : NameProviderFactory {
    private val providerMap = providers.associateBy { it.platformType }

    override fun getProvider(providerType: PlatformType): NameProvider {
        return providerMap[providerType] ?: throw IllegalArgumentException("No supported name resolver found")
    }
}
