package com.bobsdevattic.localllm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointValidationTest {
    @Test fun acceptsAnHttpApiBaseUrl() {
        assertEquals("http", parseApiBaseUrl("http://192.168.1.10:1234/v1").scheme)
    }

    @Test fun acceptsHttpsApiBaseUrl() {
        assertEquals("https", parseApiBaseUrl("https://model.example.test/v1").scheme)
    }

    @Test fun rejectsCredentialsInEndpoint() {
        val error = runCatching { parseApiBaseUrl("http://user:secret@192.168.1.10:1234/v1") }.exceptionOrNull()
        assertTrue(error is ApiException)
    }

    @Test fun rejectsUnexpectedApiPath() {
        val error = runCatching { parseApiBaseUrl("http://192.168.1.10:1234/") }.exceptionOrNull()
        assertTrue(error is ApiException)
    }

    @Test fun rejectsQueryParameters() {
        val error = runCatching { parseApiBaseUrl("https://model.example.test/v1?token=secret") }.exceptionOrNull()
        assertTrue(error is ApiException)
    }

    @Test fun refusesPublicHostsForPlainHttp() {
        assertFalse(isPrivateHttpHost("8.8.8.8"))
    }

    @Test fun refusesHostnamesForPlainHttpToAvoidDnsRebinding() {
        assertFalse(isPrivateHttpHost("lm-studio.local"))
    }

    @Test fun allowsLoopbackForLocalDevelopment() {
        assertTrue(isPrivateHttpHost("127.0.0.1"))
    }
}
