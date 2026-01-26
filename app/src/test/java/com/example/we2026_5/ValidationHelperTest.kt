package com.example.we2026_5

import org.junit.Test
import org.junit.Assert.*

class ValidationHelperTest {

    @Test
    fun testValidPhoneNumber() {
        assertTrue(ValidationHelper.isValidPhoneNumber("0123456789"))
        assertTrue(ValidationHelper.isValidPhoneNumber("+49123456789"))
        assertTrue(ValidationHelper.isValidPhoneNumber("0049123456789"))
        assertTrue(ValidationHelper.isValidPhoneNumber("0123 456 789"))
        assertTrue(ValidationHelper.isValidPhoneNumber("0123-456-789"))
        assertFalse(ValidationHelper.isValidPhoneNumber("123")) // Zu kurz
        assertFalse(ValidationHelper.isValidPhoneNumber("abc")) // Keine Zahlen
        assertTrue(ValidationHelper.isValidPhoneNumber("")) // Leer ist OK (optional)
    }

    @Test
    fun testValidAddress() {
        assertTrue(ValidationHelper.isValidAddress("Musterstraße 1"))
        assertTrue(ValidationHelper.isValidAddress("Hauptstraße 42, 12345 Berlin"))
        assertFalse(ValidationHelper.isValidAddress("abc")) // Zu kurz, keine Zahl
        assertFalse(ValidationHelper.isValidAddress("Straße")) // Keine Zahl
        assertTrue(ValidationHelper.isValidAddress("")) // Leer ist OK (optional)
    }

    @Test
    fun testFormatPhoneNumber() {
        assertEquals("0123 456 7890", ValidationHelper.formatPhoneNumber("01234567890"))
        assertTrue(ValidationHelper.formatPhoneNumber("+49123456789").contains("+49"))
    }
}
