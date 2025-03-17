package de.deutschepost.sdm.cdlib.change.sharepoint.ntlm

import org.apache.http.auth.AuthScheme
import org.apache.http.auth.AuthSchemeProvider
import org.apache.http.impl.auth.NTLMScheme
import org.apache.http.protocol.HttpContext

class JCIFSNTLMSchemeFactory : AuthSchemeProvider {
    override fun create(context: HttpContext): AuthScheme {
        return NTLMScheme(JCIFSEngine())
    }
}
