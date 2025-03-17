/*
* This script is intended to be used along with  httpsender/AddBearerTokenHeader.js  to
* handle an OAUTH2 offline token refresh workflow.
*
* authentication/OfflineTokenRefresher.js will automatically fetch the new access token for every unauthorized
* request determined by the "Logged Out" or "Logged In" indicator previously set in Context -> Authentication.
*
*  httpsender/AddBearerTokenHeader.js  will add the new access token to all requests in scope
* made by ZAP (except the authentication ones) as an "Authorization: Bearer [access_token]" HTTP Header.
*
* @author Laura Pardo <lpardo at redhat.com>
*/

var HttpRequestHeader = Java.type("org.parosproxy.paros.network.HttpRequestHeader");
var HttpHeader = Java.type("org.parosproxy.paros.network.HttpHeader");
var URI = Java.type("org.apache.commons.httpclient.URI");
var ScriptVars = Java.type('org.zaproxy.zap.extension.script.ScriptVars');

function authenticate(helper, paramsValues, credentials) {

    var tokenMsg = helper.prepareMessage();

    var current_access_token = ScriptVars.getGlobalVar("access_token");

    if (!current_access_token) {
        var token_endpoint = paramsValues.get("token_endpoint");
        var client_id = paramsValues.get("client_id");
        var username = credentials.getParam("username"); // paramsValues.get("username");
        var password = credentials.getParam("password"); // paramsValues.get("password");

        // Build body
        var getTokenBody = "client_id=" + client_id;
        getTokenBody+= "&grant_type=password&scope=openid";
        getTokenBody+= "&username=" + username;
        getTokenBody+= "&password=" + password;

        // Build header
        var tokenRequestURI = new URI(token_endpoint, false);
        var tokenRequestMethod = HttpRequestHeader.POST;
        var tokenRequestMainHeader = new HttpRequestHeader(tokenRequestMethod, tokenRequestURI, HttpHeader.HTTP11);
        tokenRequestMainHeader.setHeader("Content-Type","application/x-www-form-urlencoded");

        // Build message
        tokenMsg.setRequestBody(getTokenBody);
        tokenMsg.setRequestHeader(tokenRequestMainHeader);
        tokenMsg.getRequestHeader().setContentLength(tokenMsg.getRequestBody().length());

        // Make the request and receive the response
        helper.sendAndReceive(tokenMsg, false);

    } else {

        var current_refresh_token = ScriptVars.getGlobalVar("refresh_token");
        print("## current_access_token is present, so we have to do a refresh with current_refresh_token="+current_refresh_token);

        var token_endpoint = paramsValues.get("token_endpoint");
        var client_id = paramsValues.get("client_id");

        // Build body
        var refreshTokenBody = "client_id=" + client_id;
        refreshTokenBody+= "&grant_type=refresh_token";
        refreshTokenBody+= "&refresh_token=" + current_refresh_token;

        // Build header
        var tokenRequestURI = new URI(token_endpoint, false);
        var tokenRequestMethod = HttpRequestHeader.POST;
        var tokenRequestMainHeader = new HttpRequestHeader(tokenRequestMethod, tokenRequestURI, HttpHeader.HTTP11);
        tokenRequestMainHeader.setHeader("Content-Type","application/x-www-form-urlencoded");

        // Build message
        tokenMsg.setRequestBody(refreshTokenBody);
        tokenMsg.setRequestHeader(tokenRequestMainHeader);
        tokenMsg.getRequestHeader().setContentLength(tokenMsg.getRequestBody().length());
    }

    // Make the request and receive the response
    helper.sendAndReceive(tokenMsg, false);
    var statusCode=tokenMsg.getResponseHeader().getStatusCode()

    // Parse the JSON response and save the new access_token in a global var
    // we will replace the Authentication header in AddBearerTokenHeader.js
    var json = JSON.parse(tokenMsg.getResponseBody().toString());
    var access_token = json['access_token'];

    if (access_token) {
        ScriptVars.setGlobalVar("access_token", access_token);
    } else {
        print("Error getting access token")
        ScriptVars.setGlobalVar("access_token", null);
    }

    var refresh_token = json['refresh_token'];

    if (refresh_token) {
        ScriptVars.setGlobalVar("refresh_token", refresh_token);
    } else {
        print("Error getting refresh token")
        ScriptVars.setGlobalVar("refresh_token", null);
    }

    return tokenMsg;
}


function getRequiredParamsNames(){
    return ["token_endpoint", "client_id"];
}


function getOptionalParamsNames(){
    return [];
}


function getCredentialsParamsNames(){
    return ["username", "password"];
}