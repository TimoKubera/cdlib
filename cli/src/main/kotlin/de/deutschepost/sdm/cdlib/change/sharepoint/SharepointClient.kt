package de.deutschepost.sdm.cdlib.change.sharepoint

import de.deutschepost.sdm.cdlib.change.metrics.model.Webapproval
import de.deutschepost.sdm.cdlib.change.sharepoint.model.*
import de.deutschepost.sdm.cdlib.change.sharepoint.ntlm.JCIFSNTLMSchemeFactory
import de.deutschepost.sdm.cdlib.utils.permissiveObjectMapper
import mu.KLogging
import org.apache.http.HttpStatus
import org.apache.http.auth.AuthSchemeProvider
import org.apache.http.auth.AuthScope
import org.apache.http.auth.NTCredentials
import org.apache.http.client.config.AuthSchemes
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.config.RegistryBuilder
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils

open class SharepointClient(private val user: String, private val password: String) {
    private val authSchemeRegistry = RegistryBuilder.create<AuthSchemeProvider>()
        .register(AuthSchemes.NTLM, JCIFSNTLMSchemeFactory())
        .build()

    private val credsProvider = BasicCredentialsProvider().apply {
        setCredentials(
            AuthScope(AuthScope.ANY),
            NTCredentials(user, password, "devops.deutschepost.de", "prg-dc.dhl.com")
        )
    }
    private val requestConfig = RequestConfig.custom()
        .setTargetPreferredAuthSchemes(listOf("NTLM"))
        .build()
    protected val client = HttpClients.custom()
        .setDefaultCredentialsProvider(credsProvider)
        .setDefaultAuthSchemeRegistry(authSchemeRegistry)
        .setDefaultRequestConfig(requestConfig)
        .build()

    private fun getDigest(): String? {
        val httpPost = HttpPost("$ISHARE_WEBAPPROVAL_BASE_URL/_api/contextinfo").apply {
            addHeader("Accept", "application/json;odata=verbose")
        }
        return client.execute(httpPost).use { response ->
            logger.info { "Getting Sharepoint Digest: ${response.statusLine}" }
            val content = EntityUtils.toString(response.entity)
            logger.debug { content }
            if (response.statusLine.statusCode == HttpStatus.SC_OK) {
                val sharepointContextWebInformation =
                    permissiveObjectMapper.readValue(content, SharepointContextWebInformation::class.java)
                sharepointContextWebInformation.getDigest()
            } else {
                null
            }
        }
    }

    fun addEntryProd(approvalsListItem: SharepointApprovalsListItem) =
        addEntry(approvalsListItem, ISHARE_PROD_LIST)

    fun addEntryTest(approvalsListItem: SharepointApprovalsListItem): Webapproval {
        val approvalsListItemTest = approvalsListItem.copy(metadata = SharepointApprovalsListItem.Metadata.TEST)
        return addEntry(approvalsListItemTest, ISHARE_TEST_LIST)
    }

    private fun addEntry(
        approvalsListItem: SharepointApprovalsListItem,
        listName: String,
    ): Webapproval {
        val digest =
            checkNotNull(getDigest()) {
                """
                Failed to get digest for Record.
                This is eiter a connection issue or a credentials issue. You can check this with following command:
                curl -v --ntlm -u 'USER:PASSWORD' \"$ISHARE_WEBAPPROVAL_BASE_URL/_api/web/lists/GetByTitle('Pipeline%20Approvals')\" -H \"Accept: application/json;odata=verbose\"""".trimIndent()
            }

        val webapprovalWithoutURL =
            checkNotNull(verifyAndGenerateWebapproval(approvalsListItem.applicationId, listName == ISHARE_TEST_LIST)) {
                "Failed validating approval documents."
            }

        val httpEntity = EntityBuilder.create().apply {
            text = permissiveObjectMapper.writeValueAsString(approvalsListItem)
        }.build()
        val httpPost = HttpPost("$ISHARE_WEBAPPROVAL_BASE_URL/_api/web/lists/GetByTitle('$listName')/items").apply {
            addHeader("Accept", CONTENT_TYPE_VERBOSITY)
            addHeader("X-RequestDigest", digest)
            addHeader("Content-Type", CONTENT_TYPE_VERBOSITY)
            entity = httpEntity
        }

        val webapprovalUrl = client.execute(httpPost).use { response ->
            logger.info { "Adding Record: ${response.statusLine}" }
            val content = EntityUtils.toString(response.entity)
            logger.debug { content }

            if (response.statusLine.statusCode != HttpStatus.SC_CREATED) {
                logger.error { content }
                throw IllegalStateException("Failed to create Record.")
            } else {
                val sharepointListItemId =
                    permissiveObjectMapper.readValue(content, SharepointApprovalListItemId::class.java)
                sharepointListItemId.getUrl(ISHARE_WEBAPPROVAL_BASE_URL, listName)
            }
        }
        logger.info { "EntryUrl: $webapprovalUrl" }
        return webapprovalWithoutURL.copy(url = webapprovalUrl)
    }


