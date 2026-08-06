package com.notesnync.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import net.openid.appauth.AuthState
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class GoogleDriveAuthStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("notesnync_google_drive_oauth", Context.MODE_PRIVATE)

    fun hasAuthState(): Boolean = load() != null

    fun load(): AuthState? {
        val encrypted = prefs.getString(AuthStateKey, null) ?: return null
        return runCatching { AuthState.jsonDeserialize(decrypt(encrypted)) }.getOrNull()
    }

    fun save(authState: AuthState) {
        prefs.edit().putString(AuthStateKey, encrypt(authState.jsonSerializeString())).apply()
    }

    fun clear() {
        prefs.edit().remove(AuthStateKey).apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(Transform)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + cipherText, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > IvSizeBytes) { "Stored Google auth state is invalid" }
        val iv = payload.copyOfRange(0, IvSizeBytes)
        val cipherText = payload.copyOfRange(IvSizeBytes, payload.size)
        val cipher = Cipher.getInstance(Transform)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TagSizeBits, iv))
        return cipher.doFinal(cipherText).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(AndroidKeyStore).apply { load(null) }
        (keyStore.getEntry(KeyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, AndroidKeyStore)
        generator.init(
            KeyGenParameterSpec.Builder(KeyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val AuthStateKey = "auth_state"
        private const val AndroidKeyStore = "AndroidKeyStore"
        private const val KeyAlias = "notesnync-google-drive-auth-state"
        private const val Transform = "AES/GCM/NoPadding"
        private const val IvSizeBytes = 12
        private const val TagSizeBits = 128
    }
}
