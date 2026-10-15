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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Reads the signed in AetheriumServices account id.
 *
 * <p>AetheriumServices owns the account; it registers an Android account of type {@code
 * net.aetherium.services}. Here we look that account up through {@link AccountManager} so the phone
 * shares the same identity as the rest of the ecosystem. A handful of shared-preference stores are
 * still checked as a fallback.
 */
public final class AetheriumAccount {

  /** Account type registered by the AetheriumServices app. */
  public static final String SERVICES_ACCOUNT_TYPE = "net.aetherium.services";

  private static final String[] STORES = {
    "aetherium_services_account",
    "aetherium_services",
    "aetherium_account",
    "aetherium_auth",
    "aetherium_state"
  };

  private static final String[] KEYS = {"account_id", "accountId", "id", "user_id", "uid"};

  private AetheriumAccount() {}

  public static String getOwnAccountId(Context ctx) {
    // 1) The AetheriumServices Android account (preferred).
    try {
      AccountManager am = AccountManager.get(ctx);
      Account[] accounts = am.getAccountsByType(SERVICES_ACCOUNT_TYPE);
      if (accounts.length > 0) {
        Account account = accounts[0];
        String id = am.getUserData(account, "account_id");
        if (id != null && !id.trim().isEmpty()) {
          return id.trim();
        }
        return account.name;
      }
    } catch (Exception ignored) {
      // Fall through to the shared-preference stores.
    }

    // 2) Shared-preference stores (older AetheriumServices versions).
    for (String store : STORES) {
      SharedPreferences pref = AetheriumPrefs.get(ctx, store);
      for (String key : KEYS) {
        try {
          String v = pref.getString(key, null);
          if (v != null && !v.trim().isEmpty()) {
            return v.trim();
          }
        } catch (Exception ignored) {
          // Wrong type in a foreign store; skip.
        }
      }
    }
    // Last resort: a value that the user entered manually in Aetherium settings.
    return new AetheriumPairing(ctx).getOwnAccountId();
  }

  public static boolean isSignedIn(Context ctx) {
    return getOwnAccountId(ctx) != null;
  }
}
