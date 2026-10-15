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

import android.app.Activity;
import android.os.Bundle;

import com.fissy.dialer.main.impl.MainActivity;
import com.fissy.dialer.main.impl.OldMainActivityPeer;
import com.fissy.dialer.main.impl.bottomnav.BottomNavBar;

/**
 * Second launcher entry ("Contacts") for Aetherium Phone.
 *
 * <p>An {@code <activity-alias>} cannot pass extras to its target, so this thin, invisible
 * trampoline forwards to {@link MainActivity} using the existing "show tab" contract and then
 * removes itself. MainActivity is {@code singleTask}, so an already running instance is reused and
 * the request is delivered through {@code onNewIntent}.
 */
public final class AetheriumContactsAlias extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    startActivity(OldMainActivityPeer.getShowTabIntent(this, BottomNavBar.TabIndex.CONTACTS));
    finish();
  }
}
