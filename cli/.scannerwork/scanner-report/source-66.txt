package de.deutschepost.sdm.cdlib.change.sharepoint

import com.azure.core.credential.TokenRequestContext
import com.azure.identity.ClientSecretCredentialBuilder
import io.micronaut.context.annotation.Value
import io.micronaut.retry.annotation.Retryable
import jakarta.inject.Singleton

@Singleton
@Retryable
class GraphTokenProvider(
    @Value("\${graph-client-id}")
    private val clientId: String,
    @Value("\${graph-secret}")
    private val clientSecret: String
) {
    val token: String by lazy {
        getAccessToken()
    }

    private fun getAccessToken(): String {
        val servicePrincipal = ClientSecretCredentialBuilder()
            .tenantId(itsTenantId)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .build()

        val tokenRequestContext = TokenRequestContext().addScopes("https://graph.microsoft.com/.default")
        val accessToken = servicePrincipal.getToken(tokenRequestContext).block()

        requireNotNull(accessToken) {
            "Failed to get Microsoft Graph access token"
        }

        return accessToken.token
    }

    companion object {
        private const val itsTenantId = "cd99fef8-1cd3-4a2a-9bdf-15531181d65e"
    }
}
