package de.deutschepost.sdm.cdlib.change.commonClients

import io.micronaut.http.client.HttpClientConfiguration
import io.micronaut.http.client.ServiceHttpClientConfiguration
import jakarta.inject.Named
import jakarta.inject.Singleton

@Singleton
class O365Configuration(@param:Named("o365") val o365Config: ServiceHttpClientConfiguration) :
    HttpClientConfiguration(o365Config) {
    override fun getConnectionPoolConfiguration(): ConnectionPoolConfiguration = o365Config.connectionPoolConfiguration
}
