import dhl.gsn.GSNWebService

def call(Map grpData, String changeRequestId, GSNWebService gsn) {
    if (gsn == null) {
		gsn = new GSNWebService()
	}
		
    grpData.parent = changeRequestId
	  
    def grpAppr = gsn.create('sysapproval_group', grpData)
    
    return grpAppr
}
