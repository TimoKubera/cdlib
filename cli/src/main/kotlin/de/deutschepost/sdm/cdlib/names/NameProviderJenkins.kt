package de.deutschepost.sdm.cdlib.names

import jakarta.inject.Singleton

@Singleton
class NameProviderJenkins(override val resolver: NameResolverJenkins) : NameProvider {
    override val platformType: PlatformType = PlatformType.JENKINS

    override fun provideName(name: Names, value: String): String {
        return "$name=${value.trim()}"
    }
}
