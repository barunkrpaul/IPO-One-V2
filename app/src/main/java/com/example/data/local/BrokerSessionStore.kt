package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

/**
 * Keeps one broker login per family member. The WebView has a single cookie
 * jar, so before showing a person's broker page their saved cookies and site
 * storage are put back, and after they log in the current ones are saved.
 * Everything is encrypted with a key held in the Android Keystore and never
 * leaves the phone. Brokers still end sessions on their own schedule (often
 * daily); then the person simply logs in again.
 */
class BrokerSessionStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("broker_sessions", Context.MODE_PRIVATE)

    /** Cookies for [cookieUrls] plus the page's local/session storage JSON, saved under [personKey]. */
    suspend fun save(personKey: String, cookieUrls: List<String>, storageJson: String?) = withContext(Dispatchers.IO) {
        val cookies = JSONObject()
        val manager = CookieManager.getInstance()
        cookieUrls.forEach { url -> manager.getCookie(url)?.let { cookies.put(url, it) } }
        val blob = JSONObject()
            .put("cookies", cookies)
            .put("storage", storageJson ?: "")
            .put("savedAt", System.currentTimeMillis())
        prefs.edit().putString(personKey, encrypt(blob.toString())).apply()
    }

    fun hasSession(personKey: String): Boolean = prefs.contains(personKey)

    /**
     * Clears the shared cookie jar and site storage, then restores [personKey]'s
     * cookies. Returns their saved site storage JSON (to be injected into the
     * broker page), or null when nothing was saved for them.
     */
    suspend fun restore(personKey: String): String? {
        clearWebSession()
        val stored = prefs.getString(personKey, null) ?: return null
        val blob = runCatching { JSONObject(decrypt(stored)) }.getOrNull() ?: return null
        val manager = CookieManager.getInstance()
        val cookies = blob.optJSONObject("cookies") ?: JSONObject()
        cookies.keys().forEach { url ->
            cookies.optString(url).split(";").map { it.trim() }.filter { it.contains('=') }.forEach { pair ->
                manager.setCookie(url, "$pair; Path=/; Secure")
            }
        }
        manager.flush()
        return blob.optString("storage").takeIf { it.isNotBlank() }
    }

    fun forget(personKey: String) {
        prefs.edit().remove(personKey).apply()
    }

    /** Empties the WebView's cookies and site storage so the next person starts clean. */
    suspend fun clearWebSession() {
        suspendCancellableCoroutine { cont ->
            CookieManager.getInstance().removeAllCookies { if (cont.isActive) cont.resume(Unit) }
        }
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().flush()
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val body = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(body, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val (iv, body) = stored.split(":", limit = 2).map { Base64.decode(it, Base64.NO_WRAP) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(body), Charsets.UTF_8)
    }

    companion object {
        private const val KEY_ALIAS = "ipo_one_broker_sessions"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        fun personKey(panNumber: String, broker: String): String = "${broker}_${panNumber.uppercase()}"
    }
}
