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
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

/**
 * Client for the Aetherium one-time secure code exchange.
 *
 * <p>{@code GET https://api.aetheriumos.com/phone/codesetup?ownid=<own>&accountid=<peer>} returns a
 * 60 character code that lets both sides derive the same session key. The code lives for one minute
 * and is destroyed after it has been retrieved twice.
 */
public final class AetheriumApi {

  public static final String BASE_URL = "https://api.aetheriumos.com";

  private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

  private AetheriumApi() {}

  public static boolean isInternetAvailable(Context ctx) {
    ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (cm == null) {
      return false;
    }
    Network net = cm.getActiveNetwork();
    if (net == null) {
      return false;
    }
    NetworkCapabilities caps = cm.getNetworkCapabilities(net);
    if (caps == null) {
      return false;
    }
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
  }

  public interface CodeCallback {
    void onResult(CodeResult result);
  }

  public static final class CodeResult {
    public final boolean ok;
    public final String code;
    public final long expiresInMs;
    public final int retrievedCount;
    public final String error;

    public CodeResult(boolean ok, String code, long expiresInMs, int retrievedCount, String error) {
      this.ok = ok;
      this.code = code;
      this.expiresInMs = expiresInMs;
      this.retrievedCount = retrievedCount;
      this.error = error;
    }
  }

  /** Requests a one-time 60 character code for the given peer. */
  public static void phoneCodeSetup(String ownId, String peerId, CodeCallback cb) {
    EXECUTOR.execute(
        () -> {
          HttpURLConnection conn = null;
          try {
            String url =
                BASE_URL
                    + "/phone/codesetup?ownid="
                    + URLEncoder.encode(ownId == null ? "" : ownId, "UTF-8")
                    + "&accountid="
                    + URLEncoder.encode(peerId == null ? "" : peerId, "UTF-8");
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            int responseCode = conn.getResponseCode();
            boolean isError = responseCode >= 400;
            BufferedReader br =
                new BufferedReader(
                    new InputStreamReader(isError ? conn.getErrorStream() : conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
              sb.append(line);
            }
            br.close();
            if (isError) {
              cb.onResult(new CodeResult(false, null, 0, 0, sb.toString()));
              return;
            }
            JSONObject json = new JSONObject(sb.toString());
            cb.onResult(
                new CodeResult(
                    json.optBoolean("ok", false),
                    json.optString("code", null),
                    json.optLong("expires_in_ms", 0L),
                    json.optInt("retrieved_count", 0),
                    null));
          } catch (Exception e) {
            cb.onResult(new CodeResult(false, null, 0, 0, e.getMessage()));
          } finally {
            if (conn != null) {
              conn.disconnect();
            }
          }
        });
  }
}
