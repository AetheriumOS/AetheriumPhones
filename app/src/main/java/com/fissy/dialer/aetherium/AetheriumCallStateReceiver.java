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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Watches the normal SIM call state and drives the Aetherium secure-call handshake.
 *
 * <p>When a call goes off-hook (either placed or answered) and the peer is known to use Aetherium
 * Phone, we open a seven second detection window. Depending on what we find we either confirm that
 * the call is encrypted or warn the user. Nothing at all is shown when the peer does not use
 * Aetherium Phone.
 */
public final class AetheriumCallStateReceiver extends BroadcastReceiver {

  private static final String TAG = "AetheriumCallState";

  /** Last number dialled from the in-app dialpad, used for outgoing calls. */
  private static volatile String lastDialedNumber;

  private static HybridDetector detector;

  public static void setLastDialedNumber(String number) {
    lastDialedNumber = number;
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    if (intent == null || !TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
      return;
    }
    String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
    String incoming = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
    if (state == null) {
      return;
    }
    if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
      stopDetection();
      return;
    }
    if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
      String number = incoming != null ? incoming : lastDialedNumber;
      startHandshake(context.getApplicationContext(), number);
    }
  }

  private static void startHandshake(final Context ctx, String rawNumber) {
    if (rawNumber == null || rawNumber.trim().isEmpty()) {
      return;
    }
    final String number = normalize(rawNumber);
    final AetheriumPairing pairing = new AetheriumPairing(ctx);

    // The peer must be a known Aetherium Phone user, otherwise stay silent.
    final boolean peerKnown = pairing.isPaired(number) || pairing.hasPhysicalPair(number);
    if (!peerKnown) {
      Log.d(TAG, "peer not an Aetherium user, no popup");
      return;
    }

    final String peerName = displayName(ctx, number);

    if (!AetheriumApi.isInternetAvailable(ctx)) {
      AetheriumPopups.showNoInternet(ctx, peerName);
      return;
    }

    String ownId = AetheriumAccount.getOwnAccountId(ctx);
    AetheriumApi.phoneCodeSetup(
        ownId,
        pairing.getPeer(number),
        result -> {
          if (!result.ok || result.code == null) {
            // Server reachable, peer account not active right now.
            AetheriumPopups.showStale(ctx);
            return;
          }
          // Peer is active: resolve the best transport within the window.
          pairing.storePhysicalPairCode(number, result.code);
          runDetection(ctx, result.code);
        });
  }

  private static void runDetection(final Context ctx, String code) {
    stopDetection();
    detector =
        new HybridDetector(
            transport -> {
              if (transport == HybridDetector.Transport.NONE) {
                AetheriumPopups.showStale(ctx);
              } else {
                AetheriumPopups.showSuccess(ctx);
              }
              stopDetection();
            });
    detector.start();
  }

  private static synchronized void stopDetection() {
    if (detector != null) {
      detector.stop();
      detector = null;
    }
  }

  private static String normalize(String number) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < number.length(); i++) {
      char c = number.charAt(i);
      if (Character.isDigit(c) || c == '+') {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private static String displayName(Context ctx, String number) {
    try (Cursor c =
        ctx.getContentResolver()
            .query(
                Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number)),
                new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME},
                null,
                null,
                null)) {
      if (c != null && c.moveToFirst()) {
        return c.getString(0);
      }
    } catch (Exception ignored) {
    }
    return number;
  }
}
