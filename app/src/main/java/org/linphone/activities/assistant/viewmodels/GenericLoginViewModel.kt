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
package org.linphone.activities.assistant.viewmodels

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.linphone.LinphoneApplication.Companion.coreContext
import org.linphone.core.*
import org.linphone.core.tools.Log
import org.linphone.utils.Event

class GenericLoginViewModelFactory(private val accountCreator: AccountCreator) :
    ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GenericLoginViewModel(accountCreator) as T
    }
}

class GenericLoginViewModel(private val accountCreator: AccountCreator) : ViewModel() {
    val username = MutableLiveData<String>()

    val password = MutableLiveData<String>()

    val domain = MutableLiveData<String>()

    val displayName = MutableLiveData<String>()

    val transport = MutableLiveData<TransportType>()

    val loginEnabled: MediatorLiveData<Boolean> = MediatorLiveData()

    val waitForServerAnswer = MutableLiveData<Boolean>()

    val leaveAssistantEvent = MutableLiveData<Event<Boolean>>()

    val invalidCredentialsEvent: MutableLiveData<Event<Boolean>> by lazy {
        MutableLiveData<Event<Boolean>>()
    }

    val onErrorEvent: MutableLiveData<Event<String>> by lazy {
        MutableLiveData<Event<String>>()
    }

    private var proxyConfigToCheck: ProxyConfig? = null

    private val coreListener = object : CoreListenerStub() {
        @Deprecated("Deprecated in Java")
        override fun onRegistrationStateChanged(
            core: Core,
            cfg: ProxyConfig,
            state: RegistrationState,
            message: String
        ) {
            if (cfg == proxyConfigToCheck) {
                Log.i("[Assistant] [Generic Login] Registration state is $state: $message")
                if (state == RegistrationState.Ok) {
                    waitForServerAnswer.value = false
                    leaveAssistantEvent.value = Event(true)
                    core.removeListener(this)
                } else if (state == RegistrationState.Failed) {
                    waitForServerAnswer.value = false
                    invalidCredentialsEvent.value = Event(true)
                    core.removeListener(this)
                }
            }
        }
    }

    init {
        transport.value = TransportType.Tls

        loginEnabled.value = false
        loginEnabled.addSource(username) {
            loginEnabled.value = isLoginButtonEnabled()
        }
        loginEnabled.addSource(password) {
            loginEnabled.value = isLoginButtonEnabled()
        }
        loginEnabled.addSource(domain) {
            loginEnabled.value = isLoginButtonEnabled()
        }
    }

    fun setTransport(transportType: TransportType) {
        transport.value = transportType
    }

    fun removeInvalidProxyConfig() {
        val cfg = proxyConfigToCheck
        cfg ?: return
        val authInfo = cfg.findAuthInfo()
        if (authInfo != null) coreContext.core.removeAuthInfo(authInfo)
        coreContext.core.removeProxyConfig(cfg)
        proxyConfigToCheck = null
    }

    fun continueEvenIfInvalidCredentials() {
        leaveAssistantEvent.value = Event(true)
    }

    fun createProxyConfig() {
        waitForServerAnswer.value = true
        coreContext.core.addListener(coreListener)

        accountCreator.username = username.value
        accountCreator.password = password.value
        accountCreator.domain = domain.value
        accountCreator.displayName = displayName.value
        accountCreator.transport = transport.value

        val proxyConfig: ProxyConfig? = accountCreator.createProxyConfig()
        proxyConfigToCheck = proxyConfig

        if (proxyConfig == null) {
            Log.e("[Assistant] [Generic Login] Account creator couldn't create proxy config")
            coreContext.core.removeListener(coreListener)
            onErrorEvent.value = Event("Error: Failed to create account object")
            waitForServerAnswer.value = false
            return
        }

        // dms begin ************
        // We set the default value direct from the linphonerc conf file
        val turnusername: String = coreContext.core.config.getString("app", "turnusername", "")!!
        val turnuserpassword = coreContext.core.config.getString("app", "turnuserpassword", "")
        val turnrealm = coreContext.core.config.getString("app", "turnrealm", "")
        val turnserver = coreContext.core.config.getString("app", "turnserver", "")!!
        val outboundproxy = coreContext.core.config.getString("app", "outboundproxy", "")!!

        val authInfo = coreContext.core.findAuthInfo(turnrealm, turnusername, null)
        if (authInfo != null) {
            Log.w("######[Account Creator] Setting TURN AuthInfo")

            val newAuthInfo = authInfo.clone()
            newAuthInfo.password = turnuserpassword
            newAuthInfo.userid = turnusername
            newAuthInfo.realm = turnrealm
            coreContext.core.removeAuthInfo(authInfo)
            coreContext.core.addAuthInfo(newAuthInfo)
        } else {
            Log.w("#######[Account Creator] Creating TURN AuthInfo")
            val authInfo = Factory.instance()
                .createAuthInfo(turnusername, turnusername, turnuserpassword, null, turnrealm, null)
            coreContext.core.addAuthInfo(authInfo)
        }

        for (account in coreContext.core.accountList) {
            if ((account.params.identityAddress?.username == username.value) &&
                (account.params.identityAddress?.domain == domain.value)
            ) {

                if (account.params.natPolicy == null) {
                    Log.w("[Account Settings] No NAT Policy object in account params yet")
                    val natPolicy = coreContext.core.createNatPolicy()
                    natPolicy.stunServer = turnserver
                    natPolicy.isStunEnabled = turnserver.isNotEmpty()
                    natPolicy.isTurnEnabled = turnserver.isNotEmpty()
                    natPolicy.isIceEnabled = turnserver.isNotEmpty()
                    natPolicy.stunServerUsername = turnusername
                    account.params.natPolicy = natPolicy
                } else {
                    account.params.natPolicy?.stunServer = turnserver
                    account.params.natPolicy?.isStunEnabled = turnserver.isNotEmpty()
                    account.params.natPolicy?.stunServerUsername = turnusername
                    account.params.natPolicy?.isTurnEnabled = turnserver.isNotEmpty()
                    account.params.natPolicy?.isIceEnabled = turnserver.isNotEmpty()
                }
                account.params.pushNotificationAllowed = true
                account.params.isOutboundProxyEnabled = true
                account.params.limeServerUrl = ""

                val address = Factory.instance().createAddress(outboundproxy)
                if (address != null) {
                    account.params.serverAddress = address
                }
                account.params.isPublishEnabled = true
                account.params.transport = TransportType.Tls
                for (payloadType in coreContext.core.audioPayloadTypes) {
                    if (payloadType.isVbr) {
                        payloadType.normalBitrate = 64
                    }
                }

                break
            }
        }
        // dms end ************

        Log.i("[Assistant] [Generic Login] Proxy config created")
    }

    private fun isLoginButtonEnabled(): Boolean {
        return username.value.orEmpty().isNotEmpty() && domain.value.orEmpty().isNotEmpty() && password.value.orEmpty().isNotEmpty()
    }
}
