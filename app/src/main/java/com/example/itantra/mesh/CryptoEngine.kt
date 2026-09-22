package com.example.itantra.mesh

import android.util.Log
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom
import android.util.Base64

object CryptoEngine {
    private const val TAG = "CryptoEngine"
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128 // bits

    // In a production environment, this should be derived via Diffie-Hellman or stored in Keystore.
    // For this disaster mesh, a hardcoded pre-shared rescue key is used to allow zero-config offline pairing.
    private val PRE_SHARED_KEY = "173iTantraRescueKey2026!@#$12345".toByteArray(Charsets.UTF_8).copyOf(32)
    private val secretKey = SecretKeySpec(PRE_SHARED_KEY, ALGORITHM)

    fun encrypt(plaintext: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
            val cipherText = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Combine IV + CipherText for transmission
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Encryption failed", e)
            ""
        }
    }

    fun decrypt(encryptedPayload: String): String {
        return try {
            val combined = Base64.decode(encryptedPayload, Base64.NO_WRAP)
            
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
            val plainText = cipher.doFinal(cipherText)
            
            String(plainText, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            ""
        }
    }
}
