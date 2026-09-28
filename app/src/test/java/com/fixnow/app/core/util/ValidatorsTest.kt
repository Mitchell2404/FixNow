package com.fixnow.app.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun emailValido() {
        assertTrue("mitchell@unmsm.edu.pe".isValidEmail())
    }

    @Test
    fun emailInvalido() {
        assertFalse("mitchell@".isValidEmail())
        assertFalse("sin-arroba.com".isValidEmail())
        assertFalse("".isValidEmail())
    }
}
