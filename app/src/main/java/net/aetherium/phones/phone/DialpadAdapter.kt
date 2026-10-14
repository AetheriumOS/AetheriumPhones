package net.aetherium.phones.phone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.aetherium.phones.R

class DialpadAdapter(
    private val onKey: (String) -> Unit,
    private val onBackspace: () -> Unit
) : RecyclerView.Adapter<DialpadAdapter.KeyHolder>() {

    private data class Key(val digit: String, val letters: String)

    private val keys = listOf(
        Key("1", ""),
        Key("2", "ABC"),
        Key("3", "DEF"),
        Key("4", "GHI"),
        Key("5", "JKL"),
        Key("6", "MNO"),
        Key("7", "PQRS"),
        Key("8", "TUV"),
        Key("9", "WXYZ"),
        Key("*", ""),
        Key("0", "+"),
        Key("#", "")
    )

    class KeyHolder(view: View) : RecyclerView.ViewHolder(view) {
        val digit: TextView = view.findViewById(R.id.tvDigit)
        val letters: TextView = view.findViewById(R.id.tvLetters)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_dialpad_key, parent, false)
        return KeyHolder(v)
    }

    override fun onBindViewHolder(holder: KeyHolder, position: Int) {
        val k = keys[position]
        holder.digit.text = k.digit
        holder.letters.text = k.letters
        holder.itemView.setOnClickListener { onKey(k.digit) }
    }

    override fun getItemCount(): Int = keys.size
}
