package com.xayah.databackup.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

class EncryptionHelperTest {

    @Test
    fun encryptDecrypt_success() {
        val data = "Hello, World!".toByteArray()
        val password = "testPassword".toCharArray()

        val (salt, encryptedDataWithIv) = EncryptionHelper.encrypt(data, password)
        val decryptedData = EncryptionHelper.decrypt(encryptedDataWithIv, password, salt)

        assertArrayEquals(data, decryptedData)
    }

    @Test
    fun encryptDecrypt_differentData() {
        val data1 = "Hello, World!".toByteArray()
        val data2 = "Goodbye, World!".toByteArray()
        val password = "testPassword".toCharArray()

        val (salt, encryptedDataWithIv1) = EncryptionHelper.encrypt(data1, password)
        val decryptedData1 = EncryptionHelper.decrypt(encryptedDataWithIv1, password, salt)

        val (salt2, encryptedDataWithIv2) = EncryptionHelper.encrypt(data2, password)
        val decryptedData2 = EncryptionHelper.decrypt(encryptedDataWithIv2, password, salt2)


        assertArrayEquals(data1, decryptedData1)
        assertArrayEquals(data2, decryptedData2)
        assertNotEquals(String(encryptedDataWithIv1), String(encryptedDataWithIv2))
    }

    @Test(expected = AEADBadTagException::class)
    fun decrypt_wrongPassword_throwsException() {
        val data = "Hello, World!".toByteArray()
        val correctPassword = "correctPassword".toCharArray()
        val wrongPassword = "wrongPassword".toCharArray()

        val (salt, encryptedDataWithIv) = EncryptionHelper.encrypt(data, correctPassword)
        EncryptionHelper.decrypt(encryptedDataWithIv, wrongPassword, salt)
    }

    @Test(expected = AEADBadTagException::class)
    fun decrypt_wrongSalt_throwsException() {
        val data = "Hello, World!".toByteArray()
        val password = "testPassword".toCharArray()

        val (salt, encryptedDataWithIv) = EncryptionHelper.encrypt(data, password)

        val wrongSalt = ByteArray(EncryptionHelper.KEY_LENGTH / 8)
        SecureRandom().nextBytes(wrongSalt)

        EncryptionHelper.decrypt(encryptedDataWithIv, password, wrongSalt)
    }

    @Test(expected = AEADBadTagException::class)
    fun decrypt_tamperedData_throwsException() {
        val data = "Hello, World!".toByteArray()
        val password = "testPassword".toCharArray()

        val (salt, encryptedDataWithIv) = EncryptionHelper.encrypt(data, password)

        // Tamper with the encrypted data
        encryptedDataWithIv[encryptedDataWithIv.size / 2] = (encryptedDataWithIv[encryptedDataWithIv.size / 2] + 1).toByte()

        EncryptionHelper.decrypt(encryptedDataWithIv, password, salt)
    }

    @Test
    fun encrypt_producesDifferentCiphertextForSameDataWithDifferentSalts() {
        val data = "Hello, World!".toByteArray()
        val password = "testPassword".toCharArray()

        val (salt1, encryptedDataWithIv1) = EncryptionHelper.encrypt(data, password)
        val (salt2, encryptedDataWithIv2) = EncryptionHelper.encrypt(data, password) // Encryption will generate a new salt

        assertNotEquals(String(salt1), String(salt2))
        assertNotEquals(String(encryptedDataWithIv1), String(encryptedDataWithIv2))
    }
}
