package org.linphone.activities.main.directory.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import androidx.lifecycle.lifecycleScope
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
import org.linphone.core.Account
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.RegistrationState
import org.linphone.databinding.DirectoryFragmentBinding

class DirectoryFragment : GenericFragment<DirectoryFragmentBinding>() {
    private val viewModel: DirectoryViewModel by navGraphViewModels(R.id.directoryFragment)
    private lateinit var adapter: DirectoryAdapter
    private lateinit var searchField: EditText

    // dms: the directory is emptied when the account is disabled and reloaded when it registers again
    private val coreListener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            if (state == RegistrationState.Ok || state == RegistrationState.Cleared) {
                // Dispatched after the CoreContext listener, which updates the directory cache
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                    viewModel.updateDirectory(coreContext.fetchDirectory(searchField.text.toString().trim()))
                }
            }
        }
    }

    override fun getLayoutId(): Int = R.layout.directory_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel
        binding.viewModel = viewModel

        searchField = view.findViewById(R.id.searchField)
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

        coreContext.core.addListener(coreListener)

        viewModel.isLoading.observe(
            viewLifecycleOwner,
            { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        )
    }

    override fun onDestroyView() {
        coreContext.core.removeListener(coreListener)
        super.onDestroyView()
    }
}
