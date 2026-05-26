package com.librenotes.app.domain

import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class EncryptedVault(
    val format: String,
    val iterations: Int,
    val salt: String,
    val nonce: String,
    val cipherText: String,
)

object VaultCrypto {
    const val Format = "libre-notes-aes-gcm-v1"
    private const val Iterations = 160_000
    private const val KeyBits = 256
    private const val NonceBytes = 12
    private const val SaltBytes = 16
    private const val TagBits = 128

    fun encrypt(plainText: String, secret: CharArray): String {
        require(secret.isNotEmpty()) { "Secret must not be empty." }
        val salt = randomBytes(SaltBytes)
        val nonce = randomBytes(NonceBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(secret, salt), GCMParameterSpec(TagBits, nonce))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return buildString {
            append("{")
            append("\"format\":\"").append(Format).append("\",")
            append("\"iterations\":").append(Iterations).append(",")
            append("\"salt\":\"").append(b64(salt)).append("\",")
            append("\"nonce\":\"").append(b64(nonce)).append("\",")
            append("\"cipherText\":\"").append(b64(cipherText)).append("\"")
            append("}")
        }
    }

    fun decrypt(payload: String, secret: CharArray): String {
        require(secret.isNotEmpty()) { "Secret must not be empty." }
        val decoded = parsePayload(payload)
        require(decoded.format == Format) { "Unsupported vault format." }
        val salt = b64decode(decoded.salt)
        val nonce = b64decode(decoded.nonce)
        val cipherText = b64decode(decoded.cipherText)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, deriveKey(secret, salt), GCMParameterSpec(TagBits, nonce))
        return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    fun parsePayload(payload: String): EncryptedVault {
        val pairs = Regex("\"([^\"]+)\"\\s*:\\s*(\"([^\"]*)\"|\\d+)")
            .findAll(payload)
            .associate { match ->
                match.groupValues[1] to (match.groupValues[3].ifEmpty { match.groupValues[2] })
            }
        return EncryptedVault(
            format = pairs.getValue("format"),
            iterations = pairs["iterations"]?.toIntOrNull() ?: Iterations,
            salt = pairs.getValue("salt"),
            nonce = pairs.getValue("nonce"),
            cipherText = pairs.getValue("cipherText"),
        )
    }

    private fun deriveKey(secret: CharArray, salt: ByteArray): SecretKeySpec {
        val spec: KeySpec = PBEKeySpec(secret, salt, Iterations, KeyBits)
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { SecureRandom().nextBytes(it) }
    private fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun b64decode(value: String): ByteArray = Base64.getDecoder().decode(value)
}

