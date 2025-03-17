package dhl.gsn

import groovy.xml.MarkupBuilder

class GSNWebService implements Serializable {
	
    def steps
    GSNWebService(steps) {this.steps = steps}	
        
    def hostname = 'https://servicenow-uat.dhl.com'
    def credential = 'gsn_sa'

    /*
    * 
    ******************************************************************************/
    def convertToMap(nodes) {
        nodes.children().collectEntries {
            [ it.name(), it.childNodes() ? convertToMap(it) : it.text() ]
        }
    }

    /*
    * 
    ******************************************************************************/
    def callService(request, action, tableName) {    
        def response = steps.httpRequest consoleLogResponseBody: true, customHeaders: [[maskValue: false, name: 'SOAPAction', value: "urn:${action}"], [maskValue: false, name: 'Content-Type', value: 'text/xml;charset=UTF-8']],
        authentication: credential, httpMode: 'POST',  requestBody: request, url: "${hostname}/${tableName}.do?hierarchical=true&SOAP", validResponseCodes: '200,201', responseHandle: 'LEAVE_OPEN'
        def parsedResp = new XmlSlurper().parseText(response.getContent()).declareNamespace(soap:'http://schemas.xmlsoap.org/soap/envelope/')
        response.close()

        return parsedResp.'soap:Body'
    }

    /*
    * 
    ******************************************************************************/
    def getById(String tableName, String id) {
        def request = buildGetByIdRequest(tableName, id)
        
        return convertToMap(callService(request, 'get', tableName).getResponse)
    }

    /*
    * 
    ******************************************************************************/
    @com.cloudbees.groovy.cps.NonCPS
    def buildGetByIdRequest(String tableName, String id) {
        def body = {MarkupBuilder builder ->
            builder.'ns:get'() {
                builder.sys_id(id)
            }
        }
        def request = createSoapEnv(tableName, body)
        
        return request
    }

    /*
    * 
    ******************************************************************************/
    def getByNumber(String tableName, String number) {
        def params = [:]
        params.number = number
        
        return find(tableName, params)
    }

    /*
    * 
    ******************************************************************************/
    def create(String tableName, Map data) {
        def request = buildCreateRequest(tableName, data)
        
        return convertToMap(callService(request, 'insert', tableName).insertResponse)
    }

    /*
    * 
    ******************************************************************************/
    @com.cloudbees.groovy.cps.NonCPS
    def buildCreateRequest(String tableName, Map data) {
        def body = {MarkupBuilder builder ->
            builder.'ns:insert'() {
                
                data.collect { k, v ->
                    builder."$k" { v instanceof Map ? v.collect(owner) : mkp.yield(v) }
                }
            }
        }
    
        def request = createSoapEnv(tableName, body)
     
        return request
    }

    /*
    * 
    ******************************************************************************/
    def update(String tableName, Map data, String id) {
        def request = buildUpdateRequest(tableName, id, data)
        
        return convertToMap(callService(request, 'update', tableName).updateResponse)
    }

    /*
    * 
    ******************************************************************************/
    @com.cloudbees.groovy.cps.NonCPS
    def buildUpdateRequest(String tableName, String id, Map data) {
        def body = {MarkupBuilder builder ->
            builder.'ns:update'() {
                builder.sys_id( id)
                data.collect { k, v ->
                    builder."$k" { v instanceof Map ? v.collect(owner) : mkp.yield(v) }
                }
            }
        }
    
        def request = createSoapEnv(tableName, body)
    
        return request
    }

    /*
    * 
    ******************************************************************************/
    @com.cloudbees.groovy.cps.NonCPS
    def createSoapEnv(String tableName, Closure cl) {
        def xmlWriter = new StringWriter()
        def xmlMarkup = new MarkupBuilder(xmlWriter)
        xmlMarkup.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
        xmlMarkup
            .'soapenv:Envelope'('xmlns:soapenv' : 'http://schemas.xmlsoap.org/soap/envelope/', 'xmlns:ns' : "http://www.service-now.com/${tableName}") {
                delegate.'soapenv:Header'() 
                delegate.'soapenv:Body'() {
                    
                    cl(xmlMarkup)
                }
            }
        
        return xmlWriter.toString()
    }

    /*
    * 
    ******************************************************************************/
    def find(String tableName, Map parameters) {
        def request = buildFindRequest(tableName, parameters)
        
        return convertToMap(callService(request, 'getRecords', tableName).getRecordsResponse.getRecordsResult)
    }

    /*
    * 
    ******************************************************************************/
    @com.cloudbees.groovy.cps.NonCPS
    def buildFindRequest(String tableName, Map params) {
        def body = {MarkupBuilder builder ->
            builder.'ns:getRecords'() {
                params.collect { k, v ->
                    builder."$k" { v instanceof Map ? v.collect(owner) : mkp.yield(v) }
                }
            }
        }
        def request = createSoapEnv(tableName, body)
    
        return request
    }
}
