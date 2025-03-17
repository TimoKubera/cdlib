package de.deutschepost.sdm.cdlib.change.sharepoint.ntlm

import jcifs.ntlmssp.NtlmFlags
import jcifs.ntlmssp.Type1Message
import jcifs.ntlmssp.Type2Message
import jcifs.ntlmssp.Type3Message
import jcifs.util.Base64
import org.apache.http.impl.auth.NTLMEngine
import org.apache.http.impl.auth.NTLMEngineException
import java.io.IOException

class JCIFSEngine : NTLMEngine {
    @Throws(NTLMEngineException::class)
    override fun generateType1Msg(domain: String, workstation: String): String {
        val type1Message = Type1Message(TYPE_1_FLAGS, domain, workstation)
        return Base64.encode(type1Message.toByteArray())
    }

    @Throws(NTLMEngineException::class)
    override fun generateType3Msg(
        username: String, password: String,
        domain: String, workstation: String, challenge: String
    ): String {
        val type2Message: Type2Message = try {
            Type2Message(Base64.decode(challenge))
        } catch (exception: IOException) {
            throw NTLMEngineException("Invalid NTLM type 2 message", exception)
        }
        val type2Flags = type2Message.flags
        val type3Flags = (type2Flags
            and (-0x1 xor (NtlmFlags.NTLMSSP_TARGET_TYPE_DOMAIN or NtlmFlags.NTLMSSP_TARGET_TYPE_SERVER)))
        val type3Message = Type3Message(
            type2Message, password, domain,
            username, workstation, type3Flags
        )
        return Base64.encode(type3Message.toByteArray())
    }

    companion object {
        private const val TYPE_1_FLAGS = NtlmFlags.NTLMSSP_NEGOTIATE_56 or
            NtlmFlags.NTLMSSP_NEGOTIATE_128 or
            NtlmFlags.NTLMSSP_NEGOTIATE_NTLM2 or
            NtlmFlags.NTLMSSP_NEGOTIATE_ALWAYS_SIGN or
            NtlmFlags.NTLMSSP_REQUEST_TARGET
    }
}
