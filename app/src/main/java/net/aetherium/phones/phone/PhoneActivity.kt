package net.aetherium.phones.phone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import net.aetherium.phones.R
import net.aetherium.phones.contacts.ContactsActivity
import net.aetherium.phones.databinding.ActivityPhoneBinding
import net.aetherium.phones.pairing.PairingManager

class PhoneActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhoneBinding
    private lateinit var pagerAdapter: PhonePagerAdapter

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
        setupViewPager()
    }

    private fun setupViewPager() {
        pagerAdapter = PhonePagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNav.selectedItemId = when (position) {
                    0 -> R.id.nav_dialpad
                    1 -> R.id.nav_contacts
                    2 -> R.id.nav_recents
                    else -> R.id.nav_dialpad
                }
            }
        })

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dialpad -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.nav_contacts -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                R.id.nav_recents -> {
                    binding.viewPager.currentItem = 2
                    true
                }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_phone, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_pair_code -> {
            val code = PairingManager(this).generateCode6()
            AlertDialog.Builder(this)
                .setTitle(R.string.pairing_show_code)
                .setMessage(getString(R.string.pairing_generate) + ": $code")
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
                    Toast.makeText(this, R.string.pairing_success, Toast.LENGTH_SHORT).show()
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
                            .setMessage(c60)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()
                    } else {
                        val et = android.widget.EditText(this)
                        et.maxLines = 2
                        AlertDialog.Builder(this)
                            .setTitle(R.string.phys_pair_enter)
                            .setView(et)
                            .setPositiveButton(R.string.pairing_confirm) { _, _ ->
                                Toast.makeText(this, R.string.pairing_success, Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    }
                }
                .show()
            true
        }
        R.id.action_contacts -> {
            startActivity(Intent(this, ContactsActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun requestNeeded() {
        val need = perms.filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) reqPerms.launch(need.toTypedArray())
    }
}
