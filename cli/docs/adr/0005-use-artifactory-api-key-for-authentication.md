# 5. Use Artifactory API Key for Authentication

Date: 2021-08-18

## Status

Accepted

## Context

- We want to use a Token/Key for authentication with Artifactory
- The user should not be able to use a password for authentication
- The user should be able to create the Token/Key with the Artifactory UI
- Artifactory Access Token can only be created via the REST API and not in the UI
- Artifactory API Key can be created in the Artifactory UI

## Decision

We use the Artifactory API Key for authentication. 

## Consequences

To be able to use the API Key for authentication we have to inject a header `X-JFrog-Art-Api`. 
We could use the API Key in the Basic Auth, but then the user would be able to use their password.
Therefore, injecting the header with API Key is the only solution.
