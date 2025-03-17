import dhl.gsn.GSNWebService

def call(Map data, String id, GSNWebService gsn ) {
    if (gsn == null) {
		gsn = new GSNWebService()
	}
    
    gsn.update('change_task', data, id)
}
