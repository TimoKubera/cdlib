package de.deutschepost.sdm.cdlib.mixins.sharepoint

import de.deutschepost.sdm.cdlib.change.sharepoint.SharepointClient
import picocli.CommandLine
import picocli.CommandLine.Option

class SharepointClientMixin {

    @CommandLine.ArgGroup(validate = true, exclusive = false, heading = "", multiplicity = "0..1", order = 3)
    var webApprovalSection: WebApprovalSection = WebApprovalSection()

    class WebApprovalSection {
        @Option(
            names = ["--sharepoint-username"],
            description = ["DPDHL AD account username."]
        )
        var username = ""
            get() {
                require(field.isNotBlank()) {
                    "--sharepoint-username is required for the webapproval."
                }
                return field
            }

        @Option(
            names = ["--sharepoint-password"],
            description = ["DPDHL AD account password."]
        )
        var personaltoken: String = "" //prevents complaints by fortify
            get() {
                require(field.isNotBlank()) {
                    "--sharepoint-password is required for the webapproval."
                }
                return field
            }
    }


    val client by lazy {
        SharepointClient(webApprovalSection.username, webApprovalSection.personaltoken)
    }
}
