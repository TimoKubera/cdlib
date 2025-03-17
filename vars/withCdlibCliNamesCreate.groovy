void call(String containerName, Closure booty) {
    container(containerName) {
        def lines
        try {
            lines = booty.call()
        } catch(ignored) {
            error 'cdlib names create failed.'
        }
        echo lines
        for (String line in lines.split('\n')) {
            if (line.startsWith('CDLIB_')) {
                def (k, v) = line.split('=', 2)
                String kTrimmed = k.trim()
                String vTrimmed = v.trim()
                String key = kTrimmed - 'CDLIB_'
                env[key] = vTrimmed
                env[kTrimmed] = vTrimmed
            }
        }
    }
    if (!env['NAMES_CREATE_SUCCESS']?.toBoolean()) {
        error 'cdlib names create failed.'
    }
    currentBuild.displayName = "${env.CDLIB_APP_VERSION}_${env.CDLIB_BUILD_NUMBER}"
}

void call(Map args, Closure booty) {
    call(args['containerName'], booty)
}



/*
pipeline:
withCdlibCliNamesCreate(containerName: 'cdlib-cli') {
        sh returnStdout: true, script: 'cdlib names create'
}



 */
