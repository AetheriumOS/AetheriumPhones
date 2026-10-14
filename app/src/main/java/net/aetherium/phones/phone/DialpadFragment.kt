package net.aetherium.phones.phone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import net.aetherium.phones.databinding.FragmentDialpadBinding

class DialpadFragment : Fragment() {
    private var _binding: FragmentDialpadBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDialpadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = DialpadAdapter(
            onKey = { digit ->
                binding.etNumber.append(digit)
            },
            onBackspace = {
                val text = binding.etNumber.text
                if (text.isNotEmpty()) text.delete(text.length - 1, text.length)
            }
        )
        binding.dialpadRecycler.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.dialpadRecycler.adapter = adapter

        binding.btnCall.setOnClickListener {
            val num = binding.etNumber.text.toString().trim()
            if (num.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$num"))
                startActivity(intent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
