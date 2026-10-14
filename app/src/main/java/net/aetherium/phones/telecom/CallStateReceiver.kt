package net.aetherium.phones.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Stub – could listen to TelephonyManager.CALL_STATE_* events and trigger Hybrid detection.
    }
}
