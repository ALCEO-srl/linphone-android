/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.activities.main.contact.viewmodels

import android.content.ContentProviderOperation
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.*
import kotlinx.coroutines.*
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.LinphoneApplication.Companion.corePreferences
import org.linphone.contact.ContactsUpdatedListenerStub
import org.linphone.core.*
import org.linphone.core.tools.Log
import org.linphone.utils.Event

class ContactsListViewModel : ViewModel() {
    val sipContactsSelected = MutableLiveData<Boolean>()

    val contactsList = MutableLiveData<ArrayList<ContactViewModel>>()

    val nativeAddressBookEnabled = MutableLiveData<Boolean>()

    val fetchInProgress = MutableLiveData<Boolean>()
    private var searchResultsPending: Boolean = false
    private var fastFetchJob: Job? = null

    val filter = MutableLiveData<String>()
    private var previousFilter = "NotSet"

    val moreResultsAvailableEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    private val contactsUpdatedListener = object : ContactsUpdatedListenerStub() {
        override fun onContactsUpdated() {
            Log.i("[Contacts] Contacts have changed")
            updateContactsList(true)

            // dms begin ************
            Log.w("[ContactsList.onRegistrationStateChanged] Refreshing subscriptions")
            coreContext.enableSubscriptions()
            // dms end ************
        }
    }

    private val magicSearchListener = object : MagicSearchListenerStub() {
        override fun onSearchResultsReceived(magicSearch: MagicSearch) {
            Log.i("[Contacts] Magic search contacts available")
            searchResultsPending = false
            processMagicSearchResults(magicSearch.lastSearch)
            // Use coreContext.contactsManager.fetchInProgress instead of false in case contacts are still being loaded
            fetchInProgress.value = coreContext.contactsManager.fetchInProgress.value
        }

        override fun onLdapHaveMoreResults(magicSearch: MagicSearch, ldap: Ldap) {
            moreResultsAvailableEvent.value = Event(true)
        }
    }

    // dms begin ************

    // private var timer: Timer? = null

    private val coreListener = object : CoreListenerStub() {

        @Deprecated("Deprecated in Java")
        override fun onRegistrationStateChanged(
            core: Core,
            cfg: ProxyConfig,
            state: RegistrationState,
            message: String
        ) {
            Log.w("[ContactsList.onRegistrationStateChanged]  " + message)
            if (state == RegistrationState.Ok) {
                Log.w("[ContactsList.onRegistrationStateChanged] Refreshing subscriptions")
                // coreContext.enableSubscriptions()
            }
        }

        override fun onNotifyPresenceReceived(core: Core, linphoneFriend: Friend) {
            Log.w("[ContactsList.onNotifyPresenceReceived] PresenceReceived for user=" + linphoneFriend.address?.asStringUriOnly())
            Log.w("[ContactsList.onNotifyPresenceReceived] Searching in contacts list")
            contactsList.value.orEmpty().forEach {

                if (it.contact.value?.address?.asStringUriOnly() == linphoneFriend.address?.asStringUriOnly()) {
                    Log.w("[ContactsList.onNotifyPresenceReceived] Updating presence for user=" + it.contact.value?.address?.asStringUriOnly())
                    it.updatePresenceIconRes()
                }
            }
        }
    }

    // dms end ************

    init {

        // dms begin ************
        coreContext.core.addListener(coreListener)

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(
            {
                // First we try to start/refresh the subscription if needed
                Log.w("[ContactsList.init] Refreshing subscriptions")
                // dms coreContext.core.enableSubscriptions()

                // Then we update the presence icon res for each contact viewmodel
                Log.w("[ContactsList.init] Initializing users presence icon")
                contactsList.value.orEmpty().forEach {
                    it.updatePresenceIconRes()
                    Log.w("[ContactsList.init] Initializing user=" + it.contact.value?.address?.asStringUriOnly())
                }
            },
            1000
        )

        sipContactsSelected.value = true // dms  coreContext.contactsManager.shouldDisplaySipContactsList()
        nativeAddressBookEnabled.value = corePreferences.enableNativeAddressBookIntegration

        coreContext.contactsManager.addListener(contactsUpdatedListener)
        coreContext.contactsManager.magicSearch.addListener(magicSearchListener)
    }

