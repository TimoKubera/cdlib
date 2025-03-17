import dhl.gsn.GSNWebService

def call(Map data, String templateNumber = 'RFC1268671', GSNWebService gsn) {
	
	if (gsn == null) {
		gsn = new GSNWebService()
	}
    
	echo "DEBUG GSN TEMPLATE START"
	
    def template = gsn.getByNumber('change_request', templateNumber)
	
	echo "DEBUG GSN TEMPLATE END"
	def templateId = template.sys_id
    
    template.state=10
	template.u_from_template = template.sys_id
    template.remove('sys_id')
    template.remove('number')
    template.remove('opened_at')
    template.remove('opened_by')
    template.remove('u_change_template')
    template.remove('sys_created_by')
    template.remove('sys_created_on')
	template.remove('u_requested_by')
    template.active = 'true'
    
    
    template << data
    
    newRfc = gsn.create('change_request', template)
    def newRfcFull = gsn.getById('change_request', newRfc.sys_id)
	
	def impactedCIId = newRfcFull.u_service_outage
	def updateImpactedServiceData = [:]
	updateImpactedServiceData.u_outage_type = 'none'
	
	gsn.update('task_cmdb_ci_service', updateImpactedServiceData, impactedCIId)
	
	// add link with master RFC. 
	def templateLink = gsn.find('task_rel_task', [child: templateId])
	if (templateLink != null && templateLink.parent != null) {
		def masterRFC = templateLink.parent
		gsn.create('task_rel_task', [parent: masterRFC, child: newRfc.sys_id, type: 'fa7f9e371c97e800b1509f804a627912'])
	}
     
	return newRfcFull
}
