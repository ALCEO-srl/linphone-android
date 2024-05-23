package org.linphone.activities.main.directory.fragments

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.linphone.R
import org.linphone.activities.GenericFragment
import org.linphone.activities.main.directory.adapters.DirectoryAdapter
import org.linphone.activities.main.directory.viewmodels.DirectoryViewModel
import org.linphone.databinding.DirectoryFragmentBinding

class DirectoryFragment : GenericFragment<DirectoryFragmentBinding>() {
    private val viewModel: DirectoryViewModel by navGraphViewModels(R.id.directoryFragment)
    private lateinit var adapter: DirectoryAdapter

    override fun getLayoutId(): Int = R.layout.directory_fragment

   /* override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.directory_fragment, container, false)
    }*/

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner

        binding.viewModel = viewModel

        val searchField: EditText = view.findViewById(R.id.searchField)
        val searchButton: Button = view.findViewById(R.id.searchButton)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

        adapter = DirectoryAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

         searchButton.setOnClickListener {
            val query = searchField.text.toString()
            searchField.setText("")
            viewModel.searchDirectory(query)
        }

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
