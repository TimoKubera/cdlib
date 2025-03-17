import dhl.gsn.Change
import hudson.model.ChoiceParameterDefinition
import hudson.model.TextParameterDefinition

def call(Change change, String hostname, String credential, boolean rollback = false, boolean manualPIR = true) {
	withGSN(hostname, credential) {
		final String lessonLearnedDefault = ''
		final String preventiveMeasuresDefault = ''
		final String failedChangeCauseByDefault = 'other'
		final String failureCodeDefault = 'Not impacting business – failed to install correctly'

		def changeRequestChanges = [:]
		def pirTask = gsn.getById('change_task', change.pirTask.sys_id)

		//User has to fill in PIR
		final String defaultText =  pirTask.description
		List<String> parameters = []

		if (rollback) {
			parameters << new ChoiceParameterDefinition('Failure_code', ['Impacting business – Design/Architecture bug',
			'Not impacting business – Design/Architecture bug',
			'Impacting business – failed to install correctly',
			'Not impacting business – failed to install correctly',
			'Impacting business – Feature/software bug in solution detected',
			'Not impacting business – Feature/software bug in solution detected',
			'Impacting business – Human mistake',
			'Not impacting business – Human mistake',
			'Impacting business – Infrastructure issue (Bug)',
			'Not impacting business – Infrastructure issue (Bug)',
			'Impacting business – Infrastructure issue (hardware fail)',
			'Not impacting business – Infrastructure issue (hardware fail)',
			'Impacting business – Performance Issues/Scalability',
			'Not impacting business – Performance Issues/Scalability',
			'Impacting business – Requirement error bug',
			'Not impacting business – Requirement error bug',
			'Impacting business – RTP plan bug',
			'Not impacting business – RTP plan bug',
			'Impacting business – Security bug',
			'Not impacting business – Security bug'] as String[], '')

			parameters << new ChoiceParameterDefinition('Failed_change_caused_by', ['pds',
			'sd',
			'other',
			'buit'] as String[], '')

			parameters << new TextParameterDefinition('lesson_learned', lessonLearnedDefault, '')
			parameters << new TextParameterDefinition('preventive_measures', preventiveMeasuresDefault, '')
		} 


		def answers

		if (manualPIR) {
			parameters << new TextParameterDefinition('PIR', defaultText, '')
			answers = input message: 'Please fill in the PIR', parameters: parameters, submitterParameter: 'submitter'
		}
		else {
			answers = [:]

			answers.PIR = defaultText
			//TODO get real submitter
			answers.submitter = 'omh9ote011'

			answers.lesson_learned = lessonLearnedDefault
			answers.preventive_measures = preventiveMeasuresDefault

			answers.Failure_code = failureCodeDefault
			answers.Failed_change_caused_by = failedChangeCauseByDefault
		}
		
		
		// Update PIR task
		def pirTaskNewValues = [state: 110, u_close_code: rollback ? 'failed' : 'implemented', close_notes: answers.PIR, assigned_to: answers.submitter]
		if (rollback) {
			pirTaskNewValues << [u_lesson_learned: answers.lesson_learned, u_preventive_measures: answers.preventive_measures]
		}
		updateCtask(pirTaskNewValues, change.pirTask.sys_id, gsn)
		 
		 // Move change to closed
		if (!rollback) {
			changeRequestChanges.u_close_code = 'implemented'
			changeRequestChanges.u_process_policy = 'authorized'
		} else {
			changeRequestChanges.u_close_code = 'failed'
			changeRequestChanges.u_process_policy = 'authorized'
			changeRequestChanges.u_failure_code = answers.Failure_code
			changeRequestChanges.u_failed_change_caused_by = answers.Failed_change_caused_by
		}
		
		changeRequestChanges.state = 110
			
		 updateChangeRequest(changeRequestChanges, change.changeRequestData.sys_id, gsn)

		echo """\
				########################################
				Closed RFC: ${change.changeRequestNumber}""".stripIndent()
	}
	
}
