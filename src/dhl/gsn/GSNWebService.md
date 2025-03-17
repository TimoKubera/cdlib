# GSNWebService
GSNWebService is a class used for web service connection to interact with ServiceNow Portal.
The web service uses SOAP XML based interactions for querying and response.

## Configuration
| Parameter | Default Value | Example | Description |
| --------- | ------------- | ------- | ----------- |
| hostname | https://servicenow-uat.dhl.com | https://servicenow-uat.dhl.com | URL of the remote ServiceNow Portal |
| credentials | gsn_sa | gsn-usr | User credentials used to connect with the remote ServiceNow Portal |

## Methods  
| Name | Parameter | Description |
| ---- | --------- | ----------- |
| convertToMap | String nodes | Convert the XML DOM 'nodes' in String format to Map |
| callService | String request, String action, String tableName | Initiates a httpRequest SOAP-XML request for the action on a particular table, parses the response and returns the body of the response |
| getById | String tableName, String id | Gets information about a request 'id' from a 'tableName', uses 'buildGetByIdRequest', 'callService' and 'convertToMap' functions |
| buildGetByIdRequest | String tableName, String id | Creates a XML Markup request body based on the 'tableName' and 'id' and adds the SOAP envelope using 'createSoapEnv' function |
| getByNumber | String tableName, String number | Gets information about a request 'number' from a 'tableName', uses 'find' function |
| create | String tableName, Map data | Create a change request based on 'data' in 'tableName', uses 'buildCreateRequest', 'callService' and 'convertToMap' functions |
| buildCreateRequest | String tableName, Map data | Creates a XML Markup request body based on 'tableName' and 'data' and adds the SOAP envelope using 'createSoapEnv' function |
| update | String tableName, Map data, String id | Updates a change request for 'id' based on 'data' in 'tableName', uses 'buildUpdateRequest', 'callService' and 'convertToMap' functions |
| buildUpdateRequest | String tableName, String id, Map data | Creates a XML Markup request body based on 'tableName', 'id' and 'data' and adds the SOAP envelope using 'createSoapEnv' function |
| createSoapEnv | String tableName, Closure cl | Envelopes the XML Markup body 'cl' into a SOAP XML request for 'tableName' including the request format, encoding and standards |
| find | String tableName, Map parameters | Gets information regarding 'parameters' from a 'tableName', uses 'buildFindRequest', 'callService' and 'convertToMap' functions |
| buildFindRequest | String tableName, Map params | Creates a XML Markup request body based on 'tableName' and 'params' and adds the SOAP envelope using 'createSoapEnv' function |
