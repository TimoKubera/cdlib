import dhl.gsn.GSNWebService

def call(String groupApprovalId, String approver, GSNWebService gsn) {
    if (gsn == null) {
		gsn = new GSNWebService()
	}

    def searchParams = [:]
    searchParams.group = groupApprovalId
    searchParams.approver = approver
    
    def approval = gsn.find('sysapproval_approver', searchParams) 
    def data = [:]

    data.state = 'approved'
    gsn.update('sysapproval_approver', data, approval.sys_id)
}
