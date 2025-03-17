import dhl.gsn.Change

def call(Change change, String hostname, String credential) {
	withGSN(hostname, credential) {
		def changeRequestChanges = [:]
		
		// Move change to implemented
		updateCtask([state: 110, u_close_code: 'implemented', close_notes: 'Done'], change.implTask.sys_id, gsn)		
		changeRequestChanges.state = 80		
		 updateChangeRequest(changeRequestChanges, change.changeRequestData.sys_id, gsn)

		echo """\
				########################################
				Implemented RFC: ${change.changeRequestNumber}""".stripIndent()
	}	
}
