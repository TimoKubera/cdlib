package var

import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import dhl.gsn.GSNWebService

class createGroupApprovalTest extends JenkinsPipelineSpecification {

    Script createGroupApproval = null

    def  setup() {
        createGroupApproval = loadPipelineScriptForTest("vars/createGroupApproval.groovy")
    }

    def "run with proper config parameters"() {

        setup:
            def gsnobj = Mock(GSNWebService)

        when:
            createGroupApproval(data, chgRId, gsnobj)

        then:
            1 * gsnobj.create('sysapproval_group', cdata)

        where:
            data                                                                |   chgRId  ||  cdata
            [assignment_group: '25', sysapproval_approver: [approver: '25']]    |   '24'    ||  [assignment_group: '25', sysapproval_approver: [approver: '25'], parent: '24']

    }

}