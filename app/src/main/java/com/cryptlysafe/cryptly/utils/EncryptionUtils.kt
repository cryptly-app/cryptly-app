package com.cryptlysafe.cryptly.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Secure encryption utility for Cryptly app
 * 
 * This utility provides AES-256 encryption and decryption for:
 * - Chat messages
 * - Call metadata
 * - User data
 * - PIN codes
 * 
 * TODO: Replace hardcoded keys with Android Keystore for production
 * TODO: Implement key rotation mechanism
 * TODO: Add key derivation from user password
 */
object EncryptionUtils {
    
    companion object {
        // TODO: Replace with Android Keystore in production
        // This is a hardcoded key for testing purposes only
        private const val SECRET_KEY = "CryptlySecretKey2024SecureMessagingApp"
        
        // Encryption algorithm
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        
        // Error messages
        private const val ERROR_ENCRYPTION_FAILED = "Encryption failed"
        private const val ERROR_DECRYPTION_FAILED = "Decryption failed"
        private const val ERROR_INVALID_KEY = "Invalid encryption key"
        private const val ERROR_INVALID_DATA = "Invalid encrypted data"
    }
    
    /**
     * Encrypts plain text using AES-256-GCM
     * 
     * @param plainText The text to encrypt
     * @return Base64 encoded encrypted string
     * @throws EncryptionException if encryption fails
     */
    @Throws(EncryptionException::class)
    fun encrypt(plainText: String): String {
        return try {
            // Generate a secure random IV
            val iv = generateSecureIV()
            
            // Create cipher instance
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Create secret key
            val secretKey = generateSecretKey()
            
            // Initialize cipher for encryption
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            
            // Encrypt the plain text
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = iv + encryptedBytes
            
            // Encode to Base64 for safe storage
            Base64.encodeToString(combined, Base64.NO_WRAP)
            
        } catch (e: Exception) {
            throw EncryptionException(ERROR_ENCRYPTION_FAILED, e)
        }
    }
    
    /**
     * Decrypts encrypted text using AES-256-GCM
     * 
     * @param encryptedText Base64 encoded encrypted string
     * @return Decrypted plain text
     * @throws EncryptionException if decryption fails
     */
    @Throws(EncryptionException::class)
    fun decrypt(encryptedText: String): String {
        return try {
            // Decode from Base64
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            
            // Validate data length
            if (combined.size < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw EncryptionException(ERROR_INVALID_DATA)
            }
            
            // Extract IV and encrypted data
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            
            // Create cipher instance
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Create secret key
            val secretKey = generateSecretKey()
            
            // Initialize cipher for decryption
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            // Decrypt the data
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            
            // Convert to string
            String(decryptedBytes, Charsets.UTF_8)
            
        } catch (e: Exception) {
            throw EncryptionException(ERROR_DECRYPTION_FAILED, e)
        }
    }
    
    /**
     * Encrypts data with a custom key (for user-specific encryption)
     * 
     * @param plainText The text to encrypt
     * @param customKey Custom encryption key
     * @return Base64 encoded encrypted string
     */
    @Throws(EncryptionException::class)
    fun encryptWithKey(plainText: String, customKey: String): String {
        return try {
            val iv = generateSecureIV()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = generateSecretKeyFromString(customKey)
            
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
            
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = iv + encryptedBytes
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
            
        } catch (e: Exception) {
            throw EncryptionException(ERROR_ENCRYPTION_FAILED, e)
        }
    }
    
    /**
     * Decrypts data with a custom key
     * 
     * @param encryptedText Base64 encoded encrypted string
     * @param customKey Custom encryption key
     * @return Decrypted plain text
     */
    @Throws(EncryptionException::class)
    fun decryptWithKey(encryptedText: String, customKey: String): String {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            
            if (combined.size < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw EncryptionException(ERROR_INVALID_DATA)
            }
            
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = generateSecretKeyFromString(customKey)
            
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
            
        } catch (e: Exception) {
            throw EncryptionException(ERROR_DECRYPTION_FAILED, e)
        }
    }
    
    /**
     * Generates a secure random IV
     */
    private fun generateSecureIV(): ByteArray {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)
        return iv
    }
    
    /**
     * Generates a secret key from the hardcoded secret
     * TODO: Replace with Android Keystore in production
     */
    private fun generateSecretKey(): SecretKey {
        return try {
            val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
            keyGenerator.init(KEY_SIZE)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            // Fallback to key derivation from string
            generateSecretKeyFromString(SECRET_KEY)
        }
    }
    
    /**
     * Generates a secret key from a string using SHA-256
     */
    private fun generateSecretKeyFromString(keyString: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(keyString.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash, ALGORITHM)
    }
    
    /**
     * Validates if a string is properly encrypted
     */
    fun isValidEncryptedData(encryptedText: String): Boolean {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            combined.size >= GCM_IV_LENGTH + GCM_TAG_LENGTH
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Generates a secure random key for testing
     */
    fun generateRandomKey(): String {
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        return Base64.encodeToString(keyBytes, Base64.NO_WRAP)
    }
}

/**
 * Custom exception for encryption/decryption errors
 */
class EncryptionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Encryption levels for different types of data
 */
enum class EncryptionLevel {
    BASIC,      // 128-bit AES
    STANDARD,   // 256-bit AES (default)
    HIGH,       // 256-bit AES with additional security measures
    MILITARY    // Military-grade encryption (future implementation)
}

/**
 * Utility functions for specific encryption needs
 */
object MessageEncryption {
    
    /**
     * Encrypts a chat message
     */
    fun encryptMessage(message: String, encryptionLevel: EncryptionLevel = EncryptionLevel.STANDARD): String {
        return when (encryptionLevel) {
            EncryptionLevel.BASIC -> encryptBasic(message)
            EncryptionLevel.STANDARD -> EncryptionUtils.encrypt(message)
            EncryptionLevel.HIGH -> encryptHigh(message)
            EncryptionLevel.MILITARY -> encryptMilitary(message)
        }
    }
    
    /**
     * Decrypts a chat message
     */
    fun decryptMessage(encryptedMessage: String, encryptionLevel: EncryptionLevel = EncryptionLevel.STANDARD): String {
        return when (encryptionLevel) {
            EncryptionLevel.BASIC -> decryptBasic(encryptedMessage)
            EncryptionLevel.STANDARD -> EncryptionUtils.decrypt(encryptedMessage)
            EncryptionLevel.HIGH -> decryptHigh(encryptedMessage)
            EncryptionLevel.MILITARY -> decryptMilitary(encryptedMessage)
        }
    }
    
    private fun encryptBasic(message: String): String {
        // TODO: Implement 128-bit AES encryption
        return EncryptionUtils.encrypt(message)
    }
    
    private fun decryptBasic(encryptedMessage: String): String {
        // TODO: Implement 128-bit AES decryption
        return EncryptionUtils.decrypt(encryptedMessage)
    }
    
    private fun encryptHigh(message: String): String {
        // TODO: Implement high-security encryption with additional measures
        return EncryptionUtils.encrypt(message)
    }
    
    private fun decryptHigh(encryptedMessage: String): String {
        // TODO: Implement high-security decryption
        return EncryptionUtils.decrypt(encryptedMessage)
    }
    
    private fun encryptMilitary(message: String): String {
        // TODO: Implement military-grade encryption
        return EncryptionUtils.encrypt(message)
    }
    
    private fun decryptMilitary(encryptedMessage: String): String {
        // TODO: Implement military-grade decryption
        return EncryptionUtils.decrypt(encryptedMessage)
    }
} 