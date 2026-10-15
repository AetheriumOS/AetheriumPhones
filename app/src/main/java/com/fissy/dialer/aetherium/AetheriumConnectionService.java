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

import android.net.Uri;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Telecom {@link ConnectionService} for Aetherium calls.
 *
 * <p>Provides a self managed connection so Aetherium secure calls can appear in the system in-call
 * UI alongside normal SIM calls, with their own callbacks.
 */
public final class AetheriumConnectionService extends ConnectionService {

  private static final String TAG = "AetheriumConnSvc";

  private final List<AetheriumConnection> connections = new ArrayList<>();
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  @Override
  public Connection onCreateOutgoingConnection(
      PhoneAccountHandle handle, ConnectionRequest request) {
    AetheriumConnection connection = new AetheriumConnection(request);
    connections.add(connection);
    Log.d(TAG, "onCreateOutgoingConnection " + request.getAddress());
    return connection;
  }

  @Override
  public Connection onCreateIncomingConnection(
      PhoneAccountHandle handle, ConnectionRequest request) {
    AetheriumConnection connection = new AetheriumConnection(request);
    connections.add(connection);
    Log.d(TAG, "onCreateIncomingConnection " + request.getAddress());
    return connection;
  }

  @Override
  public void onCreateIncomingConnectionFailed(PhoneAccountHandle handle, ConnectionRequest request) {
    Log.w(TAG, "incoming connection failed");
  }

  @Override
  public void onCreateOutgoingConnectionFailed(
      PhoneAccountHandle handle, ConnectionRequest request) {
    Log.w(TAG, "outgoing connection failed");
  }

  @Override
  public void onDestroy() {
    executor.shutdownNow();
    super.onDestroy();
  }

  /** A single Aetherium call. Runs the secure handshake and reports state upward. */
  private final class AetheriumConnection extends Connection {

    private final ConnectionRequest request;
    private HybridDetector detector;

    AetheriumConnection(ConnectionRequest request) {
      this.request = request;
      setConnectionProperties(PROPERTY_SELF_MANAGED);
      setAudioModeIsVoip(true);
      if (request.getAddress() != null) {
        setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
      } else {
        setAddress(Uri.EMPTY, TelecomManager.PRESENTATION_ALLOWED);
      }
      setCallerDisplayName("Aetherium", TelecomManager.PRESENTATION_ALLOWED);
    }

    @Override
    public void onAnswer() {
      setActive();
    }

    @Override
    public void onReject() {
      setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));
      destroy();
    }

    @Override
    public void onDisconnect() {
      setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
      destroy();
    }

    @Override
    public void onHold() {
      setOnHold();
    }

    @Override
    public void onUnhold() {
      setActive();
    }

    @Override
    public void onAbort() {
      setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
      destroy();
    }

    void beginSecureHandshake() {
      detector =
          new HybridDetector(
              transport -> {
                if (transport == HybridDetector.Transport.NONE) {
                  setConnectionProperties(PROPERTY_SELF_MANAGED);
                } else {
                  setConnectionProperties(PROPERTY_SELF_MANAGED);
                }
              });
      detector.start();
    }
  }

  /** Registers the Aetherium phone account if it is not registered yet. */
  public static void ensurePhoneAccount(TelecomManager telecom, PhoneAccountHandle handle) {
    try {
      telecom.registerPhoneAccount(
          android.telecom.PhoneAccount.builder(handle, "Aetherium Phone")
              .setCapabilities(
                  android.telecom.PhoneAccount.CAPABILITY_SELF_MANAGED
                      | android.telecom.PhoneAccount.CAPABILITY_CALL_PROVIDER)
              .build());
    } catch (Exception e) {
      Log.w(TAG, "registerPhoneAccount failed", e);
    }
  }
}
