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

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.fissy.dialer.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/** Entry point for Aetherium secure calling: pairing, status and a transport self test. */
public final class AetheriumSecureActivity extends AppCompatActivity {

  private AetheriumPairing pairing;
  private TextInputEditText codeInput;
  private TextView statusTitle;
  private TextView statusSubtitle;
  private TextView transportResult;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_aetherium_secure);

    pairing = new AetheriumPairing(this);
    codeInput = findViewById(R.id.aetherium_code_input);
    statusTitle = findViewById(R.id.aetherium_status_title);
    statusSubtitle = findViewById(R.id.aetherium_status_subtitle);
    transportResult = findViewById(R.id.aetherium_transport_result);

    findViewById(R.id.aetherium_toolbar).setOnClickListener(v -> finish());
    ((androidx.appcompat.widget.Toolbar) findViewById(R.id.aetherium_toolbar))
        .setNavigationOnClickListener(v -> finish());

    String ownId = AetheriumAccount.getOwnAccountId(this);
    TextView accountView = findViewById(R.id.aetherium_account_id);
    accountView.setText(
        ownId == null
            ? getString(R.string.aetherium_account_missing)
            : getString(R.string.aetherium_account_id, ownId));

    updateStatus(ownId != null);

    MaterialButton gen6 = findViewById(R.id.aetherium_generate_6);
    MaterialButton gen60 = findViewById(R.id.aetherium_generate_60);
    MaterialButton pair = findViewById(R.id.aetherium_pair_button);
    MaterialButton test = findViewById(R.id.aetherium_test_button);

    gen6.setOnClickListener(
        v -> {
          String code = pairing.generateCode6();
          codeInput.setText(code);
          toast(getString(R.string.aetherium_code_generated_6, code));
        });

    gen60.setOnClickListener(
        v -> {
          String code = pairing.generateCode60();
          codeInput.setText(code);
          toast(getString(R.string.aetherium_code_generated_60));
        });

    pair.setOnClickListener(
        v -> {
          String code = codeInput.getText() == null ? "" : codeInput.getText().toString().trim();
          if (TextUtils.isEmpty(code)) {
            toast(getString(R.string.aetherium_enter_code));
            return;
          }
          // Any code entered stores a physical pairing under a local pseudo peer id.
          pairing.storePhysicalPairCode(code, code);
          toast(getString(R.string.aetherium_pair_saved));
        });

    test.setOnClickListener(v -> runTransportTest());
  }

  private void updateStatus(boolean signedIn) {
    if (signedIn) {
      statusTitle.setText(R.string.aetherium_status_ready);
      statusSubtitle.setText(R.string.aetherium_status_ready_desc);
    } else {
      statusTitle.setText(R.string.aetherium_status_offline);
      statusSubtitle.setText(R.string.aetherium_status_offline_desc);
    }
  }

  private void runTransportTest() {
    transportResult.setText(R.string.aetherium_transport_searching);
    HybridDetector detector =
        new HybridDetector(
            transport -> {
              int res =
                  transport == HybridDetector.Transport.ACOUSTIC
                      ? R.string.aetherium_transport_acoustic
                      : transport == HybridDetector.Transport.DATA_OVER_VOICE
                          ? R.string.aetherium_transport_dov
                          : R.string.aetherium_transport_voip;
              transportResult.setText(res);
            });
    detector.transmitBeacon(new byte[] {0x41});
    detector.start();
  }

  private void toast(String msg) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
  }
}
