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
import java.security.SecureRandom;

/**
 * Stores the Aetherium pairing state in encrypted shared preferences.
 *
 * <p>Two kinds of codes exist:
 *
 * <ul>
 *   <li>a short 6 digit ephemeral code used for the automatic out-of-band pairing, and
 *   <li>a 60 character physical pairing code used for the "pair physically" flow.
 * </ul>
 */
public final class AetheriumPairing {

  private static final String KEY_PEER_PREFIX = "pk:";
  private static final String KEY_PHYS_PREFIX = "pp:";
  private static final String KEY_OWN_ID = "own_account_id";

  private static final String CHARS60 =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

  private final SharedPreferences prefs;
  private final SecureRandom random = new SecureRandom();

  public AetheriumPairing(Context ctx) {
    this.prefs = AetheriumPrefs.get(ctx, AetheriumPrefs.PAIRING_STORE);
  }

  /** Six digit ephemeral pairing code. */
  public String generateCode6() {
    long v = Math.abs(random.nextLong()) % 1_000_000L;
    return String.format(java.util.Locale.US, "%06d", v);
  }

  /** Sixty character physical pairing code. */
  public String generateCode60() {
    StringBuilder sb = new StringBuilder(60);
    for (int i = 0; i < 60; i++) {
      sb.append(CHARS60.charAt(random.nextInt(CHARS60.length())));
    }
    return sb.toString();
  }

  public boolean isPaired(String number) {
    return number != null && prefs.contains(KEY_PEER_PREFIX + number);
  }

  public void storePeer(String number, String publicKeyHex) {
    prefs.edit().putString(KEY_PEER_PREFIX + number, publicKeyHex).apply();
  }

  public String getPeer(String number) {
    return number == null ? null : prefs.getString(KEY_PEER_PREFIX + number, null);
  }

  public void storePhysicalPairCode(String number, String code60) {
    prefs.edit().putString(KEY_PHYS_PREFIX + number, code60).apply();
  }

  public String getPhysicalPairCode(String number) {
    return number == null ? null : prefs.getString(KEY_PHYS_PREFIX + number, null);
  }

  public boolean hasPhysicalPair(String number) {
    return number != null && prefs.contains(KEY_PHYS_PREFIX + number);
  }

  public void setOwnAccountId(String id) {
    prefs.edit().putString(KEY_OWN_ID, id).apply();
  }

  public String getOwnAccountId() {
    return prefs.getString(KEY_OWN_ID, null);
  }

  public SharedPreferences raw() {
    return prefs;
  }
}
