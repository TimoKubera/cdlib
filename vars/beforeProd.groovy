import dhl.gsn.Change

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

//def call(Map config) {
//	call(config.templateNumber, config.requestor, config.)
//}



ZonedDateTime getUTCzonedDateTime() {
	return ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)
}


def call(String templateNumber = 'RFC1268671', String requestor, String workGroup, String description, String approver, String hostname, String credential, String descriptionDetail=null) {
	if (requestor == approver) {
		currentBuild.result = 'FAILED'
		error '''Requestor and approver cannot be the same person, please change it. Maybe requestor can be the developer 
		who made the change and approver whoever triggers the deployment. You can get the last commiter id using the createNames
		step https://git.dhl.com/CDLib/CDlib/blob/8ff19fb0832d8d4a4fc4c16d4c1b8c6e3b57f3d1/vars/createNames.groovy#L196'''
	}
	withGSN(hostname, credential) {
		def author = gsn.find('sys_user', [user_name: requestor])
		def groupMap = gsn.find('sys_user_group', [name: workGroup])
		def bimodalGroup = gsn.find('sys_user_group', [name: 'CAB-BIMODAL'])
		def approverMap = gsn.find('sys_user', [user_name: approver])
		echo "GSN DEBUG FIND START"
		def template = gsn.getByNumber('change_request', templateNumber)
		echo "GSN DEBUG FIND END"
		def implTemplateTask = gsn.find('change_task', [u_task_type: 'implementation', parent: template.sys_id])
		def testTemplateTask = gsn.find('change_task', [u_task_type: 'uat', parent: template.sys_id])
		def pirTemplateTask = gsn.find('change_task', [u_task_type: 'pir', parent: template.sys_id])
		
		def data = [:]
		
		data.assignment_group = groupMap.sys_id  
		data.u_requested_by = author.sys_id
		def currentDate = getUTCzonedDateTime().format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))
		data.start_date = currentDate
		data.end_date = getUTCzonedDateTime().plusDays(1).format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))
		data.u_customer_rtp_date = getUTCzonedDateTime().plusDays(1).format(DateTimeFormatter.ofPattern('yyyy-MM-dd'))
		
		data.u_backout_groups = groupMap.sys_id 
		data.u_backout_authority = author.sys_id
		data.short_description = description
		data.description = descriptionDetail ?: getChangeLogs()

		
		def newChangeRequest = createNewChangeRequest(data, templateNumber, gsn)
			   
		def groupApprovalData = [:]
		
		groupApprovalData.assignment_group = bimodalGroup.sys_id
		groupApprovalData.sysapproval_approver = [:]
		groupApprovalData.sysapproval_approver.approver = approverMap.sys_id

		def approval = createGroupApproval(groupApprovalData, newChangeRequest.sys_id, gsn)
				
		def ctaskImplData = [:]
		ctaskImplData.short_description = 'Implementation task'
		ctaskImplData.assignment_group = groupMap.sys_id 
		ctaskImplData.work_start = currentDate
		ctaskImplData.work_end = getUTCzonedDateTime().plusDays(1).format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))
		ctaskImplData.opened_by = author.sys_id
		ctaskImplData.active = true
			   
		def implCTask = createCTask(ctaskImplData, newChangeRequest.sys_id, implTemplateTask.sys_id, gsn)
		
		def ctaskTestData = [:]
		ctaskTestData.short_description = 'Test task'
		ctaskTestData.assignment_group = groupMap.sys_id 
		ctaskTestData.work_start = currentDate
		ctaskTestData.work_end = getUTCzonedDateTime().plusDays(1).format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))
		ctaskTestData.opened_by = author.sys_id
			   
		def testCTask = createCTask(ctaskTestData, newChangeRequest.sys_id, testTemplateTask.sys_id, gsn)
		
		def ctaskPirData = [:]
		ctaskPirData.assignment_group = groupMap.sys_id 		
		ctaskPirData.opened_by = author.sys_id
			   
		def pirCTask = createCTask(ctaskPirData, newChangeRequest.sys_id, pirTemplateTask.sys_id, gsn)
		
		// Change state to registered
		def changeRequestChanges = [:]
		changeRequestChanges.state = 20
	   		 
		updateChangeRequest(changeRequestChanges, newChangeRequest.sys_id, gsn)
		
		// Change state to To be Approved for implementation
		changeRequestChanges.state = 60

		updateChangeRequest(changeRequestChanges, newChangeRequest.sys_id, gsn)
				
		// Change state to approved
		updateCtask([state: 110, u_close_code: 'implemented',  close_notes: 'Done'], testCTask.sys_id, gsn)
		approveChangeRequest(approval.sys_id, approverMap.sys_id, gsn)
		changeRequestChanges.state = 70
		
		updateChangeRequest(changeRequestChanges, newChangeRequest.sys_id, gsn)
				
		def changeBean = new Change(newChangeRequest, implCTask, testCTask, pirCTask)

		echo """\
				########################################
				Created RFC: ${changeBean.changeRequestNumber}""".stripIndent()
		
		return changeBean
	}
}
