package org.linphone.activities.main.directory.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.bcsws.DirectoryItem

class DirectoryViewModel : ViewModel() {

    private val _directory = MutableLiveData<List<DirectoryItem>>()
    val directory: LiveData<List<DirectoryItem>> = _directory

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun searchDirectory(filter: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = coreContext.bcsWsHandler?.fetchDirectory(filter) // Passa il parametro di ricerca al repository
                _directory.postValue(response?.Items)
            } catch (e: Exception) {
                e.printStackTrace()
                _directory.postValue(emptyList())
            }
            _isLoading.value = false
        }
    }
}
