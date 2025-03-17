import dhl.gsn.GSNWebService

def call(Map data, String changeRequestId, String ctaskTemplateId, GSNWebService gsn) {
    if (gsn == null) {
		gsn = new GSNWebService()
	}
	
	if (ctaskTemplateId != null) {
		def ctaskTemplate = gsn.getById('change_task', ctaskTemplateId)
		ctaskTemplate << data
		data = ctaskTemplate
		data.remove('sys_id')
		data.remove('number')
		data.remove('change_request')
	}
		
    data.parent = changeRequestId
    
    return gsn.create('change_task', data)
}
