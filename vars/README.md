# Steps
This page contains information in regards to all our custom pipeline steps. For every step we describe the purpose, usage, configuration and give an example implementation and result.
If a parameter or configuration has a default value, it is always optional.

## approveChangeRequest
Only for **GSNLib Service**\
Allow approval of a change request by an approver from a approver group.

### Parameter
| Parameter | Type | Description |
| --------- | ---- | ----------- |
| groupApproverId | String | Approval ID of a group for a change request |
| approver | String | ID of the approver |
| gsn | String | GSNWebService class Object to use |

## beforeProd
Only for **GSNLib Service**\
This is an automated change management procedure used to create a change request 'createNewChangeRequest' and go through the process of approval i.e. creating approval groups `createGroupApproval`, 
creating and updating change tasks(`createCTask`, `updateCtask`), approving change request `approveChangeRequest` and updating change request `updateChangeRequest`. 
Returns the information about the change and the various tasks as an object of the Change class `changeBean`.
Uses `withGSN` for creating a GSNWebService Object for use in the various operation. The operations of beforeProd are provided as closure to `withGSN`.
Prints the created change number on success.

### Parameter
| Parameter | Type | Description |
| --------- | ---- | ----------- |
| templateNumber | String | Used to create a change request based on a template |
| requestor | String | Used to find the identifier of the change requestor |
| workGroup | String | Used to find the group identifier |
| description | String | A short description for the change request |
| approver | String | Used to find the approver identifier |
| hostname | String | Hostname of the GSN Portal |
| credential | String | Credentials for the GSN Portal |
### Example Code
#### Pipeline
```groovy
stage('GSNLib beforeProd') {
    steps {
        script {
            change = beforeProd(config.gsn.templaterfc, config.gsn.requestor, config.gsn.group, config.gsn.subject, config.gsn.approver, config.gsn.url, config.gsn.credential)
        }
    }
}
```
#### Config
```groovy
gsn: [
        templaterfc: 'RFC1283950',
        requestor: 'ab6jg8',
        group: 'GLOBAL-CI.SUPPORT',
        subject: 'CDLib GSN Initial Testing',
        approver: 'ab6jg8',
        url: 'https://servicenow-uat.dhl.com',
        credential: 'gsn_sa'
],
```
#### Result 
```
[Pipeline] stage
[Pipeline] { (GSNLib beforeProd)
[Pipeline] script
[Pipeline] {
[Pipeline] echo
15:27:06  Initiating
[Pipeline] httpRequest
15:27:06  HttpMethod: POST
15:27:06  URL: https://servicenow-uat.dhl.com/sys_user.do?hierarchical=true&SOAP
15:27:06  SOAPAction: urn:getRecords
15:27:06  Content-Type: text/xml;charset=UTF-8
15:27:06  Using authentication: gsn_sa
15:27:06  Sending request to url: https://servicenow-uat.dhl.com/sys_user.do?hierarchical=true&SOAP
15:27:06  Response Code: HTTP/1.1 200 OK
15:27:06  Response: 
15:27:06  <"Response Data in XML Format">
15:27:06  Success code from [200‥200]
..
.. 25 more Http Request are done successfully ..
..
[Pipeline] echo
15:27:18  ########################################
15:27:18  Created RFC: RFC1404548
[Pipeline] echo
15:27:18  beforeProd complete
[Pipeline] }
[Pipeline] // script
[Pipeline] }
```

## createCTask
Only for **GSNLib Service**\
Creates various tasks during the change process for example, implement change, test change and the post incident report.
Returns information about the created task.

### Parameter
| Parameter | Type | Description |
| --------- | ---- | ----------- |
| data | Map | Information about the task to create like requestor, group, creation time, etc |
| changeRequestId | String | Change to which the task would belong |
| ctaskTemplateId | String | Task template to use for task creation |
| gsn | GSNWebService | GSNWebService class Object to use |

## createGroupApproval
Only for **GSNLib Service**\
Creates an approval group for the change process.
Return group information

### Parameter
| Parameter | Type | Description |
| --------- | ---- | ----------- |
| data | Map | Information about the approval group like requestor and bimodal group id |
| changeRequestId | String | Change to which the approval group would belong |
| gsn | GSNWebService | GSNWebService class Object to use |

## createNewChangeRequest
Only for **GSNLib Service**\
Creates a change request based on a template change number and data about the one to create. 
Also updates information like service outage and impact. Return a new change request information.

### Parameter
| Parameter | Type | Description |
| --------- | ---- | ----------- |
| data | Map | Information about the new change request like author, group, creation date, backout owner, etc |
| templateNumber | String | Template to use for change creation  |
| gsn | GSNWebService | GSNWebService class Object to use |


## getChangeLogs
Provides the information about the changes to the code from the Jenkins `changeSets` provided by the repository.

