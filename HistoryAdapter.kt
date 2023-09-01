package com.example.example.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.sblink.R

class HistoryAdapter(
    private val items: MutableList<Triple<String, String, String>>,
    private val onDelete: (position: Int) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View, onDelete: (position: Int) -> Unit) : RecyclerView.ViewHolder(view) {
        val idTextView: TextView = view.findViewById(R.id.id_text_view)
        val timeTextView: TextView = view.findViewById(R.id.time_text_view)
        val fileNameTextView: TextView = view.findViewById(R.id.file_name_text_view)
        val copyButton: ImageButton = view.findViewById(R.id.copy_button)

        init {
            view.setOnLongClickListener {
                AlertDialog.Builder(view.context)
                    .setTitle("Delete ID")
                    .setMessage("Are you sure you want to delete this ID?")
                    .setPositiveButton("Yes") { _, _ ->
                        onDelete(adapterPosition)
                    }
                    .setNegativeButton("No", null)
                    .show()
                true
            }

            copyButton.setOnClickListener {
                val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("ID", idTextView.text)
                clipboard?.setPrimaryClip(clip)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return ViewHolder(view, onDelete)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.idTextView.text = item.first
        holder.timeTextView.text = item.second
        holder.fileNameTextView.text = item.third
    }

    override fun getItemCount(): Int = items.size
}
