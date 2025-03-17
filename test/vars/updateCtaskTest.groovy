package vars

import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import dhl.gsn.GSNWebService

class updateCtaskTest extends JenkinsPipelineSpecification {

    Script updateCtask = null

    def  setup() {
        updateCtask = loadPipelineScriptForTest("vars/updateCtask.groovy")
    }

    def "run with proper config parameters"() {

        setup:
            def gsnobj = Mock(GSNWebService)

        when:
            updateCtask(data, id, gsnobj)

        then:
            1 * gsnobj.update('change_task', data, id)

        where:
            data                                                            |   id
            [state: 110, u_close_code: 'implemented',  close_notes: 'Done'] |   '24'

        }

}