## image
Only for **scripted pipelines** on Jenkins, for declarative pipelines please check [this guide for kubernetes agents](../confluence/release/Tutorials/KubernetesAsJenkinsAgent.adoc)\
Create an instance of the [Image class](../src/dhl/multicontainer/Image.md) used in the multicontainer approach.
### Parameter
| Parameter | Default Value | Description |
| --------- | ------------- | ----------- |
| alias | (none) | name of the container spawned for the image |
| imageName |  (none)  | image reference name in the image registry |
| alwaysPull | true | option to pull the newest image before use |
### Example Code
Refer [nodeContainer](#nodecontainer)

## implementationKO
Only for **GSNLib Service**\
Updates the change task `updateCtask` and the change request `updateChangeRequest` with the rolled-back task and the completed change event respectively.
Uses `withGSN` for creating a GSNWebService Object for use in the various operation. The operations of implementationKO are provided as closure to `withGSN`.

### Parameter
| Parameter | Type | Description |
| --------- | ---- | ----------- |
| change | Change | Information about the change request including its tasks |
| hostname | String | Hostname of the GSN Portal |
| credential | String | Credentials for the GSN Portal |
### Example Code
#### Pipeline
```groovy
stage('PROD') {
    steps {
        script {
            // update Change
            rollback = false
            def result = input message: 'Test your application. Deployment was ok?', parameters: [choice(choices: 'Everything ok, continue\nRollback', description: '', name: 'next_action')]
            if (result == 'Everything ok, continue') {
                implementationOK(change, config.gsn.url, config.gsn.credential)
            } else {
                implementationKO(change, config.gsn.url, config.gsn.credential)
                rollback = true
            }
        }
    }
}
```
#### Config
```groovy
gsn: [
        templaterfc: 'RFC1283950',
        requestor: 'ab6jg8',
        group: 'GLOBAL-CI.SUPPORT',
        subject: 'CDLib GSN Initial Testing',
        approver: 'ab6jg8',
        url: 'https://servicenow-uat.dhl.com',
        credential: 'gsn_uat'
]
```
#### Result 
```
[Pipeline] stage
[Pipeline] { (PROD)
[Pipeline] script
[Pipeline] {
[Pipeline] input
15:27:18  Input requested
15:27:24  Approved by Werner,M.,SNL IT P&P,Abt.4210,DD,External, external
[Pipeline] httpRequest
15:27:24  HttpMethod: POST
15:27:24  URL: https://servicenow-uat.dhl.com/change_task.do?hierarchical=true&SOAP
15:27:24  SOAPAction: urn:update
15:27:24  Content-Type: text/xml;charset=UTF-8
15:27:24  Using authentication: gsn_uat
15:27:24  Sending request to url: https://servicenow-uat.dhl.com/change_task.do?hierarchical=true&SOAP
15:27:25  Response Code: HTTP/1.1 200 OK
15:27:25  Response: 
15:27:25  <"Response Data in XML Format">
15:27:25  Success code from [200‥200]
..
.. 1 more Http Request are done successfully ..
..
[Pipeline] echo
15:27:26  ########################################
15:27:26  Implemented RFC: RFC1404548
[Pipeline] echo
15:27:26  implementationKO complete
[Pipeline] }
[Pipeline] // script
[Pipeline] }
```

## implementationOK
Only for **GSNLib Service**\
Updates the change task `updateCtask` and the change request `updateChangeRequest` with the implemented status and the completed change event respectively.
Uses `withGSN` for creating a GSNWebService Object for use in the various operation. The operations of implementationOK are provided as closure to `withGSN`.

For more information regarding this step and its integration, please have a look at our [GSN User documentation](../confluence/release/Tutorials/ItsGSN.adoc) here.
### Parameter
| Parameter | Type | Description |
| --------- | ---- | ----------- |
| change | Change | Information about the change request including its tasks |
| hostname | String | Hostname of the GSN Portal |
| credential | String | Credentials for the GSN Portal |
### Example Code
#### Pipeline
```groovy
stage('PROD') {
    steps {
        script {
            // update Change
            rollback = false
            def result = input message: 'Test your application. Deployment was ok?', parameters: [choice(choices: 'Everything ok, continue\nRollback', description: '', name: 'next_action')]
            if (result == 'Everything ok, continue') {
                implementationOK(change, config.gsn.url, config.gsn.credential)
            } else {
                implementationKO(change, config.gsn.url, config.gsn.credential)
                rollback = true
            }
        }
    }
}
```
#### Config
```groovy
gsn: [
        templaterfc: 'RFC1283950',
        requestor: 'ab6jg8',
        group: 'GLOBAL-CI.SUPPORT',
        subject: 'CDLib GSN Initial Testing',
        approver: 'ab6jg8',
        url: 'https://servicenow-uat.dhl.com',
        credential: 'gsn_uat'
],
```
#### Result 
```
[Pipeline] stage
[Pipeline] { (PROD)
[Pipeline] script
[Pipeline] {
[Pipeline] input
15:27:18  Input requested
15:27:24  Approved by Werner,M.,SNL IT P&P,Abt.4210,DD,External, external
[Pipeline] httpRequest
15:27:24  HttpMethod: POST
15:27:24  URL: https://servicenow-uat.dhl.com/change_task.do?hierarchical=true&SOAP
15:27:24  SOAPAction: urn:update
15:27:24  Content-Type: text/xml;charset=UTF-8
15:27:24  Using authentication: gsn_uat
15:27:24  Sending request to url: https://servicenow-uat.dhl.com/change_task.do?hierarchical=true&SOAP
15:27:25  Response Code: HTTP/1.1 200 OK
15:27:25  Response: 
15:27:25  <"Response Data in XML Format">
15:27:25  Success code from [200‥200]
..
.. 1 more Http Request are done successfully ..
..
[Pipeline] echo
15:27:26  ########################################
15:27:26  Implemented RFC: RFC1404548
[Pipeline] echo
15:27:26  implementationOK complete
[Pipeline] }
[Pipeline] // script
[Pipeline] }
```

## PIRAndClose
Only for **GSNLib Service**\
Creates a Post Incident Report(PIR) and closes the change request. Updates the task `updateCtask` and the change request `updateChangeRequest` with the result based on the `rollback` flag.
Uses `withGSN` for creating a GSNWebService Object for use in the various operation. The operations of PIRAndClose are provided as closure to `withGSN`.

For more information regarding this step and its integration, please have a look at our [GSN User documentation](../confluence/release/Tutorials/ItsGSN.adoc) here.
### Parameter
| Parameter | Type | Description |
| --------- | ---- | ----------- |
| change | Change | Information about the change request including its tasks |
| hostname | String | Hostname of the GSN Portal |
| credential | String | Credentials for the GSN Portal |
| rollback | boolean | Defaults to `false` |
| manualPIR | boolean | Defaults to `true` |
### Example Code
#### Pipeline
```groovy
post {
   always {
        //close Change
        PIRAndClose(change, config.gsn.url, config.gsn.credential, rollback)
    }
}
```
#### Config
```groovy
gsn: [
        templaterfc: 'RFC1283950',
        requestor: 'ab6jg8',
        group: 'GLOBAL-CI.SUPPORT',
        subject: 'CDLib GSN Initial Testing',
        approver: 'ab6jg8',
        url: 'https://servicenow-uat.dhl.com',
        credential: 'gsn_uat'
],
```
#### Result 
```
[Pipeline] stage
[Pipeline] { (Declarative: Post Actions)
[Pipeline] httpRequest
15:27:26  HttpMethod: POST
15:27:26  URL: https://servicenow-uat.dhl.com/change_task.do?hierarchical=true&SOAP
15:27:26  SOAPAction: urn:get
15:27:26  Content-Type: text/xml;charset=UTF-8
15:27:26  Using authentication: gsn_uat
15:27:26  Sending request to url: https://servicenow-uat.dhl.com/change_task.do?hierarchical=true&SOAP
15:27:26  Response Code: HTTP/1.1 200 OK
15:27:26  Response: 
15:27:26  <"Response Data in XML Format">
15:27:26  Success code from [200‥200]
[Pipeline] input
15:27:27  Input requested
15:27:39  Approved by Werner,M.,SNL IT P&P,Abt.4210,DD,External, external
..
.. 2 more Http Request are done successfully ..
..
[Pipeline] echo
15:27:41  ########################################
15:27:41  Closed RFC: RFC1404548
```

## updateChangeRequest
Only for **GSNLib Service**\
Updates a change request data with the information about the result or status of the child task.

### Parameter
| Parameter | Type | Description |
| --------- | ---- | ----------- |
| data | Map | Information about the change request with the status of the last task |
| changeRequestId | String | Change Request Identifier  |
| gsn | GSNWebService | GSNWebService class Object to use |

## updateCtask
Only for **GSNLib Service**\
Update the status or result of a task during the change process.

### Parameter
| Parameter | Type | Description |
| --------- | ---- | ----------- |
| data | Map | Information about the update to a task like state, close code, notes |
| Id | String | Task Identifier |
| gsn | GSNWebService | GSNWebService class Object to use |


## withCdlibCliNamesCreate
This step uses the `cdlib-cli` container to create different important names for your build/deployment.
It ensures a standardized naming schema and speeds up the pipeline creation as the names are needed for other steps.
These names are exported as environment variables. 
These environment variables are prefixed with `CDLIB_`

#### Example
Prerequisite is to have configured the [Kubernetes agent](../confluence/release/CLI.adoc) accordingly.
```groovy
withCdlibCliNamesCreate(containerName: 'cdlib-cli') {
        sh returnStdout: true, script: 'cdlib names create'
}
```

## withGSN
Only for **GSNLib Service**\
Create an object of class `GSNWebService` using the `hostname` and `credential`. Replace the identifier `gsn` with the GSNWebService object created.
Closure to this function is the operation to be performed using the created object to replace `gsn` identifier in the closure.

### Parameter
| Parameter | Type | Description |
| --------- | ---- | ----------- |
| hostname | String | Hostname of the GSN Portal |
| credential | String | Credentials for the GSN Portal |
| body | Closure | The code to delegate the `gsn` identifier mentioned above |
