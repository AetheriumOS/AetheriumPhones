package net.aetherium.phones.phone

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.aetherium.phones.R
import net.aetherium.phones.databinding.FragmentRecentsBinding

class RecentsFragment : Fragment() {
    private var _binding: FragmentRecentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecentAdapter

    data class Call(val name: String, val number: String, val type: Int, val date: Long)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRecentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = RecentAdapter { c ->
            if (c.number.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:${c.number}")))
            }
        }
        binding.rvRecents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecents.adapter = adapter
        loadRecents()
    }

    private fun loadRecents() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED) {
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }
        val list = mutableListOf<Call>()
        val cursor = requireContext().contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            null, null,
            CallLog.Calls.DATE + " DESC"
        )
        cursor?.use {
            val nIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
            val tIdx = it.getColumnIndex(CallLog.Calls.TYPE)
            val dIdx = it.getColumnIndex(CallLog.Calls.DATE)
            while (it.moveToNext()) {
                val name = if (nIdx >= 0) it.getString(nIdx) else null
                val num = if (numIdx >= 0) it.getString(numIdx) else ""
                val type = if (tIdx >= 0) it.getInt(tIdx) else 0
                val date = if (dIdx >= 0) it.getLong(dIdx) else 0L
                list.add(Call(name ?: num ?: "", num ?: "", type, date))
            }
        }
        adapter.submit(list)
        binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class RecentAdapter(private val onClick: (RecentsFragment.Call) -> Unit) :
    RecyclerView.Adapter<RecentAdapter.VH>() {

    private val items = mutableListOf<RecentsFragment.Call>()

    fun submit(list: List<RecentsFragment.Call>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvRecentName)
        val meta: TextView = view.findViewById(R.id.tvRecentMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_recent, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.name.text = c.name.ifBlank { c.number }
        val typeStr = when (c.type) {
            CallLog.Calls.INCOMING_TYPE -> "↘ Incoming"
            CallLog.Calls.OUTGOING_TYPE -> "↗ Outgoing"
            CallLog.Calls.MISSED_TYPE -> "✖ Missed"
            else -> ""
        }
        holder.meta.text = "$typeStr  ·  ${c.number}"
        holder.itemView.setOnClickListener { onClick(c) }
    }

    override fun getItemCount(): Int = items.size
}
