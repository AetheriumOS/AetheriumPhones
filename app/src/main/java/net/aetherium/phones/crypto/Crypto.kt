package net.aetherium.phones.crypto

import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.ChaCha20ParameterSpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    private val rnd = SecureRandom()
    fun generateX25519(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("X25519")
        kpg.initialize(255)
        return kpg.generateKeyPair()
    }
    fun x25519Shared(priv: PrivateKey, pub: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("X25519")
        ka.init(priv)
        ka.doPhase(pub, true)
        return ka.generateSecret()
    }
    fun hkdfSha256(ikm: ByteArray, salt: ByteArray? = null, info: ByteArray = "aetherium-call-v1".toByteArray(), len: Int = 32): ByteArray {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(salt ?: ByteArray(32), "HmacSHA256"))
        val prk = mac.doFinal(ikm)
        var previous = ByteArray(0)
        var generated = ByteArray(0)
        var counter = 1.toByte()
        while (generated.size < len) {
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            val buf = ByteBuffer.allocate(previous.size + info.size + 1)
                .put(previous)
                .put(info)
                .put(counter)
                .array()
            previous = mac.doFinal(buf)
            generated = generated + previous
            counter++
        }
        return generated.copyOf(len)
    }
    fun chacha20Poly1305Encrypt(key: ByteArray, nonce12: ByteArray, plain: ByteArray, aad: ByteArray? = null): ByteArray {
        val sk = SecretKeySpec(key, "ChaCha20")
        val cipher = Cipher.getInstance("ChaCha20-Poly1305/None/NoPadding")
        val spec = ChaCha20ParameterSpec(nonce12, 0)
        cipher.init(Cipher.ENCRYPT_MODE, sk, spec)
        if (aad != null) cipher.updateAAD(aad)
        return cipher.doFinal(plain)
    }
    fun chacha20Poly1305Decrypt(key: ByteArray, nonce12: ByteArray, cipherText: ByteArray, aad: ByteArray? = null): ByteArray {
        val sk = SecretKeySpec(key, "ChaCha20")
        val cipher = Cipher.getInstance("ChaCha20-Poly1305/None/NoPadding")
        val spec = ChaCha20ParameterSpec(nonce12, 0)
        cipher.init(Cipher.DECRYPT_MODE, sk, spec)
        if (aad != null) cipher.updateAAD(aad)
        return cipher.doFinal(cipherText)
    }
    fun randomNonce12(): ByteArray {
        val b = ByteArray(12)
        rnd.nextBytes(b)
        return b
    }
    fun sha256(b: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(b)
}
