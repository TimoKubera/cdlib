package vars

import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import dhl.gsn.GSNWebService

class approveChangeRequestTest extends JenkinsPipelineSpecification {

    Script approveChangeRequest = null

    def  setup() {
        approveChangeRequest = loadPipelineScriptForTest("vars/approveChangeRequest.groovy")
    }

    def "run with proper config parameters"() {

        setup:
            def gsnobj = Mock(GSNWebService)

        when:
            approveChangeRequest(groupAI, approver, gsnobj)

        then:
            1 * gsnobj.find('sysapproval_approver', searchP) >> result
            1 * gsnobj.update('sysapproval_approver', data, id)

        where:
            groupAI |   approver    ||  searchP                             |   result              |   data                |   id
            '25'    |   'sys_id'    ||  [group: '25', approver: 'sys_id']   |   ['sys_id' : '24']   |   [state: 'approved'] |   '24'

    }

}
