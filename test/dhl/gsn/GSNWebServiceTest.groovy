package dhl.gsn

import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import dhl.gsn.GSNWebService
import jenkins.plugins.http_request.ResponseContentSupplier

class GSNWebServiceTest extends JenkinsPipelineSpecification {

    def "convertToMap test"() {
        setup:
            def gsnwsTest = new GSNWebService()
            def parserTest = new XmlSlurper()

        expect:
            result == gsnwsTest.convertToMap(parserTest.parseText(xml).Body)

        where:
            xml                                                                                                                                                                                 ||  result
            '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body xmlns:ns0="prefix"><ns0:Person_ID>PPL000000301739</ns0:Person_ID></soap:Body></soap:Envelope>'    ||  [ 'Person_ID':'PPL000000301739' ]

    }

    def "callService test"() {

        setup:
            def gsnwsTest = new GSNWebService(getPipelineMock("CpsScript"))

        when:
            result = gsnwsTest.callService(_ as String, _ as String, _ as String)

        then:
            1 * getPipelineMock( 'httpRequest' ) (_) >> new ResponseContentSupplier( xml, 200 )

        where:
            xml                                                                                                                                                                                 ||  result
            '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body xmlns:ns0="prefix"><ns0:Person_ID>PPL000000301739</ns0:Person_ID></soap:Body></soap:Envelope>'    ||  '<ns0:Person_ID>PPL000000301739</ns0:Person_ID>'
    }

    def "getById test"() {

        setup:
            def gsnwsTest = new GSNWebService(getPipelineMock("CpsScript"))

        when:
            result = gsnwsTest.getById(_ as String, _ as String)

        then:
            1 * getPipelineMock( 'httpRequest' ) (_) >> new ResponseContentSupplier( xml, 200 )

        where:
            xml                                                                                                                                                                                 ||  result
            '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body xmlns:ns0="prefix"><ns0:Person_ID>PPL000000301739</ns0:Person_ID></soap:Body></soap:Envelope>'    ||  [ 'Person_ID':'PPL000000301739' ]

    }

    def "buildGetByIdRequest test"() {

        setup:
            def gsnwsTest = new GSNWebService(getPipelineMock("CpsScript"))

        expect:
            result == gsnwsTest.buildGetByIdRequest(tableName, id)

        where:
            tableName   |   id      ||  result
            'tab1'      |   '25'    ||  '<?xml version=\'1.0\' encoding=\'utf-8\'?>\n<soapenv:Envelope xmlns:soapenv=\'http://schemas.xmlsoap.org/soap/envelope/\' xmlns:ns=\'http://www.service-now.com/tab1\'>\n  <soapenv:Header />\n  <soapenv:Body>\n    <ns:get>\n      <sys_id>25</sys_id>\n    </ns:get>\n  </soapenv:Body>\n</soapenv:Envelope>'

    }

    def "getByNumber test"() {

        setup:
            def gsnwsTest = new GSNWebService(getPipelineMock("CpsScript"))
            explicitlyMockPipelineVariable("mkp")

        when:
            result = gsnwsTest.getByNumber(_ as String, _ as String)

        then:
            1 * getPipelineMock( 'httpRequest' ) (_) >> new ResponseContentSupplier( xml, 200 )

        where:
            xml                                                                                                                                                                                 ||  result
            '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body xmlns:ns0="prefix"><ns0:Person_ID>PPL000000301739</ns0:Person_ID></soap:Body></soap:Envelope>'    ||  [ 'Person_ID':'PPL000000301739' ]

    }

    def "create test"() {

        setup:
            def gsnwsTest = new GSNWebService(getPipelineMock("CpsScript"))
            explicitlyMockPipelineVariable("mkp")

        when:
            result = gsnwsTest.create(_ as String, var as Map)

        then:
            1 * getPipelineMock( 'httpRequest' ) (_) >> new ResponseContentSupplier( xml, 200 )

        where:
            var                 |   xml                                                                                                                                                                                 ||  result
            ['sys_id' : '24']   |   '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body xmlns:ns0="prefix"><ns0:Person_ID>PPL000000301739</ns0:Person_ID></soap:Body></soap:Envelope>'    ||  [ 'Person_ID':'PPL000000301739' ]

    }

