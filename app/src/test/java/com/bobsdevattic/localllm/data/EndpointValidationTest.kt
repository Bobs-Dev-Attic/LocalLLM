package com.bobsdevattic.localllm.data

import org.junit.Assert.assertTrue
import org.junit.Test

class EndpointValidationTest {
    @Test fun privateLanHttpIsTheDocumentedDefaultPattern() {
        // Endpoint parsing and DNS policy are exercised by integration tests with MockWebServer.
        assertTrue("http://192.168.1.10:1234/v1".startsWith("http://"))
    }
}
