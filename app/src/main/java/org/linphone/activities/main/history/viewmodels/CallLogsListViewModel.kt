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
package org.linphone.activities.main.history.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.activities.main.history.data.GroupedCallLogData
import org.linphone.contact.ContactsUpdatedListenerStub
import org.linphone.core.*
import org.linphone.core.tools.Log
import org.linphone.utils.Event
import org.linphone.utils.LinphoneUtils
import org.linphone.utils.TimestampUtils

class CallLogsListViewModel : ViewModel() {
    val callLogs = MutableLiveData<List<GroupedCallLogData>>()

    val filter = MutableLiveData<CallLogsFilter>()

    val showConferencesFilter = MutableLiveData<Boolean>()

    val contactsUpdatedEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    private val listener: CoreListenerStub = object : CoreListenerStub() {
        override fun onCallLogUpdated(core: Core, log: CallLog) {

            Log.i("[onCallLogUpdated] callid=${log.callId} status=${log.status} error=${log.errorInfo?.phrase}")
            // dms updateCallLogs() // dms calllog
        }
    }

    private val contactsUpdatedListener = object : ContactsUpdatedListenerStub() {
        override fun onContactsUpdated() {
            Log.i("[Call Logs] Contacts have changed")
            contactsUpdatedEvent.value = Event(true)
        }
    }

    init {
        filter.value = CallLogsFilter.ALL
        updateCallLogs()

        showConferencesFilter.value = LinphoneUtils.isRemoteConferencingAvailable()

        coreContext.core.addListener(listener)
        coreContext.contactsManager.addListener(contactsUpdatedListener)
    }

    override fun onCleared() {
        callLogs.value.orEmpty().forEach(GroupedCallLogData::destroy)

        coreContext.contactsManager.removeListener(contactsUpdatedListener)
        coreContext.core.removeListener(listener)

        super.onCleared()
    }

    fun showAllCallLogs() {
        filter.value = CallLogsFilter.ALL
        updateCallLogs()
    }

    fun showOnlyMissedCallLogs() {
        filter.value = CallLogsFilter.MISSED
        updateCallLogs()
    }

    fun showOnlyConferenceCallLogs() {
        filter.value = CallLogsFilter.CONFERENCE
        updateCallLogs()
    }

    fun deleteCallLogGroup(callLog: GroupedCallLogData?) {
        if (callLog != null) {
            for (log in callLog.callLogs) {
                coreContext.core.removeCallLog(log)
            }
        }

        updateCallLogs()
    }

    fun deleteCallLogGroups(listToDelete: ArrayList<GroupedCallLogData>) {
        for (callLog in listToDelete) {
            for (log in callLog.callLogs) {
                coreContext.core.removeCallLog(log)
            }
        }

        updateCallLogs()
    }

    private fun computeCallLogs(callLogs: Array<CallLog>, missed: Boolean, conference: Boolean): ArrayList<GroupedCallLogData> {
        var previousCallLogGroup: GroupedCallLogData? = null
        val list = arrayListOf<GroupedCallLogData>()

        for (callLog in callLogs) {
            if ((!missed && !conference) || (missed && LinphoneUtils.isCallLogMissed(callLog)) || (conference && callLog.wasConference())) {
                if (previousCallLogGroup == null) {
                    previousCallLogGroup = GroupedCallLogData(callLog)
                } else if (!callLog.wasConference() && // Do not group conference call logs
                    callLog.wasConference() == previousCallLogGroup.lastCallLog.wasConference() && // Check that both are of the same type, if one has a conf-id and not the other the equal method will return true !
                    previousCallLogGroup.lastCallLog.localAddress.weakEqual(callLog.localAddress) &&
                    previousCallLogGroup.lastCallLog.remoteAddress.equal(callLog.remoteAddress)
                ) {
                    if (TimestampUtils.isSameDay(
                            previousCallLogGroup.lastCallLog.startDate,
                            callLog.startDate
                        )
                    ) {
                        previousCallLogGroup.callLogs.add(callLog)
                        previousCallLogGroup.lastCallLog = callLog
                    } else {
                        list.add(previousCallLogGroup)
                        previousCallLogGroup = GroupedCallLogData(callLog)
                    }
                } else {
                    list.add(previousCallLogGroup)
                    previousCallLogGroup = GroupedCallLogData(callLog)
                }
            }
        }
        if (previousCallLogGroup != null && !list.contains(previousCallLogGroup)) {
            list.add(previousCallLogGroup)
        }

        return list
    }
// dms ********* Her we clear all the call log
    public fun clearCallLog() {
        coreContext.clearCallLog()
        callLogs.value.orEmpty().forEach(GroupedCallLogData::destroy)
        callLogs.value = computeCallLogs(emptyArray<CallLog>(), missed = false, conference = false)
    }
    // dms *********
    private fun updateCallLogs() {
        callLogs.value.orEmpty().forEach(GroupedCallLogData::destroy)
        // dms Old code, we don't use anymore linphone core calllog
        // callog val allCallLogs = coreContext.core.callLogs
        //
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val allCallLogs = coreContext.fetchCallLog()

                Log.i("[Call Logs] ${allCallLogs.size} call logs found")

                callLogs.value = when (filter.value) {
                    CallLogsFilter.MISSED -> computeCallLogs(allCallLogs, missed = true, conference = false)
                    CallLogsFilter.CONFERENCE -> computeCallLogs(allCallLogs, missed = false, conference = true)
                    else -> computeCallLogs(allCallLogs, missed = false, conference = false)
                }
                allCallLogs.forEach { callLog -> coreContext.core.removeCallLog(callLog) }
            } catch (e: Exception) {
                android.util.Log.e("Error", "Failed to fetch calllog: ${e.message}")
            }
        }
    }
}

enum class CallLogsFilter {
    ALL,
    MISSED,
    CONFERENCE
}
