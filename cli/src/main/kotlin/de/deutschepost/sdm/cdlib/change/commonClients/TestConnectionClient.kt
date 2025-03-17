package de.deutschepost.sdm.cdlib.change.commonClients

import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.HttpClientConfiguration
import jakarta.inject.Named
import mu.KLogging

@Factory
class TestConnectionClientFactory(
    private val defaultConfig: HttpClientConfiguration,
    private val o365Config: O365Configuration
) {
    @get:Bean
    @get:Named("default")
    val default = TestConnectionClient(defaultConfig)

    @get:Bean
    @get:Named("o365")
    val o365 = TestConnectionClient(o365Config)
}

class TestConnectionClient(val config: HttpClientConfiguration) {

    // Removed accessToken as per TODO comment
    fun testConnection(url: String, accessToken: String? = null): Boolean {
        config.isFollowRedirects = false
        val client = HttpClient.create(null, config)

        logger.info { "Testing connection to $url" }

        val httpGet = HttpRequest.GET<Any>(url).apply {
            if (accessToken != null) {
                header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            }
        }

        return try {
            val response = client.toBlocking().exchange(httpGet, Argument.STRING, Argument.STRING)
            (response.status in setOf(
                HttpStatus.FOUND,
                HttpStatus.OK,
                HttpStatus.UNAUTHORIZED,
                HttpStatus.MOVED_PERMANENTLY
            )).also {
                if (it) {
                    logger.info { "Connection successful established to $url." }
                } else {
                    logger.error {
                        """|Cannot connect to $url.
                           |Status: ${response.status}
                           |Body: ${response.body}
                        """.trimMargin()
                    }
                }
            }
        } catch (e: Exception) {
            logger.error {
                """Cannot connect to $url.
                | ${e.message}
            """.trimMargin()
            }
            false
        } finally {
            config.isFollowRedirects = true
        }
    }

    fun getErrorMessage() =
        """|There are multiple possibilities:
           |Check if you have firewall clearance
           |Check if you have proxy clearance if needed
           |You can define a proxy for o365 with the following environment variable:
           |MICRONAUT_HTTP_SERVICES_O365_PROXY_ADDRESS="b2b-http.dhl.com:8080"
        """.trimMargin()

    companion object : KLogging()

}
