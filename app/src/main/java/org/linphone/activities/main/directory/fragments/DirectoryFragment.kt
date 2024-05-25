package org.linphone.activities.main.directory.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.R
import org.linphone.activities.GenericFragment
import org.linphone.activities.main.directory.adapters.DirectoryAdapter
import org.linphone.activities.main.directory.viewmodels.DirectoryViewModel
import org.linphone.databinding.DirectoryFragmentBinding

class DirectoryFragment : GenericFragment<DirectoryFragmentBinding>() {
    private val viewModel: DirectoryViewModel by navGraphViewModels(R.id.directoryFragment)
    private lateinit var adapter: DirectoryAdapter

    override fun getLayoutId(): Int = R.layout.directory_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel
        binding.viewModel = viewModel

        val searchField: EditText = view.findViewById(R.id.searchField)
        searchField.setText("")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                viewModel.updateDirectory(coreContext.fetchDirectory(""))
                // Usa il risultato come necessario
            } catch (e: Exception) {
                Log.e("Error", "Failed to fetch directory: ${e.message}")
            }
        }

        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Non è necessario fare nulla prima della modifica del testo
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Qui puoi chiamare la tua funzione di ricerca ogni volta che il testo cambia
                // Assicurati di gestire il caso in cui il testo è vuoto per ripristinare lo stato precedente
                val query = s.toString().trim()

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        viewModel.updateDirectory(coreContext.fetchDirectory(query))
                        // Usa il risultato come necessario
                    } catch (e: Exception) {
                        Log.e("Error", "Failed to fetch directory: ${e.message}")
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Non è necessario fare nulla dopo la modifica del testo
            }
        })

        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

        adapter = DirectoryAdapter(sharedViewModel, requireContext())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewModel.directory.observe(
            viewLifecycleOwner,
            { directory ->
                adapter.setItems(directory)
                recyclerView.visibility = if (directory.isNotEmpty()) View.VISIBLE else View.GONE
            }
        )

        viewModel.isLoading.observe(
            viewLifecycleOwner,
            { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        )
    }
}
