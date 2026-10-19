package com.example.util

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricCapability(val label: String, val canAuthenticate: Boolean) {
    READY("Biometric Hardware Ready (Fingerprint / Face Unlock)", true),
    NO_HARDWARE("No Biometric Hardware Detected on Device", false),
    HW_UNAVAILABLE("Biometric Sensor Temporarily Unavailable", false),
    NONE_ENROLLED("No Fingerprint/Face Enrolled in Device Settings", false),
    UNKNOWN("Biometric Status Unknown", false)
}

object BiometricAuthHelper {

    fun checkCapability(context: Context): BiometricCapability {
        val manager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        return when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.READY
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NONE_ENROLLED
            else -> BiometricCapability.UNKNOWN
        }
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock IPO One Financial Vault",
        subtitle: String = "Verify your identity to access PAN Vault, Demat IDs & IPO Applications",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val capability = checkCapability(activity)
        if (!capability.canAuthenticate) {
            onError("${capability.label}. Please enroll a fingerprint or screen lock in Android Settings.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric not recognized. Try again.")
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(promptInfo)
    }
}