    override fun onCleared() {
        // dms begin *********************

        coreContext.core.removeListener(coreListener)

        // dms coreContext.disableSubscriptions()

        // dms end *********************

        contactsList.value.orEmpty().forEach(ContactViewModel::destroy)
        coreContext.contactsManager.magicSearch.removeListener(magicSearchListener)
        coreContext.contactsManager.removeListener(contactsUpdatedListener)

        super.onCleared()
    }

    fun updateContactsList(clearCache: Boolean) {
        val filterValue = filter.value.orEmpty()

        if (clearCache || (
            previousFilter.isNotEmpty() && (
                previousFilter.length > filterValue.length ||
                    (previousFilter.length == filterValue.length && previousFilter != filterValue)
                )
            )
        ) {
            coreContext.contactsManager.magicSearch.resetSearchCache()
        }
        previousFilter = filterValue

        val domain = if (sipContactsSelected.value == true) coreContext.core.defaultAccount?.params?.domain ?: "" else ""
        val filter = MagicSearchSource.Friends.toInt() or MagicSearchSource.LdapServers.toInt()
        val aggregation = MagicSearchAggregation.Friend
        searchResultsPending = true
        fastFetchJob?.cancel()
        Log.i("[Contacts] Asking Magic search for contacts matching filter [$filterValue], domain [$domain] and in sources [$filter]")
        coreContext.contactsManager.magicSearch.getContactsListAsync(filterValue, domain, filter, aggregation)

        val spinnerDelay = corePreferences.delayBeforeShowingContactsSearchSpinner.toLong()
        fastFetchJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                delay(spinnerDelay)
            }
            withContext(Dispatchers.Main) {
                if (searchResultsPending) {
                    fetchInProgress.value = true
                }
            }
        }
    }

    private fun processMagicSearchResults(results: Array<SearchResult>) {
        Log.i("[Contacts] Processing ${results.size} results")
        contactsList.value.orEmpty().forEach(ContactViewModel::destroy)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val list = arrayListOf<ContactViewModel>()

                for (result in results) {
                    val friend = result.friend

                    Log.w("[Contacts] SearchResult " + friend?.name)

                    val viewModel = if (friend != null) {
                        ContactViewModel(friend, true)
                    } else {
                        Log.w("[Contacts] SearchResult [$result] has no Friend!")
                        val fakeFriend = coreContext.contactsManager.createFriendFromSearchResult(result)
                        ContactViewModel(fakeFriend, true)
                    }

                    list.add(viewModel)
                }

                contactsList.postValue(list)
            }

            withContext(Dispatchers.Main) {
                Log.i("[Contacts] Processed ${results.size} results")
            }
        }
    }

    fun deleteContact(friend: Friend) {
        friend.remove() // TODO: FIXME: friend is const here!

        val id = friend.refKey
        if (id == null) {
            Log.w("[Contacts] Friend has no refkey, can't delete it from native address book")
            return
        }

        val select = ContactsContract.Data.CONTACT_ID + " = ?"
        val ops = ArrayList<ContentProviderOperation>()

        Log.i("[Contacts] Adding Android contact id $id to batch removal")
        val args = arrayOf(id)
        ops.add(
            ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                .withSelection(select, args)
                .build()
        )

        if (ops.isNotEmpty()) {
            try {
                Log.i("[Contacts] Removing ${ops.size} contacts")
                coreContext.context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            } catch (e: Exception) {
                Log.e("[Contacts] $e")
            }
        }
    }

    fun deleteContacts(list: ArrayList<Friend>) {
        val select = ContactsContract.Data.CONTACT_ID + " = ?"
        val ops = ArrayList<ContentProviderOperation>()

        for (friend in list) {
            val id = friend.refKey
            if (id != null) {
                Log.i("[Contacts] Adding Android contact id $id to batch removal")
                val args = arrayOf(id)
                ops.add(
                    ContentProviderOperation.newDelete(ContactsContract.RawContacts.CONTENT_URI)
                        .withSelection(select, args)
                        .build()
                )
            }
            friend.remove()
        }

        if (ops.isNotEmpty()) {
            try {
                Log.i("[Contacts] Removing ${ops.size} contacts")
                coreContext.context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            } catch (e: Exception) {
                Log.e("[Contacts] $e")
            }
        }
    }
}
