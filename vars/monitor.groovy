def call(String update, Integer issue, Boolean skipAlert = false) {
    if (env.BRANCH_NAME == 'master' && !skipAlert) {
        container('cdlib-cli') {
            withCredentials([usernamePassword(credentialsId: 'sdm-githubapp-git.dhl.com', passwordVariable: 'GH_ENTERPRISE_TOKEN', usernameVariable: '')]) {
                sh "GH_HOST=git.dhl.com /usr/bin/gh issue ${update} https://git.dhl.com/CDLib/monitor/issues/${issue} -c '${currentBuild.result}: ${BUILD_URL}'"
            }
        }
    }
}
