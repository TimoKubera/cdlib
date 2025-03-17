package dhl.gsn

class Change implements Serializable {
    final def changeRequestData
	final def implTask
	final def testTask
	final def pirTask
	final String changeRequestNumber

	Change(changeRequestData, implTask, testTask, pirTask) {
		this.changeRequestData = changeRequestData
		this.implTask = implTask
		this.testTask = testTask
		this.pirTask = pirTask
		changeRequestNumber = changeRequestData.number
	}
}
