package net.aetherium.phones.phone

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.aetherium.phones.R
import net.aetherium.phones.databinding.ActivityIncallBinding

class InCallActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIncallBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val number = intent.getStringExtra("number") ?: getString(R.string.unknown_number)
        binding.tvCallee.text = number
        binding.btnEndCall.setOnClickListener { finish() }
        // Pair button will open physical pair dialog – handled in PhoneActivity via shared PairingManager
    }
}
