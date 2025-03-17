package de.deutschepost.sdm.cdlib.release.report.external.cca

import de.deutschepost.sdm.cdlib.release.report.external.CcaTestResult
import io.micronaut.http.BasicAuth
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.annotation.Client
import io.micronaut.retry.annotation.Retryable
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Client
@Retryable
interface HarborApiClient {
    @Get("https://{registry}/api/v2.0/projects/{project}/repositories/{repository}/artifacts?q=tags%3D{reference}&with_scan_overview=true")
    @Header(name = HttpHeaders.ACCEPT, value = "application/json")
    @Header(
        name = "X-Accept-Vulnerabilities",
        value = "application/vnd.security.vulnerability.report; version=1.1, application/vnd.scanner.adapter.vuln.report.harbor+json; version=1.0'"
    )
    fun getScanStatus(
        registry: String,
        project: String,
        repository: String,
        reference: String,
        basicAuth: BasicAuth
    ): HttpResponse<ScanStatus>

    @Get("https://{registry}/api/v2.0/projects/{project}/repositories/{repository}/artifacts/{reference}/additions/vulnerabilities")
    @Header(name = HttpHeaders.ACCEPT, value = "application/json")
    @Header(
        name = "X-Accept-Vulnerabilities",
        value = "application/vnd.security.vulnerability.report; version=1.1, application/vnd.scanner.adapter.vuln.report.harbor+json; version=1.0'"
    )
    fun getVulnerabilities(
        registry: String,
        project: String,
        repository: String,
        reference: String,
        basicAuth: BasicAuth
    ): HttpResponse<CcaTestResult>

    @Get("https://{registry}/api/v2.0/projects?name={project}&with_detail=true")
    fun getCveAllowList(registry: String, project: String, basicAuth: BasicAuth): HttpResponse<CcaSuppressionList>

    @Get("https://{registry}/api/v2.0/projects/{project}/repositories/{repository}/artifacts?q=tags%3D{reference}")
    @Header(name = HttpHeaders.ACCEPT, value = "application/json")
    fun checkIfPresent(
        registry: String,
        project: String,
        repository: String,
        reference: String,
        basicAuth: BasicAuth
    ): HttpResponse<String>
}

const val CSS_QHCR_HARBOR = "dpdhl.css-qhcr-pi.azure.deutschepost.de"
fun getCcaVulnerabilitiesUrl(registry: String, project: String, repository: String, reference: String): String {
    // harbor uses double url encoding for the repository. we have to emulate it here.
    val encodedRepository = URLEncoder.encode(repository, StandardCharsets.UTF_8)
    return "https://$registry/api/v2.0/projects/$project/repositories/$encodedRepository/artifacts/$reference/additions/vulnerabilities"
}
