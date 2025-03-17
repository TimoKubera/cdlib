import com.cloudbees.groovy.cps.NonCPS

def call() {
    
    def changeString = ""
    for (def build = currentBuild; build != null && build.result != 'SUCCESS'; build = build.previousBuild) {
        changeString += getChangeString(build)
    }
    if (!changeString) {
        
        changeString = " - No new changes"
    }
    return changeString
}

@NonCPS
def getChangeString(build) {
    MAX_MSG_LEN = 100
    def changeString = ""

    echo "Gathering SCM changes"
    def changeLogSets = build.changeSets
    for (int i = 0; i < changeLogSets.size(); i++) {
        def entries = changeLogSets[i].items
        for (int j = 0; j < entries.length; j++) {
            def entry = entries[j]
            truncated_msg = entry.msg.take(MAX_MSG_LEN)
            changeString += " - ${truncated_msg} [${entry.author.getProperty(hudson.tasks.Mailer.UserProperty.class).getAddress()}]\n"
        }
    }

    
    return changeString
}
