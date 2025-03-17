package vars

import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import dhl.gsn.GSNWebService

class updateChangeRequestTest extends JenkinsPipelineSpecification {

    Script updateChangeRequest = null

    def  setup() {
        updateChangeRequest = loadPipelineScriptForTest("vars/updateChangeRequest.groovy")
    }

    def "run with proper config parameters"() {

        setup:
            def gsnobj = Mock(GSNWebService)

        when:
            updateChangeRequest(data, cRId, gsnobj)

        then:
            1 * gsnobj.update('change_request', data, cRId)

        where:
            data        |   cRId
            [state: 20] |   '24'

        }

}
