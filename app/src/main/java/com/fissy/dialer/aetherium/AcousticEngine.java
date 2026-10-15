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

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real audio transport used by {@link HybridDetector}.
 *
 * <p>Two modes:
 *
 * <ul>
 *   <li><b>Acoustic</b>: a short near-ultrasonic beacon (18-22 kHz) played with {@link AudioTrack}
 *       and detected with {@link AudioRecord} using a Goertzel filter per bin.
 *   <li><b>Data-over-Voice</b>: a slow AFSK burst (mark/space tones inside the voice band) so the
 *       payload survives a normal, de-noised voice channel.
 * </ul>
 */
public final class AcousticEngine {

  private static final String TAG = "AetheriumAcoustic";

  public interface BeaconListener {
    void onBeacon(byte[] payload);

    void onWindowExpired();
  }

  private static final int SAMPLE_RATE = 44100;
  private static final int ACOUSTIC_HZ = 19000; // middle of the 18-22 kHz band
  private static final int AFSK_MARK_HZ = 1800;
  private static final int AFSK_SPACE_HZ = 1200;
  private static final int SYMBOL_MS = 20;
  private static final int WINDOW_MS = 7000;

  private final AtomicBoolean active = new AtomicBoolean(false);
  private final Handler handler = new Handler(Looper.getMainLooper());

  private AudioTrack toneTrack;
  private AudioRecord record;
  private Thread rxThread;
  private Runnable windowTimeout;

  /** Plays the ultrasonic beacon once, then listens for a peer beacon. */
  public void startListening(BeaconListener listener) {
    if (!active.compareAndSet(false, true)) {
      return;
    }
    playToneBurst(ACOUSTIC_HZ, 250);
    startReceiver(listener, true);
  }

  /** Switches the receiver to the voice-band AFSK mode. */
  public void startDataOverVoice(BeaconListener listener) {
    stopReceiverOnly();
    startReceiver(listener, false);
  }

  public void transmitBeacon(byte[] payload) {
    playToneBurst(ACOUSTIC_HZ, 250);
    if (payload != null) {
      transmitAfsk(payload);
    }
  }

