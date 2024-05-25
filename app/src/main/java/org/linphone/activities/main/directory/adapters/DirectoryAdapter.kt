package org.linphone.activities.main.directory.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import org.linphone.R
import org.linphone.activities.main.viewmodels.SharedMainViewModel
import org.linphone.bcsws.DirectoryItem

class DirectoryAdapter(
    private val sharedViewModel: SharedMainViewModel,
    private val context: Context
) : RecyclerView.Adapter<DirectoryAdapter.DirectoryViewHolder>() {

    private var items: List<DirectoryItem> = listOf()

// ViewHolder per l'adapter
    class DirectoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val displayName: TextView = itemView.findViewById(R.id.directoryItemDisplayName)
        val organization: TextView = itemView.findViewById(R.id.directoryItemOrganization)
    }

    // Metodo per creare nuovi ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DirectoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.directory_item, parent, false)

        return DirectoryViewHolder(view)
    }

    // Metodo per legare i dati al ViewHolder
    override fun onBindViewHolder(holder: DirectoryViewHolder, position: Int) {
        val item = items[position]

        holder.itemView.setOnClickListener {
            sharedViewModel.selectedDirectoryItem.postValue(item)
            val navController = holder.itemView.findNavController()
            navController.navigate(R.id.action_directoryFragment_to_directoryItemDetailFragment)
        }

        holder.displayName.text = when {
            item.DisplayName.isNotEmpty() -> item.DisplayName
            item.Name.isNotEmpty() || item.Surname.isNotEmpty() -> "${item.Name} ${item.Surname}"
            item.Name.isNotEmpty() || item.Surname.isNotEmpty() -> "${item.Name} ${item.Surname}"
            else -> item.Uri
        }

        holder.organization.text = when {
            item.Company.isNotEmpty() -> item.Company
            else -> ""
        }
        holder.organization.visibility = when {
            item.Company.isNotEmpty() -> View.VISIBLE
            else -> View.GONE
        }
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
