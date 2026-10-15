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
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;

/**
 * The three Aetherium call notices.
 *
 * <p>These are shown when a call starts and the peer also uses Aetherium Phone. If the peer does not
 * use Aetherium Phone <b>no popup is shown at all</b>.
 */
public final class AetheriumPopups {

  private AetheriumPopups() {}

  /** Peer uses Aetherium Phone, but the key could not be transferred (no wifi). */
  public static void showNoInternet(Context ctx, String peerName) {
    show(
        ctx,
        ctx.getString(
            com.fissy.dialer.R.string.aetherium_popup_no_internet,
            peerName == null ? "" : peerName));
  }

  /** Key transferred: the SIM call is end to end encrypted. */
  public static void showSuccess(Context ctx) {
    show(ctx, ctx.getString(com.fissy.dialer.R.string.aetherium_popup_success));
  }

  /** The peer used Aetherium Phone before, but is not using it right now. */
  public static void showStale(Context ctx) {
    show(ctx, ctx.getString(com.fissy.dialer.R.string.aetherium_popup_stale));
  }

  private static void show(Context ctx, String message) {
    try {
      AlertDialog dialog =
          new AlertDialog.Builder(ctx)
              .setTitle(com.fissy.dialer.R.string.aetherium_secure_title)
              .setMessage(message)
              .setPositiveButton(android.R.string.ok, (DialogInterface d, int w) -> d.dismiss())
              .create();
      dialog.show();
    } catch (Exception ignored) {
      // Never let a notice crash a call.
    }
  }
}
