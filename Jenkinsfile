import org.codehaus.groovy.runtime.DateGroovyMethods

pipeline {
    triggers {
        cron 'H 4 * * 3'
    }
    libraries {
        lib("CDlib@${env.CHANGE_BRANCH ?: env.BRANCH_NAME}")
    }
    options {
        // enables performance mode to avoid CPS Errors
        durabilityHint 'PERFORMANCE_OPTIMIZED'
    }
    //activate approval steps
    parameters {
        booleanParam(name: 'RELEASE_BUILD', defaultValue: false, description: 'Should this build release a new major or patch version?')
    }
    //Define the agent, where the pipeline will be executed. If your approvals block your executors either get more, approve faster or set it to none and define individual agents per stage.
    agent {
        kubernetes {
            cloud 'prod-prg'
            inheritFrom 'jenkins-slave-prg'
            yamlFile 'agent-documentation.yml'
        }
    }
    environment {
        HOME = "${env.WORKSPACE}"
        JAVA_TOOL_OPTIONS = "-Duser.home=${env.WORKSPACE}"
        ROOT_CONFLUENCE_URL = "https://confluence1.lcm.deutschepost.de/confluence1"
        MAX_REQUESTS_PER_SECOND = "10"
        ORPHAN_REMOVAL_STRATEGY = "KEEP_ORPHANS"
    }
    stages {
        stage('SETUP') {
            steps {
                script {
                    //load pipeline configuration from same path as Jenkinsfile
                    config = load 'config.jenkins'

                    //! initializes build tool and environment variables
                    withCdlibCliNamesCreate(containerName: 'cdlib-cli') {
                        sh returnStdout: true, script: 'cdlib names create'
                    }
                }
            }
        }
        stage('PREPARE') {
            environment {
                SPACE_KEY = "SDMNPI"
                ANCESTOR_ID = "${config.confluence.ancestorIDTEST}"
            }
            steps {
                //the following section is not DRY, but it doesn't make sense to waste time, since we'll migrate this to Azure Pipelines soon...

                //checkout phippyandfriends for inclusions
                //determine whether to use master or feature branch for inclusions
                withCredentials([string(credentialsId: 'azureDevOpsPAT', variable: 'AZURE_DEVOPS_EXT_PAT')]) {
                    script {
                        //should I use master or the feature branch?
                        String checkoutBranch = ""
                        container('helm') {
                            String branchList = sh(returnStdout: true, script: "az repos ref list --org 'https://dev.azure.com/sw-zustellung-31b3183' --project ICTO-3339_SDM --repository phippyandfriends")
                            if (branchList.contains("${CDLIB_EFFECTIVE_BRANCH_NAME}")) {
                                echo "using branch ${CDLIB_EFFECTIVE_BRANCH_NAME} of https://git.dhl.com/CDLib/phippyandfriends"
                                checkOutBranch = "${CDLIB_EFFECTIVE_BRANCH_NAME}"
                            } else {
                                echo "could not find branch ${CDLIB_EFFECTIVE_BRANCH_NAME} for https://dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/phippyandfriends, using default branch master"
                                checkOutBranch = "master"
                            }
                        }
                        checkout scmGit(branches: [[name: checkOutBranch]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'phippyandfriends']], userRemoteConfigs: [[credentialsId: 'azure_repos_sdm', url: 'https://sw-zustellung-31b3183@dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/_git/phippyandfriends']])
                    }
                }

                //checkout Gitops repository for inclusions
                checkout scmGit(branches: [[name: 'prod']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'gitops']], userRemoteConfigs: [[credentialsId: 'azure_repos_sdm', url: 'https://sw-zustellung-31b3183@dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/_git/gitops']])

                //checkout renovate repository for inclusions
                checkout scmGit(branches: [[name: 'master']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'renovate']], userRemoteConfigs: [[credentialsId: 'azure_repos_sdm', url: 'https://sw-zustellung-31b3183@dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/_git/renovate']])

                //checkout cdaas for inclusions
                //determine whether to use master or feature branch for inclusions
                withCredentials([string(credentialsId: 'azureDevOpsPAT', variable: 'AZURE_DEVOPS_EXT_PAT')]) {
                    script {
                        //should I use master or the feature branch?
                        String checkoutBranch = ""
                        container('helm') {
                            String branchList = sh(returnStdout: true, script: "az repos ref list --org 'https://dev.azure.com/sw-zustellung-31b3183' --project ICTO-3339_SDM --repository cdaas")
                            if (branchList.contains("${CDLIB_EFFECTIVE_BRANCH_NAME}")) {
                                echo "using branch ${CDLIB_EFFECTIVE_BRANCH_NAME} of https://git.dhl.com/CDLib/cdaas"
                                checkOutBranch = "${CDLIB_EFFECTIVE_BRANCH_NAME}"
                            } else {
                                echo "could not find branch ${CDLIB_EFFECTIVE_BRANCH_NAME} for https://dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/cdaas, using default branch master"
                                checkOutBranch = "master"
                            }
                        }
                        checkout scmGit(branches: [[name: checkOutBranch]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'cdaas']], userRemoteConfigs: [[credentialsId: 'azure_repos_sdm', url: 'https://sw-zustellung-31b3183@dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/_git/cdaas']])
                    }
                }

                //checkout cdaas-template-gradle for inclusions
                //determine whether to use master or feature branch for inclusions
                withCredentials([string(credentialsId: 'azureDevOpsPAT', variable: 'AZURE_DEVOPS_EXT_PAT')]) {
                    script {
                        //should I use master or the feature branch?
                        String checkoutBranch = ""
                        container('helm') {
                            String branchList = sh(returnStdout: true, script: "az repos ref list --org 'https://dev.azure.com/sw-zustellung-31b3183' --project ICTO-3339_SDM --repository cdaas-template-gradle")
                            if (branchList.contains("${CDLIB_EFFECTIVE_BRANCH_NAME}")) {
                                echo "using branch ${CDLIB_EFFECTIVE_BRANCH_NAME} of https://git.dhl.com/CDLib/cdaas-template-gradle"
                                checkOutBranch = "${CDLIB_EFFECTIVE_BRANCH_NAME}"
                            } else {
                                echo "could not find branch ${CDLIB_EFFECTIVE_BRANCH_NAME} for https://dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/cdaas-template-gradle, using default branch master"
                                checkOutBranch = "master"
                            }
                        }
                        checkout scmGit(branches: [[name: checkOutBranch]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'cdaas-template-gradle']], userRemoteConfigs: [[credentialsId: 'azure_repos_sdm', url: 'https://sw-zustellung-31b3183@dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/_git/cdaas-template-gradle']])
                    }
                }

                //checkout cdaas-template-maven for inclusions
                //determine whether to use master or feature branch for inclusions
                withCredentials([string(credentialsId: 'azureDevOpsPAT', variable: 'AZURE_DEVOPS_EXT_PAT')]) {
                    script {
                        //should I use master or the feature branch?
                        String checkoutBranch = ""
                        container('helm') {
                            String branchList = sh(returnStdout: true, script: "az repos ref list --org 'https://dev.azure.com/sw-zustellung-31b3183' --project ICTO-3339_SDM --repository cdaas-template-maven")
                            if (branchList.contains("${CDLIB_EFFECTIVE_BRANCH_NAME}")) {
                                echo "using branch ${CDLIB_EFFECTIVE_BRANCH_NAME} of https://git.dhl.com/CDLib/cdaas-template-maven"
                                checkOutBranch = "${CDLIB_EFFECTIVE_BRANCH_NAME}"
                            } else {
                                echo "could not find branch ${CDLIB_EFFECTIVE_BRANCH_NAME} for https://dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/cdaas-template-maven, using default branch master"
                                checkOutBranch = "master"
                            }
                        }
                        checkout scmGit(branches: [[name: checkOutBranch]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'cdaas-template-maven']], userRemoteConfigs: [[credentialsId: 'azure_repos_sdm', url: 'https://sw-zustellung-31b3183@dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/_git/cdaas-template-maven']])
                    }
                }

                //checkout cdaas-template-npm for inclusions
                //determine whether to use master or feature branch for inclusions
                withCredentials([string(credentialsId: 'azureDevOpsPAT', variable: 'AZURE_DEVOPS_EXT_PAT')]) {
                    script {
                        //should I use master or the feature branch?
                        String checkoutBranch = ""
                        container('helm') {
                            String branchList = sh(returnStdout: true, script: "az repos ref list --org 'https://dev.azure.com/sw-zustellung-31b3183' --project ICTO-3339_SDM --repository cdaas-template-npm")
                            if (branchList.contains("${CDLIB_EFFECTIVE_BRANCH_NAME}")) {
                                echo "using branch ${CDLIB_EFFECTIVE_BRANCH_NAME} of https://git.dhl.com/CDLib/cdaas-template-npm"
                                checkOutBranch = "${CDLIB_EFFECTIVE_BRANCH_NAME}"
                            } else {
                                echo "could not find branch ${CDLIB_EFFECTIVE_BRANCH_NAME} for https://dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/cdaas-template-npm, using default branch master"
                                checkOutBranch = "master"
                            }
                        }
                        checkout scmGit(branches: [[name: checkOutBranch]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'cdaas-template-npm']], userRemoteConfigs: [[credentialsId: 'azure_repos_sdm', url: 'https://sw-zustellung-31b3183@dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/_git/cdaas-template-npm']])
                    }
                }

                //checkout cdaas-template-gitops for inclusions
                //determine whether to use master or feature branch for inclusions
                withCredentials([string(credentialsId: 'azureDevOpsPAT', variable: 'AZURE_DEVOPS_EXT_PAT')]) {
                    script {
                        //should I use default or the feature branch?
                        String checkoutBranch = ""
                        container('helm') {
                            String branchList = sh(returnStdout: true, script: "az repos ref list --org 'https://dev.azure.com/sw-zustellung-31b3183' --project ICTO-3339_SDM --repository cdaas-template-gitops")
                            if (branchList.contains("${CDLIB_EFFECTIVE_BRANCH_NAME}")) {
                                echo "using branch ${CDLIB_EFFECTIVE_BRANCH_NAME} of https://git.dhl.com/CDLib/cdaas-template-gitops"
                                checkOutBranch = "${CDLIB_EFFECTIVE_BRANCH_NAME}"
                            } else {
                                echo "could not find branch ${CDLIB_EFFECTIVE_BRANCH_NAME} for https://dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/cdaas-template-gitops, using default branch prod"
                                checkOutBranch = "prod"
                            }
                        }
                        checkout scmGit(branches: [[name: checkOutBranch]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'cdaas-template-gitops']], userRemoteConfigs: [[credentialsId: 'azure_repos_sdm', url: 'https://sw-zustellung-31b3183@dev.azure.com/sw-zustellung-31b3183/ICTO-3339_SDM/_git/cdaas-template-gitops']])
                    }
                }

                //checkout Images for inclusions
                checkout scmGit(branches: [[name: 'main']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'Images']], userRemoteConfigs: [[credentialsId: 'SDM-git.dhl.com-Access', url: 'https://git.dhl.com/CDLib/image']])

                //checkout sockshop services for inclusions
                //front-end
                //determine whether to use master or feature branch for inclusions
                withCredentials([string(credentialsId: 'github-token', variable: 'GH_ENTERPRISE_TOKEN')]) {
                    script {
                        //should I use master or the feature branch?
                        checkoutBranch = ""
                        container('cdlib-cli') {
                            String branchContent = sh(returnStdout: true, script: "gh repo view https://git.dhl.com/SockShop/front-end --branch ${CDLIB_EFFECTIVE_BRANCH_NAME}")
                            if (branchContent.contains("Front-end app")) {
                                echo "using branch ${CDLIB_EFFECTIVE_BRANCH_NAME} of https://git.dhl.com/SockShop/front-end directly, since there is no PR for it"
                                checkOutBranch = "${CDLIB_EFFECTIVE_BRANCH_NAME}"
                            } else {
                                echo "could not find branch ${CDLIB_EFFECTIVE_BRANCH_NAME} for https://git.dhl.com/SockShop/front-end, using default branch master"
                                checkOutBranch = "master"
                            }
                        }
                    }
                }
                checkout scmGit(branches: [[name: checkOutBranch]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'frontend']], userRemoteConfigs: [[credentialsId: 'SDM-git.dhl.com-Access', url: 'https://git.dhl.com/SockShop/front-end']])
                //carts
                //determine whether to use master or feature branch for inclusions
                withCredentials([string(credentialsId: 'github-token', variable: 'GH_ENTERPRISE_TOKEN')]) {
                    script {
                        //should I use master or the feature branch?
                        checkoutBranch = ""
                        container('cdlib-cli') {
                            String branchContent = sh(returnStdout: true, script: "gh repo view https://git.dhl.com/SockShop/carts --branch ${CDLIB_EFFECTIVE_BRANCH_NAME}")
                            if (branchContent.contains("shopping carts")) {
                                echo "using branch ${CDLIB_EFFECTIVE_BRANCH_NAME} of https://git.dhl.com/SockShop/carts directly, since there is no PR for it"
                                checkOutBranch = "${CDLIB_EFFECTIVE_BRANCH_NAME}"
                            } else {
                                echo "could not find branch ${CDLIB_EFFECTIVE_BRANCH_NAME} for https://git.dhl.com/SockShop/carts, using default branch master"
                                checkOutBranch = "master"
                            }
                        }
                    }
                }
                checkout scmGit(branches: [[name: checkOutBranch]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'carts']], userRemoteConfigs: [[credentialsId: 'SDM-git.dhl.com-Access', url: 'https://git.dhl.com/SockShop/carts']])

                //payment
                //determine whether to use master or feature branch for inclusions
                withCredentials([string(credentialsId: 'github-token', variable: 'GH_ENTERPRISE_TOKEN')]) {
                    script {
                        //should I use master or the feature branch?
                        checkoutBranch = ""
                        container('cdlib-cli') {
                            String branchContent = sh(returnStdout: true, script: "gh repo view https://git.dhl.com/SockShop/payment --branch ${CDLIB_EFFECTIVE_BRANCH_NAME}")
                            if (branchContent.contains("payment services")) {
                                echo "using branch ${CDLIB_EFFECTIVE_BRANCH_NAME} of https://git.dhl.com/SockShop/payment directly, since there is no PR for it"
                                checkOutBranch = "${CDLIB_EFFECTIVE_BRANCH_NAME}"
                            } else {
                                echo "could not find branch ${CDLIB_EFFECTIVE_BRANCH_NAME} for https://git.dhl.com/SockShop/payment, using default branch master"
                                checkOutBranch = "master"
                            }
                        }
                    }
                }
                checkout scmGit(branches: [[name: checkOutBranch]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'payment']], userRemoteConfigs: [[credentialsId: 'SDM-git.dhl.com-Access', url: 'https://git.dhl.com/SockShop/payment']])

                //shipping
                //determine whether to use master or feature branch for inclusions
                withCredentials([string(credentialsId: 'github-token', variable: 'GH_ENTERPRISE_TOKEN')]) {
                    script {
                        //should I use master or the feature branch?
                        checkoutBranch = ""
                        container('cdlib-cli') {
                            String branchContent = sh(returnStdout: true, script: "gh repo view https://git.dhl.com/SockShop/shipping --branch ${CDLIB_EFFECTIVE_BRANCH_NAME}")
                            if (branchContent.contains("shipping capabilities")) {
                                echo "using branch ${CDLIB_EFFECTIVE_BRANCH_NAME} of https://git.dhl.com/SockShop/shipping directly, since there is no PR for it"
                                checkOutBranch = "${CDLIB_EFFECTIVE_BRANCH_NAME}"
                            } else {
                                echo "could not find branch ${CDLIB_EFFECTIVE_BRANCH_NAME} for https://git.dhl.com/SockShop/shipping, using default branch master"
                                checkOutBranch = "master"
                            }
                        }
                    }
                }
                checkout scmGit(branches: [[name: checkOutBranch]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'shipping']], userRemoteConfigs: [[credentialsId: 'SDM-git.dhl.com-Access', url: 'https://git.dhl.com/SockShop/shipping']])

                //acceptance-test
                //determine whether to use master or feature branch for inclusions
                withCredentials([string(credentialsId: 'github-token', variable: 'GH_ENTERPRISE_TOKEN')]) {
                    script {
                        //should I use master or the feature branch?
                        checkoutBranch = ""
                        container('cdlib-cli') {
                            String branchContent = sh(returnStdout: true, script: "gh repo view https://git.dhl.com/SockShop/acceptance-test --branch ${CDLIB_EFFECTIVE_BRANCH_NAME}")
                            if (branchContent.contains("cucumber")) {
                                echo "using branch ${CDLIB_EFFECTIVE_BRANCH_NAME} of https://git.dhl.com/SockShop/acceptance-test directly, since there is no PR for it"
                                checkOutBranch = "${CDLIB_EFFECTIVE_BRANCH_NAME}"
                            } else {
                                echo "could not find branch ${CDLIB_EFFECTIVE_BRANCH_NAME} for https://git.dhl.com/SockShop/acceptance-test, using default branch master"
                                checkOutBranch = "master"
                            }
                        }
                    }
                }
                checkout scmGit(branches: [[name: checkOutBranch]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'acceptance-test']], userRemoteConfigs: [[credentialsId: 'SDM-git.dhl.com-Access', url: 'https://git.dhl.com/SockShop/acceptance-test']])

                //checkout terraform-registry for includes
                checkout scmGit(branches: [[name: 'master']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'terraform-registry']], userRemoteConfigs: [[credentialsId: 'SDM-git.dhl.com-Access', url: 'https://git.dhl.com/CDLib/terraform_registry']])

                // render asciidoc files and create png from puml files
                container('asciidoctor') {
                    dir('confluence') {
                        sh 'find . -type f -name \'*.adoc\' | xargs -I {} asciidoctor --failure-level=WARN -r asciidoctor-diagram -b docbook {} -a allow-uri-read'
                    }
                }

                //test export docs to LCM confluence as subpage for https://lcm.deutschepost.de/confluence1/display/SDMNPI/SDMNPI+Startseite
                //according to https://confluence-publisher.atlassian.net/wiki/spaces/CPD/overview
                withCredentials([string(credentialsId: 'tfcd_bot-confluence-token', variable: 'PASSWORD')]) {
                    container('confluence-publisher') {
                        //export static pages
                        sh "VERSION_MESSAGE='${CDLIB_RELEASE_NAME}' ASCIIDOC_ROOT_FOLDER=confluence/static PAGE_TITLE_PREFIX=${CDLIB_EFFECTIVE_BRANCH_NAME}_ publish.sh"
                        //export release specific pages
                        sh "VERSION_MESSAGE='${CDLIB_RELEASE_NAME}' ASCIIDOC_ROOT_FOLDER=confluence/release PAGE_TITLE_PREFIX=${CDLIB_EFFECTIVE_BRANCH_NAME}_ publish.sh"
                    }
                }
            }
        }
        stage('UPDATE_DOCS') {
            when {
                branch 'master'
            }
            environment {
                SPACE_KEY = "SDM"
                ANCESTOR_ID = "${config.confluence.ancestorIDLATEST}"
            }
            steps {
                //export docs to LCM confluence as subpage for https://lcm.deutschepost.de/confluence1/display/SDM/CDlib tagged as latest for search without suffix
                //according to https://confluence-publisher.atlassian.net/wiki/spaces/CPD/overview
                withCredentials([string(credentialsId: 'tfcd_bot-confluence-token', variable: 'PASSWORD')]) {
                    container('confluence-publisher') {
                        //export static pages
                        sh "VERSION_MESSAGE='${CDLIB_RELEASE_NAME}' ASCIIDOC_ROOT_FOLDER=confluence/static publish.sh"
                        //export release specific pages
                        sh "VERSION_MESSAGE='${CDLIB_RELEASE_NAME}' ASCIIDOC_ROOT_FOLDER=confluence/release publish.sh"
                    }
                }
            }
        }
        stage('RELEASE') {
            when {
                expression { params.RELEASE_BUILD }
            }
            environment {
                SPACE_KEY = "SDM"
                ANCESTOR_ID = "${config.confluence.ancestorIDRELEASE}"
            }
            steps {
                script {
                    String oldVersion = '0.0.0-SNAPSHOT'
                    String newVersion = input message: 'Which should be the next release version?', parameters: [string(defaultValue: '', description: 'release version: x.x', name: 'version', trim: true)]
                    String newVersionTag = newVersion.replace('.', '_')

                    // create new release branch
                    setupLocalGit()
                    def releaseDate = "== ${oldVersion}\\n\\n== ${newVersion} - ${DateGroovyMethods.format(new Date(), 'yyyy-MM-dd')}"
                    currentBuild.displayName = newVersion

                    sh "sed -i 's|${oldVersion}|${releaseDate}|g' confluence/static/Changelog.adoc"
                    sh "git commit -a -m 'Update Changelog.adoc for release ${newVersion}'"
                    withCredentials([usernamePassword(credentialsId: config.git.credentialsId,
                            passwordVariable: 'GIT_PASSWORD',
                            usernameVariable: 'GIT_USERNAME')]) {
                        sh 'git push -u origin HEAD:master'
                    }

                    String majorVersion = newVersion.substring(0, newVersion.indexOf('.'))
                    sh "git tag ${newVersion}"
                    sh "git tag ${majorVersion}.Latest"
                    sh "git tag ${majorVersion}.latest"

                    withCredentials([usernamePassword(credentialsId: config.git.credentialsId,
                            passwordVariable: 'GIT_PASSWORD',
                            usernameVariable: 'GIT_USERNAME')]) {
                        sh 'git push --force origin --tags'
                    }

                    //replace label "latest" with "${newVersion}" for release export to avoid search result redundancy but allow targeted search
                    sh "find confluence/release -type f -exec sed -i 's|:keywords: latest|:keywords: ${newVersionTag}|g' {} +"

                    //export docs to LCM confluence as subpage for https://lcm.deutschepost.de/confluence1/display/SDM/CDlib/Archive tagged and suffixed with release version
                    //according to https://confluence-publisher.atlassian.net/wiki/spaces/CPD/overview
                    withCredentials([string(credentialsId: 'tfcd_bot-confluence-token', variable: 'PASSWORD')]) {
                        container('confluence-publisher') {
                            //export release specific pages
                            sh "VERSION_MESSAGE='CDlib ${newVersion} via ${CDLIB_RELEASE_NAME}' ASCIIDOC_ROOT_FOLDER=confluence/release PAGE_TITLE_SUFFIX=' ${newVersion}' publish.sh"
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            monitorJira('Close Issue', 'SDM-139')
        }
        failure {
            monitorJira('Reopen Issue', 'SDM-139')
        }
    }
}

void setupLocalGit(String name = 'Jenkins', String mail = 'lieferheld@deutschepost.de') {
    final String text = '''\
#!/bin/bash
echo username=$GIT_USERNAME
echo password=$GIT_PASSWORD
'''
    writeFile file: 'credentialsHelper.sh', text: text

    sh 'git config --local credential.helper "/bin/bash credentialsHelper.sh"'
    sh "git config --local user.name 'Jenkins'"
    sh "git config --local user.email 'lieferheld@deutschepost.de'"
}