    fun verifyAndGenerateWebapproval(applicationId: Int, isTest: Boolean): Webapproval? {
        val sharepointApprovalConfiguration = getSharepointApprovalConfigurations().findById(applicationId)
        logger.info { "Getting approvalStatus for ApplicationID $applicationId" }
        when {
            sharepointApprovalConfiguration?.status == null -> {
                logger.error {
                    """Cannot find valid pipeline configuration entry for ApplicationID $applicationId
                    Did you already request one: $ISHARE_WEBAPPROVAL_BASE_URL/Lists/Pipeline%20Approval%20Configuration/AllItems.aspx
                    """.trimIndent()
                }
                return null
            }

            sharepointApprovalConfiguration.status != "Approved" -> {
                val logMessage =
                    "ApprovalStatus of ApplicationID $applicationId is ${sharepointApprovalConfiguration.status} and not Approved!"
                if (isTest) {
                    logger.warn { logMessage }
                } else {
                    logger.error { logMessage }
                    return null
                }
            }

            else -> {
                logger.info { "ApprovalStatus of ApplicationID is ${sharepointApprovalConfiguration.status}" }
            }
        }

        val webapplication = Webapproval.Webapplication(
            certification = Webapproval.Webapplication.Certification(
                id = applicationId,
                status = sharepointApprovalConfiguration.status
            ),
            sharepointUrl = "$ISHARE_WEBAPPLICATION_BY_ID_URL${sharepointApprovalConfiguration.listId}"
        )

        return Webapproval(
            url = "$ISHARE_WEBAPPROVAL_BASE_URL${if (isTest) ISHARE_TEST_LIST else ISHARE_PROD_LIST}/AllItems.aspx",
            webapplication = webapplication,
        )
    }

    private fun getSharepointApprovalConfigurations(): SharepointApprovalConfigurations {
        val httpGet =
            HttpGet("$ISHARE_WEBAPPROVAL_BASE_URL/_api/web/lists/GetByTitle('Pipeline%20Approval%20Configuration')/items").apply {
                addHeader("Accept", CONTENT_TYPE_VERBOSITY)
            }
        client.execute(httpGet).use { response ->
            logger.info { "Executed request ${httpGet.requestLine} --- ${response.statusLine}" }
            val content = EntityUtils.toString(response.entity)
            logger.debug { content }

            return permissiveObjectMapper.readValue(content, SharepointApprovalConfigurations::class.java)
        }
    }
            }
        client.execute(httpGet).use { response ->
            logger.info { "Executed request ${httpGet.requestLine} --- ${response.statusLine}" }
            val content = EntityUtils.toString(response.entity)
            logger.debug { content }

            return permissiveObjectMapper.readValue(content, SharepointApprovalConfigurations::class.java)
        }
    }

    companion object : KLogging() {
        const val ISHARE_BASE_URL = "https://itm.prg-dc.dhl.com"
        const val ISHARE_WEBAPPROVAL_BASE_URL = "${ISHARE_BASE_URL}/sites/it-sec"
        const val ISHARE_PROD_LIST = "Pipeline%20Approvals"
        const val ISHARE_TEST_LIST = "Pipeline%20Approvals%20TEST"
        const val ISHARE_WEBAPPLICATION_BY_ID_URL =
            "https://itm.prg-dc.dhl.com/sites/it-sec/Lists/Pipeline%20Approval%20Configuration/DispForm.aspx?ID="
    }
}