    def "buildCreateRequest test"() {

        setup:
            def gsnwsTest = new GSNWebService(getPipelineMock("CpsScript"))
            explicitlyMockPipelineVariable("mkp")

        expect:
            result == gsnwsTest.buildCreateRequest(table, data)

        where:
            table   |   data                ||  result
            'tab1'  |   ['sys_id' : '24']   ||  '<?xml version=\'1.0\' encoding=\'utf-8\'?>\n<soapenv:Envelope xmlns:soapenv=\'http://schemas.xmlsoap.org/soap/envelope/\' xmlns:ns=\'http://www.service-now.com/tab1\'>\n  <soapenv:Header />\n  <soapenv:Body>\n    <ns:insert>\n      <sys_id />\n    </ns:insert>\n  </soapenv:Body>\n</soapenv:Envelope>'

    }

    def "update test"() {

        setup:
            def gsnwsTest = new GSNWebService(getPipelineMock("CpsScript"))
            explicitlyMockPipelineVariable("mkp")

        when:
            result = gsnwsTest.update(_ as String, var as Map, _ as String)

        then:
            1 * getPipelineMock( 'httpRequest' ) (_) >> new ResponseContentSupplier( xml, 200 )

        where:
            var                 |   xml                                                                                                                                                                                 ||  result
            ['sys_id' : '24']   |   '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body xmlns:ns0="prefix"><ns0:Person_ID>PPL000000301739</ns0:Person_ID></soap:Body></soap:Envelope>'    ||  [ 'Person_ID':'PPL000000301739' ]

    }

    def "buildUpdateRequest test"() {

        setup:
            def gsnwsTest = new GSNWebService(getPipelineMock("CpsScript"))
            explicitlyMockPipelineVariable("mkp")

        expect:
            result == gsnwsTest.buildUpdateRequest(table, id, data)

        where:
            table   |   id      |   data                ||  result
            'tab1'  |   '27'    |   ['sys_id' : '24']   ||  '<?xml version=\'1.0\' encoding=\'utf-8\'?>\n<soapenv:Envelope xmlns:soapenv=\'http://schemas.xmlsoap.org/soap/envelope/\' xmlns:ns=\'http://www.service-now.com/tab1\'>\n  <soapenv:Header />\n  <soapenv:Body>\n    <ns:update>\n      <sys_id>27</sys_id>\n      <sys_id />\n    </ns:update>\n  </soapenv:Body>\n</soapenv:Envelope>'

    }

    def "find test"() {

        setup:
            def gsnwsTest = new GSNWebService(getPipelineMock("CpsScript"))
            explicitlyMockPipelineVariable("mkp")

        when:
            result = gsnwsTest.find(_ as String, var as Map)

        then:
            1 * getPipelineMock( 'httpRequest' ) (_) >> new ResponseContentSupplier( xml, 200 )

        where:
            var                 |   xml                                                                                                                                                                                 ||  result
            ['sys_id' : '24']   |   '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body xmlns:ns0="prefix"><ns0:Person_ID>PPL000000301739</ns0:Person_ID></soap:Body></soap:Envelope>'    ||  [ 'Person_ID':'PPL000000301739' ]

    }

    def "buildFindRequest test"() {

        setup:
            def gsnwsTest = new GSNWebService(getPipelineMock("CpsScript"))
            explicitlyMockPipelineVariable("mkp")

        expect:
            result == gsnwsTest.buildFindRequest(table, data)

        where:
            table   |   data                ||  result
            'tab1'  |   ['sys_id' : '24']   ||  '<?xml version=\'1.0\' encoding=\'utf-8\'?>\n<soapenv:Envelope xmlns:soapenv=\'http://schemas.xmlsoap.org/soap/envelope/\' xmlns:ns=\'http://www.service-now.com/tab1\'>\n  <soapenv:Header />\n  <soapenv:Body>\n    <ns:getRecords>\n      <sys_id />\n    </ns:getRecords>\n  </soapenv:Body>\n</soapenv:Envelope>'

    }

}
