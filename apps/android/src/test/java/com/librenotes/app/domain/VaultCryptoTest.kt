package com.librenotes.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VaultCryptoTest {
    @Test
    fun encryptsAndDecryptsVaultText() {
        val clearText = """{"notes":[{"title":"Private"}]}"""
        val encrypted = VaultCrypto.encrypt(clearText, "123456".toCharArray())

        assertNotEquals(clearText, encrypted)
        assertEquals(clearText, VaultCrypto.decrypt(encrypted, "123456".toCharArray()))
    }

    @Test
    fun wrongSecretCannotDecryptVaultText() {
        val encrypted = VaultCrypto.encrypt("secret", "123456".toCharArray())

        assertThrows(Exception::class.java) {
            VaultCrypto.decrypt(encrypted, "000000".toCharArray())
        }
    }
}

