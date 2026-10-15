/*
 * Copyright (C) 2026 The Aetherium Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package com.fissy.dialer.aetherium;

import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Cryptographic primitives for Aetherium secure calling.
 *
 * <p>X25519 ECDH + HKDF-SHA256 + ChaCha20-Poly1305. The goal is that a normal SIM
 * call is end to end encrypted so the carrier or operator only hears gibberish.
 */
public final class AetheriumCrypto {

  public static final String HKDF_INFO = "aetherium-call-v1";

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  private AetheriumCrypto() {}

  /** Generates an ephemeral X25519 key pair. */
  public static KeyPair generateX25519() {
    try {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
      kpg.initialize(255);
      return kpg.generateKeyPair();
    } catch (Exception e) {
      throw new IllegalStateException("X25519 unavailable", e);
    }
  }

  /** Raw X25519 shared secret between our private key and the peer public key. */
  public static byte[] x25519Shared(PrivateKey priv, PublicKey pub) {
    try {
      KeyAgreement ka = KeyAgreement.getInstance("X25519");
      ka.init(priv);
      ka.doPhase(pub, true);
      return ka.generateSecret();
    } catch (Exception e) {
      throw new IllegalStateException("X25519 agreement failed", e);
    }
  }

  /** HKDF-SHA256 (extract + expand) over {@link #HKDF_INFO}. */
  public static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int len) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(salt == null ? new byte[32] : salt, "HmacSHA256"));
      byte[] prk = mac.doFinal(ikm);
      if (info == null) {
        info = HKDF_INFO.getBytes("UTF-8");
      }
      byte[] previous = new byte[0];
      byte[] generated = new byte[0];
      byte counter = 1;
      while (generated.length < len) {
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        ByteBuffer buf = ByteBuffer.allocate(previous.length + info.length + 1);
        buf.put(previous).put(info).put(counter);
        previous = mac.doFinal(buf.array());
        byte[] next = new byte[generated.length + previous.length];
        System.arraycopy(generated, 0, next, 0, generated.length);
        System.arraycopy(previous, 0, next, generated.length, previous.length);
        generated = next;
        counter++;
      }
      byte[] out = new byte[len];
      System.arraycopy(generated, 0, out, 0, len);
      return out;
    } catch (Exception e) {
      throw new IllegalStateException("HKDF failed", e);
    }
  }

  public static byte[] hkdfSha256(byte[] ikm) {
    return hkdfSha256(ikm, null, HKDF_INFO.getBytes(), 32);
  }

  public static byte[] chacha20Poly1305Encrypt(byte[] key, byte[] nonce12, byte[] plain, byte[] aad) {
    try {
      Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
      cipher.init(
          Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "ChaCha20"), new javax.crypto.spec.IvParameterSpec(nonce12));
      if (aad != null) {
        cipher.updateAAD(aad);
      }
      return cipher.doFinal(plain);
    } catch (Exception e) {
      throw new IllegalStateException("Encrypt failed", e);
    }
  }

  public static byte[] chacha20Poly1305Decrypt(byte[] key, byte[] nonce12, byte[] cipherText, byte[] aad) {
    try {
      Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
      cipher.init(
          Cipher.DECRYPT_MODE, new SecretKeySpec(key, "ChaCha20"), new javax.crypto.spec.IvParameterSpec(nonce12));
      if (aad != null) {
        cipher.updateAAD(aad);
      }
      return cipher.doFinal(cipherText);
    } catch (Exception e) {
      throw new IllegalStateException("Decrypt failed", e);
    }
  }

  public static byte[] randomNonce12() {
    byte[] b = new byte[12];
    RANDOM.nextBytes(b);
    return b;
  }

  public static byte[] randomBytes(int n) {
    byte[] b = new byte[n];
    RANDOM.nextBytes(b);
    return b;
  }

  public static byte[] sha256(byte[] b) {
    try {
      return MessageDigest.getInstance("SHA-256").digest(b);
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }

  public static String toHex(byte[] b) {
    char[] out = new char[b.length * 2];
    for (int i = 0; i < b.length; i++) {
      int v = b[i] & 0xFF;
      out[i * 2] = HEX[v >>> 4];
      out[i * 2 + 1] = HEX[v & 0x0F];
    }
    return new String(out);
  }

  public static byte[] fromHex(String s) {
    if (s == null) {
      return null;
    }
    int len = s.length();
    byte[] out = new byte[len / 2];
    for (int i = 0; i < out.length; i++) {
      out[i] = (byte) ((Character.digit(s.charAt(i * 2), 16) << 4) + Character.digit(s.charAt(i * 2 + 1), 16));
    }
    return out;
  }

  /** Encodes a peer public key to hex for storage. */
  public static String encodePublicKey(PublicKey pub) {
    return toHex(pub.getEncoded());
  }

  public static PublicKey decodePublicKey(String hex) {
    try {
      byte[] enc = fromHex(hex);
      return KeyFactory.getInstance("X25519").generatePublic(new X509EncodedKeySpec(enc));
    } catch (Exception e) {
      return null;
    }
  }
}
