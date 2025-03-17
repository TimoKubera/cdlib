package de.deutschepost.sdm.cdlib.utils

import java.net.InetAddress
import java.net.spi.InetAddressResolver
import java.net.spi.InetAddressResolver.LookupPolicy
import java.net.spi.InetAddressResolverProvider
import java.util.stream.Stream


class ZScalerInetAddressResolver(private val defaultResolver: InetAddressResolver) :
    InetAddressResolver {
    override fun lookupByName(host: String, lookupPolicy: LookupPolicy): Stream<InetAddress> =
        when (host) {
            "cdlib-dashboard-npi.documents.azure.com" -> zscalerLookup("10.175.17.100", lookupPolicy)
            "cdlib-dashboard-npi-westeurope.documents.azure.com" -> zscalerLookup("10.175.17.101", lookupPolicy)
            "cdlib-dashboard.documents.azure.com" -> zscalerLookup("10.187.37.92", lookupPolicy)
            "cdlib-dashboard-westeurope.documents.azure.com" -> zscalerLookup("10.187.37.93", lookupPolicy)
            else -> defaultResolver.lookupByName(host, lookupPolicy)
        }

    override fun lookupByAddress(addr: ByteArray): String =
        defaultResolver.lookupByAddress(addr)

    private fun zscalerLookup(ip: String, lookupPolicy: LookupPolicy): Stream<InetAddress> =
        defaultResolver.lookupByName("$ip.ip.deutschepost.de", lookupPolicy)
}

class ZScalerDNSResolverProvider : InetAddressResolverProvider() {
    override fun get(configuration: Configuration): InetAddressResolver {

        return ZScalerInetAddressResolver(configuration.builtinResolver())
    }

    override fun name(): String {
        return "Cosmosdb Dashboard Internet Address Resolver Provider"
    }
}
