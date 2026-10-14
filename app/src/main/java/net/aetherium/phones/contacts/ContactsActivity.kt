package net.aetherium.phones.contacts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.aetherium.phones.R
import net.aetherium.phones.databinding.ActivityContactsBinding
import net.aetherium.phones.phone.ContactsFragment

class ContactsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarContacts)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarContacts.setNavigationOnClickListener { finish() }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.contactsContainer, ContactsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
