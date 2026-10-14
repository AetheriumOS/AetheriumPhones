package net.aetherium.phones.phone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.aetherium.phones.R
import net.aetherium.phones.databinding.FragmentContactsBinding

class ContactsFragment : Fragment() {
    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private val all = mutableListOf<Contact>()
    private lateinit var adapter: ContactAdapter

    data class Contact(val name: String, val number: String)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ContactAdapter { c ->
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${c.number}"))
            startActivity(intent)
        }
        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        loadContacts()
    }

    private fun loadContacts() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            binding.tvEmpty.text = getString(R.string.no_contacts)
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }
        val list = mutableListOf<Contact>()
        val seen = HashSet<String>()
        val cursor = requireContext().contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: continue
                val num = it.getString(numIdx) ?: continue
                val key = "$name|$num"
                if (seen.add(key)) list.add(Contact(name, num))
            }
        }
        all.clear()
        all.addAll(list)
        adapter.submit(all)
        binding.tvEmpty.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun filter(q: String) {
        if (q.isBlank()) {
            adapter.submit(all)
        } else {
            adapter.submit(all.filter { it.name.contains(q, true) || it.number.contains(q) })
        }
        binding.tvEmpty.visibility = if (all.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class ContactAdapter(private val onClick: (ContactsFragment.Contact) -> Unit) :
    RecyclerView.Adapter<ContactAdapter.VH>() {

    private val items = mutableListOf<ContactsFragment.Contact>()

    fun submit(list: List<ContactsFragment.Contact>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.tvAvatar)
        val name: TextView = view.findViewById(R.id.tvContactName)
        val number: TextView = view.findViewById(R.id.tvContactNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.name.text = c.name
        holder.number.text = c.number
        holder.avatar.text = c.name.trim().firstOrNull()?.uppercase() ?: "?"
        holder.itemView.setOnClickListener { onClick(c) }
    }

    override fun getItemCount(): Int = items.size
}
