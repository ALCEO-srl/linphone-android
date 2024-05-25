package org.linphone.activities.main.directory.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.linphone.R
import org.linphone.activities.main.directory.data.DirectoryItemData
import org.linphone.bcsws.DirectoryItem
import org.linphone.utils.AppUtils.Companion.getString

class DirectoryItemDetailViewModel : ViewModel() {
    val mainCaption: MutableLiveData<String> = MutableLiveData<String>()
    val company: MutableLiveData<String> = MutableLiveData<String>()
    val items = MutableLiveData<ArrayList<DirectoryItemData>>()
    fun setItem(directoryItem: DirectoryItem) {
        mainCaption.value = when {
            directoryItem.DisplayName.isNotEmpty() -> directoryItem.DisplayName
            directoryItem.Name.isNotEmpty() || directoryItem.Surname.isNotEmpty() -> "${directoryItem.Name} ${directoryItem.Surname}"
            directoryItem.Name.isNotEmpty() || directoryItem.Surname.isNotEmpty() -> "${directoryItem.Name} ${directoryItem.Surname}"
            else -> directoryItem.Uri
        }

        company.value = directoryItem.Company

        val list = arrayListOf<DirectoryItemData>()
        if (directoryItem.Uri.isNotEmpty()) {
            list.add(DirectoryItemData(true, getString(R.string.directory_item_detail_uri), directoryItem.Uri, true))
        }
        if (directoryItem.MobilePhone.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_mobilephone), directoryItem.MobilePhone, true))
        }
        if (directoryItem.LandlinePhone.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_landlinephone), directoryItem.LandlinePhone, true))
        }
        if (directoryItem.FaxPhone.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_faxphone), directoryItem.FaxPhone))
        }
        if (directoryItem.EmailAddress.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_emailaddress), directoryItem.EmailAddress))
        }
        if (directoryItem.Address.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_address), directoryItem.Address))
        }
        if (directoryItem.Title.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_title), directoryItem.Title))
        }
        if (directoryItem.Profession.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_profession), directoryItem.Profession))
        }
        if (directoryItem.Company.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_company), directoryItem.Company))
        }
        if (directoryItem.Branch.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_branch), directoryItem.Branch))
        }
        if (directoryItem.Office.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_office), directoryItem.Office))
        }
        if (directoryItem.Manager.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_manager), directoryItem.Manager))
        }
        if (directoryItem.Assistant.isNotEmpty()) {
            list.add(DirectoryItemData(false, getString(R.string.directory_item_detail_assistant), directoryItem.Assistant))
        }
        items.value = list
    }
}
