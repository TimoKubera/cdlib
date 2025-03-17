package de.deutschepost.sdm.cdlib.change.metrics.client

import java.net.InetAddress
import java.net.UnknownHostException
import java.net.spi.InetAddressResolver
import java.net.spi.InetAddressResolver.LookupPolicy
import java.net.spi.InetAddressResolverProvider
import java.util.stream.Stream


class CosmosDashboardInetAddressResolver(private val defaultResolver: InetAddressResolver) : InetAddressResolver {
    @Throws(UnknownHostException::class)
    override fun lookupByName(host: String, lookupPolicy: LookupPolicy): Stream<InetAddress> =
        when (host) {
            "cdlib-dashboard-npi.documents.azure.com" -> "10.175.17.100".toInetAddress()
            "cdlib-dashboard-npi-westeurope.documents.azure.com" -> "10.175.17.101".toInetAddress()
            "cdlib-dashboard.documents.azure.com" -> "10.187.37.92".toInetAddress()
            "cdlib-dashboard-westeurope.documents.azure.com" -> "10.187.37.93".toInetAddress()
            else -> defaultResolver.lookupByName(host, lookupPolicy)
        }

    override fun lookupByAddress(addr: ByteArray): String =
        defaultResolver.lookupByAddress(addr)

    private fun String.toInetAddress(): Stream<InetAddress> {
        val bytes = split(".").map { it.toInt().toByte() }.toByteArray()
        return Stream.of(InetAddress.getByAddress(bytes))
    }
}

class CosmosDashboardResolverProvider : InetAddressResolverProvider() {
    override fun get(configuration: Configuration): InetAddressResolver {

        return CosmosDashboardInetAddressResolver(configuration.builtinResolver())
    }

    override fun name(): String {
        return "Cosmosdb Dashboard Internet Address Resolver Provider"
    }
}
