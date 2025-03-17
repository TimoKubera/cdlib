/*
* This script is intended to be used along with authentication/OfflineTokenRefresher.js to
* handle an OAUTH2 offline token refresh workflow.
*
* authentication/OfflineTokenRefresher.js will automatically fetch the new access token for every unauthorized
* request determined by the "Logged Out" or "Logged In" indicator previously set in Context -> Authentication.
*
*  httpsender/AddBearerTokenHeader.js will add the new access token to all requests in scope
* made by ZAP (except the authentication ones) as an "Authorization: Bearer [access_token]" HTTP Header.
*
* @author Laura Pardo <lpardo at redhat.com>
*/

var HttpSender = Java.type('org.parosproxy.paros.network.HttpSender');
var ScriptVars = Java.type('org.zaproxy.zap.extension.script.ScriptVars');

function sendingRequest(msg, initiator, helper) {

    var access_token=ScriptVars.getGlobalVar("access_token")

    // add Authorization header to all request in scope except the authorization request itself
    if (initiator !== HttpSender.AUTHENTICATION_INITIATOR && msg.isInScope()) {
		// remove header first to allow changing the header value
        msg.getRequestHeader().setHeader("Authorization", null);
        msg.getRequestHeader().setHeader("Authorization", "Bearer " + access_token);       
    }

    return msg;
}

function responseReceived(msg, initiator, helper) {}