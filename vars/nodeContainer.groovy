import dhl.multicontainer.Image

def call(List<Image> images, Closure body) {
	String label = "mypod-${UUID.randomUUID().toString()}"
	List containers = []
	for (image in images) {
		if (!checkIfImageExists(image.imageName)) {
			currentBuild.result = 'FAILURE'
			error('The image ' + image.imageName + ' does not exist. Check if it exists in dockerhub or, if it is a custom image, that you pushed it to Artifactoy. In case of any issue please raise a ticket using the form https://gsd.dhl.com/forms/1072')
		}

		containers << containerTemplate(name: image.alias, image: image.imageName, ttyEnabled: true, command: 'cat',
						  resourceRequestCpu: '50m',
						  resourceLimitCpu: '4',
						  resourceRequestMemory: '100Mi',
						  resourceLimitMemory: '8Gi',
						  alwaysPullImage: image.alwaysPull,
						  workingDir: '/var/lib/jenkins',
						  envVars: [
								  envVar(key: 'HOME', value: '/home/multicontainer')
						  ])
	}

	podTemplate(label: label, name: 'jenkins-slave-prg-multi', cloud: 'prod-prg', inheritFrom: 'jenkins-slave-prg',
		containers: containers, imagePullSecrets: ['artifactory', 'internal']) {
		node(label) {
			body()
		}
	}
}

def checkIfImageExists(String imageName) {
	if (imageName.startsWith('docker.artifactory.dhl.com/')) {
		imageName = imageName.substring('docker.artifactory.dhl.com/'.length())
	}
	
	String[] split = imageName.split(':')
	String name = split[0]
	String tag = 'latest'
	if (split.length > 1) {
		tag = split[1]
	}
	
	// If imagename has no folder (i.e. the node image) it is in the library folder
	if (name.indexOf('/') == -1) {
		name = 'library/' + name
	}
	def response
	withCredentials([string(credentialsId: 'jenkins-user', variable: 'token')]) {
 		response = httpRequest url: 'https://artifactory.dhl.com/api/docker/docker/v2/' + name + '/tags/list', customHeaders: [[maskValue: true, name: 'X-JFrog-Art-Api', value: token]], validResponseCodes: '200,404,403', quiet: true
	}
	
	
	if (response.status == 404) {
		return false
	}
	
	if (response.status == 403) {
		error('could not authenticate against https://artifactory.dhl.com/api/docker/docker/v2/ with credentialsId: jenkins-user, please open a ticket via https://gsd.dhl.com/forms/6012 with a link to your failing build')
		return false
	}
	
	def jsonResponse = readJSON text: response.content
	if (jsonResponse.containsKey('tags')) {
		return jsonResponse.tags.contains(tag)
	} else {
		return false
	}
}
