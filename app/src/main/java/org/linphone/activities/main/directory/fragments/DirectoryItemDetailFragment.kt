package org.linphone.activities.main.directory.fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.navGraphViewModels
import org.linphone.R
import org.linphone.activities.GenericFragment
import org.linphone.activities.main.directory.viewmodels.DirectoryItemDetailViewModel
import org.linphone.databinding.DirectoryItemDetailBinding

class DirectoryItemDetailFragment : GenericFragment<DirectoryItemDetailBinding>() {
    private val viewModel: DirectoryItemDetailViewModel by navGraphViewModels(R.id.directoryItemDetailFragment)
    override fun getLayoutId(): Int = R.layout.directory_item_detail

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel
        binding.viewModel = viewModel
        if (sharedViewModel.selectedDirectoryItem.value != null)
            viewModel.setItem(sharedViewModel.selectedDirectoryItem.value!!)
    }
}
