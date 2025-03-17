import dhl.gsn.GSNWebService

def call(String hostname, String credential, Closure body) {
    def gsn = new GSNWebService(this)
	gsn.hostname = hostname
	gsn.credential = credential
            
    body.getDelegate().gsn = gsn
    body()   
}
