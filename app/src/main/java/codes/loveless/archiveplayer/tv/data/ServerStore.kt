package codes.loveless.archiveplayer.tv.data

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ServerStore(context: Context) {
    private val prefs = context.getSharedPreferences("archiveplayer_tv", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun loadProfile(): ServerProfile? {
        val raw = prefs.getString(KEY_PROFILE, null) ?: return null
        val profile = runCatching { json.decodeFromString<ServerProfile>(raw) }.getOrNull() ?: return null
        return profile.copy(token = readToken())
    }

    fun saveProfile(profile: ServerProfile) {
        prefs.edit().putString(KEY_PROFILE, json.encodeToString(profile.copy(token = null))).apply()
        if (profile.token.isNullOrBlank()) {
            prefs.edit().remove(KEY_TOKEN).apply()
        } else {
            prefs.edit().putString(KEY_TOKEN, encrypt(profile.token)).apply()
        }
        if (!profile.deviceName.isNullOrBlank()) {
            setDeviceName(profile.deviceName)
        }
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun deviceName(): String {
        val stored = prefs.getString(KEY_DEVICE_NAME, null)
        if (!stored.isNullOrBlank()) return stored
        val model = Build.MODEL?.trim().orEmpty()
        return if (model.isBlank() || model.equals("unknown", ignoreCase = true)) {
            "Android TV"
        } else {
            "$model TV"
        }.take(80)
    }

    fun setDeviceName(name: String) {
        val cleaned = name.trim().take(80).ifBlank { "Android TV" }
        prefs.edit().putString(KEY_DEVICE_NAME, cleaned).apply()
    }

    fun activeUrl(): String? = prefs.getString(KEY_ACTIVE_URL, null)

    fun setActiveUrl(url: String) {
        prefs.edit().putString(KEY_ACTIVE_URL, url).apply()
    }

    private fun readToken(): String? {
        val stored = prefs.getString(KEY_TOKEN, null) ?: return null
        return runCatching { decrypt(stored) }.getOrElse {
            prefs.edit().remove(KEY_TOKEN).apply()
            null
        }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val payload = byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > 2) { "Invalid token" }
        val ivSize = payload[0].toInt() and 0xFF
        val iv = payload.copyOfRange(1, 1 + ivSize)
        val ciphertext = payload.copyOfRange(1 + ivSize, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "archiveplayer-tv-token"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_PROFILE = "profile"
        private const val KEY_TOKEN = "token"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_ACTIVE_URL = "active_url"
    }
}
