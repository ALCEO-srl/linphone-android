package org.linphone.activities.main.directory.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.linphone.R
import org.linphone.bcsws.DirectoryItem

class DirectoryAdapter : RecyclerView.Adapter<DirectoryAdapter.DirectoryViewHolder>() {

    private var items: List<DirectoryItem> = listOf()

// ViewHolder per l'adapter
    class DirectoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val displayName: TextView = itemView.findViewById(R.id.displayName)
    }

    // Metodo per creare nuovi ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.directory_item, parent, false)
        return DirectoryViewHolder(view)
    }

    // Metodo per legare i dati al ViewHolder
    override fun onBindViewHolder(holder: DirectoryViewHolder, position: Int) {
        val item = items[position]
        holder.displayName.text = item.DisplayName
    }

    // Metodo per ottenere il numero di elementi nella lista
    override fun getItemCount(): Int {
        return items.size
    }

    // Metodo per aggiornare la lista degli elementi e notificare l'adapter del cambiamento
    fun setItems(newItems: List<DirectoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
