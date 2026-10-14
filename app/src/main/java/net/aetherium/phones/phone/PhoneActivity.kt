package net.aetherium.phones.phone

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import net.aetherium.phones.R
import net.aetherium.phones.databinding.ActivityPhoneBinding
import net.aetherium.phones.pairing.PairingManager

class PhoneActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhoneBinding
    private val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        )
    } else {
        arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS
        )
    }
    private val reqPerms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhoneBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarPhone)
        requestNeeded()
        binding.btnCall.setOnClickListener {
            val num = binding.etNumber.text.toString().trim()
            if (num.isEmpty()) return@setOnClickListener
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$num"))
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_phone, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean = when (item.itemId) {
        R.id.action_pair_code -> {
            val code = PairingManager(this).generateCode6()
            AlertDialog.Builder(this)
                .setTitle(R.string.pairing_show_code)
                .setMessage(getString(R.string.pairing_generate) + ": $code\n\nShare this with other Aetherium Phones to pair (out‑of‑band).")
                .setPositiveButton(android.R.string.ok, null)
                .show()
            true
        }
        R.id.action_enter_code -> {
            val input = android.widget.EditText(this)
            input.hint = getString(R.string.pairing_code_hint)
            AlertDialog.Builder(this)
                .setTitle(R.string.pairing_enter_code)
                .setView(input)
                .setPositiveButton(R.string.pairing_confirm) { _, _ ->
                    val c = input.text.toString().trim()
                    Toast.makeText(this, "[STUB] Entered code: $c (pairing logic wired later)", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            true
        }
        R.id.action_phys_pair -> {
            val pm = PairingManager(this)
            val opts = arrayOf(getString(R.string.phys_pair_gen), getString(R.string.phys_pair_enter))
            AlertDialog.Builder(this)
                .setTitle(R.string.phys_pair_title)
                .setItems(opts) { _, i ->
                    if (i == 0) {
                        val c60 = pm.generateCode60()
                        AlertDialog.Builder(this)
                            .setTitle(R.string.phys_pair_gen)
                            .setMessage("60‑char code:\n\n$c60\n\nUse \"Kod Gir\" on the other device (per‑number).")
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    } else {
                        val et = android.widget.EditText(this)
                        et.hint = "Enter 60‑character code"
                        et.maxLines = 2
                        AlertDialog.Builder(this)
                            .setTitle(R.string.phys_pair_enter)
                            .setView(et)
                            .setPositiveButton(R.string.pairing_confirm) { _, _ ->
                                val v = et.text.toString().trim()
                                if (v.length == 60) {
                                    Toast.makeText(this, R.string.pairing_success, Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Code must be 60 characters", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
                .show()
            true
        }
        R.id.action_contacts -> {
            startActivity(Intent(this, net.aetherium.phones.contacts.ContactsActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun requestNeeded() {
        val need = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) reqPerms.launch(need.toTypedArray())
    }
}
