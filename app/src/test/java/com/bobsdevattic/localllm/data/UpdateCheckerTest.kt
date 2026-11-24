package com.bobsdevattic.localllm.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test fun detectsNewerPatch() {
        assertTrue(UpdateChecker.isNewer("0.1.2", "0.1.1"))
    }

    @Test fun detectsNewerMinorAndMajor() {
        assertTrue(UpdateChecker.isNewer("0.2.0", "0.1.9"))
        assertTrue(UpdateChecker.isNewer("1.0.0", "0.9.9"))
    }

    @Test fun sameVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("0.1.1", "0.1.1"))
    }

    @Test fun olderVersionIsNotNewer() {
        assertFalse(UpdateChecker.isNewer("0.1.0", "0.1.1"))
        assertFalse(UpdateChecker.isNewer("0.9.9", "1.0.0"))
    }

    @Test fun handlesDifferingComponentCounts() {
        assertTrue(UpdateChecker.isNewer("0.2", "0.1.9"))
        assertFalse(UpdateChecker.isNewer("0.1", "0.1.0"))
    }

    @Test fun ignoresNonNumericSuffixes() {
        assertTrue(UpdateChecker.isNewer("0.2.0-rc1", "0.1.1"))
        assertFalse(UpdateChecker.isNewer("0.1.1-beta", "0.1.1"))
    }
}
