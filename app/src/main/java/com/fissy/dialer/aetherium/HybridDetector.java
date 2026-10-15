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

/**
 * Hybrid transport detector for Aetherium secure calls.
 *
 * <p>When a call is placed to a peer that also runs Aetherium Phone, the session key has to travel
 * out of band. Three transports are attempted, in order, inside a seven second window:
 *
 * <ol>
 *   <li><b>Acoustic</b> - a near-ultrasonic beacon in the 18-22 kHz band is played into the call.
 *   <li><b>Data-over-Voice</b> - the key is modulated as AFSK on top of the voice stream.
 *   <li><b>VoIP</b> - if nothing acoustic is heard we fall back to the IP channel.
 * </ol>
 *
 * <p>The detector reports the first transport that succeeds. It is deliberately transport agnostic
 * so the actual send/receive implementation lives in {@link AcousticEngine}.
 */
public final class HybridDetector {

  public enum Transport {
    ACOUSTIC,
    DATA_OVER_VOICE,
    VOIP,
    NONE
  }

  public interface Listener {
    void onTransportResolved(Transport transport);
  }

  public static final long WINDOW_MS = 7000L;
  public static final int ACOUSTIC_MIN_HZ = 18000;
  public static final int ACOUSTIC_MAX_HZ = 22000;

  private final Listener listener;
  private final AcousticEngine acousticEngine;

  private volatile boolean running;
  private long startedAt;

  public HybridDetector(Listener listener) {
    this.listener = listener;
    this.acousticEngine = new AcousticEngine();
  }

  /** Starts the detection window. Safe to call from the main thread. */
  public synchronized void start() {
    if (running) {
      return;
    }
    running = true;
    startedAt = System.currentTimeMillis();

    // Stage 1: acoustic beacon (near ultrasonic).
    acousticEngine.startListening(
        new AcousticEngine.BeaconListener() {
          @Override
          public void onBeacon(byte[] payload) {
            if (!running) {
              return;
            }
            running = false;
            acousticEngine.stop();
            listener.onTransportResolved(Transport.ACOUSTIC);
          }

          @Override
          public void onWindowExpired() {
            if (!running) {
              return;
            }
            // Stage 2: try Data-over-Voice before giving up.
            acousticEngine.startDataOverVoice(
                new AcousticEngine.BeaconListener() {
                  @Override
                  public void onBeacon(byte[] payload) {
                    if (!running) {
                      return;
                    }
                    running = false;
                    acousticEngine.stop();
                    listener.onTransportResolved(Transport.DATA_OVER_VOICE);
                  }

                  @Override
                  public void onWindowExpired() {
                    if (!running) {
                      return;
                    }
                    running = false;
                    acousticEngine.stop();
                    // Stage 3: nothing on the wire, use the IP fallback.
                    listener.onTransportResolved(Transport.VOIP);
                  }
                });
          }
        });
  }

  public synchronized void stop() {
    running = false;
    acousticEngine.stop();
  }

  public synchronized boolean isRunning() {
    return running;
  }

  public long elapsedMs() {
    return startedAt == 0 ? 0 : System.currentTimeMillis() - startedAt;
  }

  /** Transmits our beacon over the active call so the other side can detect us. */
  public void transmitBeacon(byte[] payload) {
    acousticEngine.transmitBeacon(payload);
  }
}
