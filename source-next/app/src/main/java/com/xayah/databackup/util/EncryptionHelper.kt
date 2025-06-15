package com.xayah.databackup.util

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object EncryptionHelper {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_FACTORY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256
    private const val IV_LENGTH = 16 // AES block size in bytes

    fun encrypt(data: ByteArray, password: CharArray): Pair<ByteArray, ByteArray> {
        val salt = ByteArray(KEY_LENGTH / 8)
        SecureRandom().nextBytes(salt)

        val keySpec = PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH)
        val secretKeyFactory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
        val secretKey = secretKeyFactory.generateSecret(keySpec)
        val secretKeySpec = SecretKeySpec(secretKey.encoded, ALGORITHM)

        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        val ivParameterSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)
        val encryptedData = cipher.doFinal(data)

        return Pair(salt, iv + encryptedData) // Prepend IV to the encrypted data
    }

    fun decrypt(encryptedDataWithIv: ByteArray, password: CharArray, salt: ByteArray): ByteArray {
        val iv = encryptedDataWithIv.copyOfRange(0, IV_LENGTH)
        val encryptedData = encryptedDataWithIv.copyOfRange(IV_LENGTH, encryptedDataWithIv.size)

        val keySpec = PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH)
        val secretKeyFactory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORITHM)
        val secretKey = secretKeyFactory.generateSecret(keySpec)
        val secretKeySpec = SecretKeySpec(secretKey.encoded, ALGORITHM)

        val ivParameterSpec = IvParameterSpec(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)
        return cipher.doFinal(encryptedData)
    }
}
