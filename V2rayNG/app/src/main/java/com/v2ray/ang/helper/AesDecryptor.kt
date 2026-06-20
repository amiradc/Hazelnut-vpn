package com.v2ray.ang.helper

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

object AesDecryptor {
    private const val SECRET_SALT = "My_Super_Secret_Salt_990_Safe"

    fun decrypt(encryptedBase64: String, deviceId: String): String {
        val key = MessageDigest.getInstance("SHA-256")
            .digest("${deviceId}_${SECRET_SALT}".toByteArray())
        val iv = MessageDigest.getInstance("MD5")
            .digest("${SECRET_SALT}_${deviceId}".toByteArray())

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
        val decryptedBytes = cipher.doFinal(Base64.decode(encryptedBase64, Base64.NO_WRAP))
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
