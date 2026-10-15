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

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

/** Thin wrapper that returns an encrypted prefs store with a plaintext fallback. */
public final class AetheriumPrefs {

  public static final String PAIRING_STORE = "aetherium_pairing";
  public static final String STATE_STORE = "aetherium_state";

  private AetheriumPrefs() {}

  public static SharedPreferences get(Context ctx, String name) {
    try {
      String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
      return EncryptedSharedPreferences.create(
          name,
          masterKeyAlias,
          ctx,
          EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
          EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    } catch (Exception e) {
      // Keystore can be unavailable on some emulators; degrade to normal prefs.
      return ctx.getSharedPreferences(name + "_plain", Context.MODE_PRIVATE);
    }
  }
}