  private void startReceiver(final BeaconListener listener, final boolean acoustic) {
    windowTimeout =
        () -> {
          if (active.get()) {
            stopReceiverOnly();
            listener.onWindowExpired();
          }
        };
    handler.postDelayed(windowTimeout, WINDOW_MS);

    rxThread =
        new Thread(
            () -> {
              int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
              if (minBuf <= 0) {
                minBuf = SAMPLE_RATE;
              }
              try {
                record =
                    new AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2);
                if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                  return;
                }
                record.startRecording();
                short[] buf = new short[1024];
                while (active.get()) {
                  int n = record.read(buf, 0, buf.length);
                  if (n <= 0) {
                    continue;
                  }
                  byte[] payload =
                      acoustic ? detectAcoustic(buf, n) : detectAfsk(buf, n);
                  if (payload != null) {
                    active.set(false);
                    handler.removeCallbacks(windowTimeout);
                    stopReceiverOnly();
                    listener.onBeacon(payload);
                    return;
                  }
                }
              } catch (Exception e) {
                Log.w(TAG, "rx failed", e);
              } finally {
                stopReceiverOnly();
              }
            });
    rxThread.setDaemon(true);
    rxThread.start();
  }

  /** Goertzel energy check on the acoustic bin, repeated for a stable lock. */
  private int acousticHitStreak = 0;

  private byte[] detectAcoustic(short[] buf, int n) {
    double energy = goertzel(buf, n, ACOUSTIC_HZ);
    double total = 0;
    for (int i = 0; i < n; i++) {
      total += (double) buf[i] * buf[i];
    }
    double ratio = total <= 0 ? 0 : energy / total;
    if (ratio > 0.35) {
      acousticHitStreak++;
      if (acousticHitStreak >= 3) {
        acousticHitStreak = 0;
        return new byte[] {1};
      }
    } else {
      acousticHitStreak = 0;
    }
    return null;
  }

  /** Very small AFSK demodulator: alternates between mark/space, 1 bit per symbol. */
  private final StringBuilder afskBits = new StringBuilder();
  private int afskStreak = 0;

  private byte[] detectAfsk(short[] buf, int n) {
    double mark = goertzel(buf, n, AFSK_MARK_HZ);
    double space = goertzel(buf, n, AFSK_SPACE_HZ);
    if (Math.max(mark, space) < 1e6) {
      return null;
    }
    afskBits.append(mark > space ? '1' : '0');
    if (afskBits.length() >= 16) {
      afskStreak++;
      afskBits.setLength(0);
      if (afskStreak >= 2) {
        afskStreak = 0;
        return new byte[] {2};
      }
    }
    return null;
  }

  private double goertzel(short[] samples, int n, double targetHz) {
    double w = 2.0 * Math.PI * targetHz / SAMPLE_RATE;
    double coeff = 2.0 * Math.cos(w);
    double s0 = 0, s1 = 0, s2 = 0;
    for (int i = 0; i < n; i++) {
      s0 = samples[i] + coeff * s1 - s2;
      s2 = s1;
      s1 = s0;
    }
    return s1 * s1 + s2 * s2 - coeff * s1 * s2;
  }

  private void playToneBurst(final double hz, final int ms) {
    Thread t =
        new Thread(
            () -> {
              try {
                int count = (int) (SAMPLE_RATE * (ms / 1000.0));
                short[] samples = new short[count];
                for (int i = 0; i < count; i++) {
                  double env = Math.min(1.0, Math.min(i, count - i) / (SAMPLE_RATE * 0.01));
                  samples[i] = (short) (Math.sin(2.0 * Math.PI * hz * i / SAMPLE_RATE) * 16000 * env);
                }
                toneTrack =
                    new AudioTrack(
                        AudioManager.STREAM_VOICE_CALL,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        samples.length * 2,
                        AudioTrack.MODE_STATIC);
                toneTrack.write(samples, 0, samples.length);
                toneTrack.play();
                Thread.sleep(ms + 100);
                toneTrack.stop();
                toneTrack.release();
                toneTrack = null;
              } catch (Exception e) {
                Log.w(TAG, "tone failed", e);
              }
            });
    t.setDaemon(true);
    t.start();
  }

  private void transmitAfsk(byte[] payload) {
    Thread t =
        new Thread(
            () -> {
              try {
                StringBuilder bits = new StringBuilder();
                for (byte b : payload) {
                  for (int i = 7; i >= 0; i--) {
                    bits.append(((b >> i) & 1) == 1 ? '1' : '0');
                  }
                }
                int symSamples = (int) (SAMPLE_RATE * (SYMBOL_MS / 1000.0));
                short[] out = new short[bits.length() * symSamples];
                int idx = 0;
                for (int i = 0; i < bits.length(); i++) {
                  double hz = bits.charAt(i) == '1' ? AFSK_MARK_HZ : AFSK_SPACE_HZ;
                  for (int s = 0; s < symSamples; s++) {
                    out[idx++] = (short) (Math.sin(2.0 * Math.PI * hz * s / SAMPLE_RATE) * 12000);
                  }
                }
                AudioTrack track =
                    new AudioTrack(
                        AudioManager.STREAM_VOICE_CALL,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        out.length * 2,
                        AudioTrack.MODE_STATIC);
                track.write(out, 0, out.length);
                track.play();
                Thread.sleep((long) (bits.length() * SYMBOL_MS) + 100);
                track.stop();
                track.release();
              } catch (Exception e) {
                Log.w(TAG, "afsk tx failed", e);
              }
            });
    t.setDaemon(true);
    t.start();
  }

  private void stopReceiverOnly() {
    try {
      if (record != null) {
        try {
          record.stop();
        } catch (Exception ignored) {
        }
        record.release();
        record = null;
      }
    } catch (Exception ignored) {
    }
  }

  public void stop() {
    active.set(false);
    if (windowTimeout != null) {
      handler.removeCallbacks(windowTimeout);
      windowTimeout = null;
    }
    stopReceiverOnly();
    try {
      if (toneTrack != null) {
        toneTrack.stop();
        toneTrack.release();
        toneTrack = null;
      }
    } catch (Exception ignored) {
    }
  }
}
