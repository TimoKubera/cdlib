package vars

import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import dhl.gsn.GSNWebService

class createCTaskTest extends JenkinsPipelineSpecification {

    Script createCTask = null

    def  setup() {
        createCTask = loadPipelineScriptForTest("vars/createCTask.groovy")
    }

    def "run with proper config parameters"() {

        setup:
            def gsnobj = Mock(GSNWebService)

        when:
            createCTask(data, cRId, ctTId, gsnobj)

        then:
            1 * gsnobj.getById('change_task', ctTId) >> result
            1 * gsnobj.create('change_task', data)

        where:
            data                                                                                                |   cRId    |   ctTId   ||  result
            [short_description: 'Implementation task', assignment_group: '16', opened_by: '20', parent: '24'] |   '24'    |   '25'    ||  ['sys_id' : '24']

    }

}
