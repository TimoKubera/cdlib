def call(String update, String issue, Boolean integrationTest = false) {
    //only master and no integration tests
    if (env.BRANCH_NAME == 'master' && !integrationTest) {
        container('jira-cli') {
            withCredentials([string(credentialsId: 'cd_bot_chgm_token', variable: 'JIRA_API_TOKEN')]) {
                sh "JIRA_AUTH_TYPE=bearer jira init --force --installation Local --server https://jira1.lcm.deutschepost.de/jira1/ --login tfcd_bot --project SDM --board 'Lieferhelden Board'"
                //get issue state
                String issueDetail = sh returnStdout: true, script: "JIRA_AUTH_TYPE=bearer jira issue list --jql 'key = ${issue}' --plain --no-headers --columns status"
                echo "${issueDetail}"
                //if state is not closed and pipeline succeeded
                if (!issueDetail.contains("Closed") && update.contains("Close")) { sh "JIRA_AUTH_TYPE=bearer jira issue move ${issue} '${update}'" }
                //if stage is closed and pipeline failed
                if (issueDetail.contains("Closed") && update.contains("Reopen")) { sh "JIRA_AUTH_TYPE=bearer jira issue move ${issue} '${update}'" }
                //comment status and link to run that healed/broke monitor
                sh "JIRA_AUTH_TYPE=bearer jira issue comment add --no-input ${issue} '${currentBuild.result}: ${BUILD_URL}'"
            }
        }
    } else {
        echo "skip monitor"
    }
}
