import com.homeaway.devtools.jenkins.testing.JenkinsPipelineSpecification
import dhl.gsn.GSNWebService

class createNewChangeRequestTest extends JenkinsPipelineSpecification {

    Script createNewChangeRequest = null

    def  setup() {
        createNewChangeRequest = loadPipelineScriptForTest("vars/createNewChangeRequest.groovy")
    }

    def "run with proper config parameters"(data, tempN, result, cresult, gresult) {

        setup:
            def gsnobj = Mock(GSNWebService)

        when:
            createNewChangeRequest(data, tempN, gsnobj)

        then:
            1 * gsnobj.getByNumber('change_request', tempN) >> result
            1 * gsnobj.create('change_request', _ as Map) >> cresult
            1 * gsnobj.getById('change_request', _ as String) >> gresult
            1 * gsnobj.update('task_cmdb_ci_service', [u_outage_type: 'none'], _ as String)
            1 * gsnobj.find('task_rel_task', [child: result.sys_id]) >> result
            1 * gsnobj.create('task_rel_task', _ as Map) >> cresult

        where:
            data            |   tempN       ||  result                              |   cresult         |   gresult
            [sys_id: '25']  |   'RFC023'    ||  ['sys_id': '25', 'parent': '24']    |   [sys_id: '25']  |   [sys_id: '25', 'u_service_outage': '64']

        }

}
