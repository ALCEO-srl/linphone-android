package org.linphone.activities.main.directory.fragments

import android.os.Bundle
import android.view.View
import androidx.navigation.navGraphViewModels
import org.linphone.LinphoneApplication
import org.linphone.R
import org.linphone.activities.GenericFragment
import org.linphone.activities.findMasterNavController
import org.linphone.activities.main.directory.data.DirectoryItemDataDestinationClickListener
import org.linphone.activities.main.directory.viewmodels.DirectoryItemDetailViewModel
import org.linphone.activities.popupTo
import org.linphone.databinding.DirectoryItemDetailBinding
import org.linphone.utils.Event

class DirectoryItemDetailFragment : GenericFragment<DirectoryItemDetailBinding>() {
    private val viewModel: DirectoryItemDetailViewModel by navGraphViewModels(R.id.directoryItemDetailFragment)
    override fun getLayoutId(): Int = R.layout.directory_item_detail

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        binding.sharedMainViewModel = sharedViewModel
        binding.viewModel = viewModel
        if (sharedViewModel.selectedDirectoryItem.value != null)
            viewModel.setItem(sharedViewModel.selectedDirectoryItem.value!!, listener)
    }
    private val listener = object : DirectoryItemDataDestinationClickListener {
        override fun onCall(destination: String) {
            if (LinphoneApplication.coreContext.core.callsNb > 0) {
                org.linphone.core.tools.Log.i("[Contact] Starting dialer with pre-filled URI $destination, is transfer? ${sharedViewModel.pendingCallTransfer}")
                sharedViewModel.updateContactsAnimationsBasedOnDestination.value =
                    Event(R.id.dialerFragment)
                sharedViewModel.updateDialerAnimationsBasedOnDestination.value =
                    Event(R.id.masterContactsFragment)

                val args = Bundle()
                args.putString("URI", destination)
                args.putBoolean("Transfer", sharedViewModel.pendingCallTransfer)
                args.putBoolean(
                    "SkipAutoCallStart",
                    true
                ) // If auto start call setting is enabled, ignore it
                navigateToDialer(args)
            } else {
                LinphoneApplication.coreContext.startCall(destination)
            }
        }

        override fun onChat(destination: String) {
            if (LinphoneApplication.coreContext.lastAccountIdRegistered == null) return
            val args = Bundle()
            args.putString("LocalSipUri", LinphoneApplication.coreContext.lastAccountIdRegistered)
            args.putString("RemoteSipUri", destination)
            navigateToChatRoom(args)
        }
    }
    private fun DirectoryItemDetailFragment.navigateToChatRoom(args: Bundle?) {
        findMasterNavController().navigate(
            R.id.action_global_masterChatRoomsFragment,
            args,
            popupTo(R.id.masterChatRoomsFragment, true)
        )
    }

    private fun DirectoryItemDetailFragment.navigateToDialer(args: Bundle?) {
        findMasterNavController().navigate(
            R.id.action_global_dialerFragment,
            args,
            popupTo(R.id.dialerFragment, true)
        )
    }
}
