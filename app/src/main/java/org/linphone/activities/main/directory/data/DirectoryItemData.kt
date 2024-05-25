package org.linphone.activities.main.directory.data

class DirectoryItemData(
    val isSipAddress: Boolean,
    val caption: String,
    val data: String,
    private val destinationClickListener: DirectoryItemDataDestinationClickListener? = null,
    val isCallable: Boolean = false,
) {

    fun startCall() {
        destinationClickListener?.onCall(data)
    }

    fun startChat() {
        destinationClickListener?.onChat(data)
    }
}
interface DirectoryItemDataDestinationClickListener {
    fun onCall(destination: String)

    fun onChat(destination: String)
}
