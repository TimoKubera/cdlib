# README

## HowTo

It should suffice to make the scripts available in your pipeline and load them in ZAP.
You will need to provide a `token_endpoint` as parameter and working credentials with the correct names (you can see which at the bottom of the files) to the auth scripts.

The HTTP sender script will check if the token exists, and if it does, use it to authenticate.
If the response aligns with your `logged_out` condition, ZAP will attempt to generate a new token using the authentication script.

## With automation framework
As always it is recommended to generate the plan in the UI,
export and adapt it to your pipeline.

````yaml
  - type: script
    parameters:
      action: "add"
      type: "httpsender"
      engine: "ECMAScript : Oracle Nashorn"
      name:                      # String: The name of the script, defaults to the file name
      file: "AddBearerTokenHeader.js"
  - type: script
    parameters:
      action: "add"
      type: "authentication"
      engine: "ECMAScript : Oracle Nashorn"
      name:                      # String: The name of the script, defaults to the file name
      file: "KeycloakLoginAndRefreshTokenInternal.js" #Make sure you use the correct script for your endpoint. You might need to modify the URL params.
````


## With hooks file
````python
def load_script():

    script = "KeycloakLoginAndRefreshTokenInternal.js" 
    // script = "KeycloakLoginAndRefreshToken.js"
    # Load a script into ZAP. Requires full PATH
    if not zap.script.load(script,"authentication","ECMAScript : Oracle Nashorn", "/zap/wrk/authscripts/" + script) == "OK":
    
    script = "AddBearerTokenHeader.js"
    # Load a script into ZAP. Requires full PATH
    if not zap.script.load(script,"httpsender","ECMAScript : Oracle Nashorn", "/zap/wrk/authscripts/" + script) == "OK":
      raise RuntimeError("Unable to load script 'AddBearerTokenHeader.js'.")
````