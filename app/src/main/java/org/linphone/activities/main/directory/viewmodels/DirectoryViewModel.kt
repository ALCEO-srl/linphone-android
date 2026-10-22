package org.linphone.activities.main.directory.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.bcsws.DirectoryItem
import org.linphone.core.tools.Log

// dms: the BCS directory is searched by the server, one page at a time. The server can't interrupt a search, so
// only one request is in flight: what is typed meanwhile is searched when the answer arrives, only after a pause in
// typing and from MIN_FILTER_LENGTH characters (shorter text shows the whole directory).
// Once all the entries matching a filter are loaded, a search containing that filter only narrows them and is done
// locally: with the whole directory loaded (scrolled to the end) the server is not asked anymore.
class DirectoryViewModel : ViewModel() {
    companion object {
        private const val PAGE_SIZE = 50
        private const val MIN_FILTER_LENGTH = 2
        private const val TYPING_PAUSE_MS = 400L
    }

    private val _directory = MutableLiveData<List<DirectoryItem>>()
    val directory: LiveData<List<DirectoryItem>> = _directory

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val selectedItem = MutableLiveData<DirectoryItem>()

    // Text of the search field, kept to restore it when the view is created again
    var searchText = ""
        private set

    private val filter = MutableStateFlow("")
    private val requestMutex = Mutex()
    private var loadedFilter = ""
    private var totalCount = 0
    private var loadingNextPage = false

    // Widest filter whose matching entries are all loaded, null when none
    private var completeFilter: String? = null
    private var completeItems = listOf<DirectoryItem>()

    init {
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            filter
                .debounce { if (it.isEmpty()) 0L else TYPING_PAUSE_MS }
                .distinctUntilChanged()
                .collect { loadFirstPage(it) }
        }
    }

    fun selectItem(item: DirectoryItem) {
        selectedItem.value = item
    }

    fun getSelectedItem(): LiveData<DirectoryItem> = selectedItem

    fun search(text: String) {
        searchText = text
        val trimmed = text.trim()
        filter.value = if (trimmed.length < MIN_FILTER_LENGTH) "" else trimmed
    }

    // Account registered again or disabled: the current search is repeated on the server
    fun reload() {
        completeFilter = null
        completeItems = emptyList()
        viewModelScope.launch { loadFirstPage(filter.value) }
    }

    fun loadNextPage() {
        if (loadingNextPage || _directory.value.orEmpty().size >= totalCount) return

        loadingNextPage = true
        viewModelScope.launch {
            requestMutex.withLock {
                val items = _directory.value.orEmpty()
                if (items.size < totalCount) {
                    val page = coreContext.fetchDirectoryPage(loadedFilter, items.size, PAGE_SIZE)
                    if (page != null) {
                        // An empty page means the directory changed meanwhile: stop asking for more
                        totalCount = if (page.Items.isEmpty()) items.size else page.Count
                        _directory.value = items + page.Items
                        rememberIfComplete()
                    }
                }
            }
            loadingNextPage = false
        }
    }

    private suspend fun loadFirstPage(filter: String) {
        requestMutex.withLock {
            val complete = completeFilter
            if (complete != null && filter.contains(complete, ignoreCase = true)) {
                val items = completeItems.filter { matches(it, filter) }
                loadedFilter = filter
                totalCount = items.size
                _directory.value = items
                Log.i("[Directory] Search [$filter]: ${items.size} entries, filtered locally")
                return@withLock
            }

            _isLoading.value = true
            val page = coreContext.fetchDirectoryPage(filter, 0, PAGE_SIZE)
            _isLoading.value = false

            loadedFilter = filter
            totalCount = page?.Count ?: 0
            _directory.value = page?.Items.orEmpty()
            Log.i("[Directory] Search [$filter]: ${page?.Items?.size ?: 0} of $totalCount entries")
            if (page != null) rememberIfComplete()
        }
    }

    // The entries shown are all those matching loadedFilter: kept if no wider complete result is known
    private fun rememberIfComplete() {
        val items = _directory.value.orEmpty()
        val complete = completeFilter
        if (items.size >= totalCount && (complete == null || !loadedFilter.contains(complete, ignoreCase = true))) {
            completeFilter = loadedFilter
            completeItems = items
        }
    }

    // Same fields searched by the server
    private fun matches(item: DirectoryItem, filter: String): Boolean {
        return listOf(
            item.Id, item.Uri, item.Name, item.Surname, item.DisplayName, item.Profession, item.Company,
            item.EmailAddress, item.MobilePhone, item.LandlinePhone, item.FaxPhone, item.Title, item.Address,
            item.Branch, item.Office, item.Manager, item.Assistant, item.Attr1, item.Attr2, item.Attr3, item.Attr4,
            item.Attr5, item.Attr6, item.Attr7, item.Attr8, item.Attr9, item.Attr10
        ).any { it.contains(filter, ignoreCase = true) }
    }
}
