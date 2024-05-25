package org.linphone.activities.main.directory.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import org.linphone.bcsws.DirectoryItem

class DirectoryViewModel : ViewModel() {

    private val _directory = MutableLiveData<List<DirectoryItem>>()
    val directory: LiveData<List<DirectoryItem>> = _directory

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val selectedItem = MutableLiveData<DirectoryItem>()

    fun selectItem(item: DirectoryItem) {
        selectedItem.value = item
    }

    fun getSelectedItem(): LiveData<DirectoryItem> = selectedItem

    fun updateDirectory(Items: List<DirectoryItem>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _directory.postValue(Items)
            } catch (e: Exception) {
                e.printStackTrace()
                _directory.postValue(emptyList())
            }
            _isLoading.value = false
        }
    }
}
