/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.vpn

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.getSystemService
import io.github.theseafarer.safi.App
import io.github.theseafarer.safi.data.RuleRepository
import io.github.theseafarer.safi.MainActivity
import io.github.theseafarer.safi.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class VpnService : VpnService() {


    companion object {
        const val EXTRA_COMMAND = "command"
        const val TAG = "VPNVPN"
    }

    private var vpnPfd: MutableStateFlow<ParcelFileDescriptor?> = MutableStateFlow(null)
    private val ruleRepository: RuleRepository by lazy { App.ruleRepository }
    private lateinit var connectivityChangeWatcher: ConnectivityChangeWatcher
    private lateinit var appInstallWatcher: AppInstallWatcher

    private val serviceManager: VpnServiceManager by lazy { App.vpnServiceManager }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = runBlocking {
        Log.d(TAG, "onStart, ${intent?.extras}")
        intent?.extras?.getParcelable<Command>(EXTRA_COMMAND)?.let { cmd ->
            serviceManager.sendCommand(cmd)
        } ?: {
            Log.e(TAG, "onStartCommand: no Command extra found!")
//            stopSelf()
        }

        return@runBlocking START_STICKY
    }

    private fun vpnReload() {
        vpnStop()
        vpnStart()
    }

    //FIXME
    private fun vpnStop() {
        Log.i(TAG, "vpnStop: stopping")
        vpnPfd.value
            ?.runCatching {
                close()
                vpnPfd.update { null }
            }
            ?.onFailure { Log.e(TAG, "vpnStop: failed to close vpn") }
    }

    private fun vpnStart() {
        serviceScope.launch {
            runCatching {
                Log.i(TAG, "vpnStart: starting")
                val vpn = Builder().apply {
                    setSession(getString(R.string.app_name))
                    addAddress("10.1.10.1", 32)
                    addRoute("0.0.0.0", 0)
                    setBlocking(false)

                    val transportType = connectivityChangeWatcher.state.value
                    Log.d(TAG, "vpnStart: transport: $transportType")
                    if (transportType == -1 || transportType == null) throw Exception("No transport type!") //FIXME
                    val shouldBlockByDefault =
                        ruleRepository.shouldBlockByDefault(transportType).first()
                    Log.d(TAG, "vpnStart: shouldBlock: $shouldBlockByDefault")

                    val rules = ruleRepository.getAppTransportRules(transportType).first()
                    var noExceptions = true
                    rules.forEach { rule ->
                        Log.d(
                            TAG,
                            "vpnStart: rule: ${rule.packageName} : isAllowed(${rule.state.isAllowed()})"
                        )
                        if (shouldBlockByDefault) {
                            if (rule.state.isAllowed()) {
                                try {
                                    addDisallowedApplication(rule.packageName)
                                } catch (e: Exception) {
                                    Log.e(
                                        TAG,
                                        "Failed to apply ALLOWED rule for package: ${rule.packageName}"
                                    )
                                }
                            }
                        } else {
                            if (!rule.state.isAllowed()) {
                                Log.d(TAG, "vpnStart, here : ${rule.packageName}")
                                try {
                                    addAllowedApplication(rule.packageName)
                                    noExceptions = false
                                } catch (e: Exception) {
                                    Log.e(
                                        TAG,
                                        "Failed to apply BLOCKED rule for package: ${rule.packageName}"
                                    )
                                }
                            }
                        }
                    }
                    if (!shouldBlockByDefault && noExceptions) {
                        runCatching { addAllowedApplication(packageName) }
                    }

                    val configPendingIntent = PendingIntent.getActivity(
                        this@VpnService,
                        0,
                        Intent(this@VpnService, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    setConfigureIntent(configPendingIntent)
                }.establish()
                vpnPfd.update { vpn }

            }
                .onFailure { serviceManager.onStateChanged(ServiceState.STOPPED) }
                .onSuccess { serviceManager.onStateChanged(ServiceState.STARTED) }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate")
        val connMgr = getSystemService<ConnectivityManager>()!!
        connectivityChangeWatcher =
            ConnectivityChangeWatcher(connMgr, serviceManager::onConnectivityChange)
        connMgr.registerDefaultNetworkCallback(connectivityChangeWatcher)
        appInstallWatcher = AppInstallWatcher(serviceManager::onAppInstallChange)
        val packageAddedFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addDataScheme("package")
        }
        registerReceiver(appInstallWatcher, packageAddedFilter)
        serviceScope.launch {
            serviceManager.command.collect { cmd ->
                Log.d(TAG, "cmd: $cmd")
                when (cmd) {
                    Command.START -> {
                        serviceManager.onStateChanged(ServiceState.STARTING)
                        vpnStart()

                    }

                    Command.RESTART -> {
                        serviceManager.onStateChanged(ServiceState.RESTARTING)
                        vpnReload()
                    }

                    Command.STOP -> {
                        vpnStop()
                        serviceManager.onStateChanged(ServiceState.STOPPED) //!?
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onDestroy() = runBlocking {
        unregisterReceiver(appInstallWatcher)
        serviceScope.cancel()
        vpnStop()
        super.onDestroy()
    }

    override fun onRevoke() = runBlocking {
        serviceScope.cancel()
        vpnStop()
        super.onRevoke()
    }
}

internal class AppInstallWatcher(
    private val onPackageAdded: (packageName: String) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?): Unit = runBlocking {
        intent?.let {
            intent.data?.host?.let { packageName -> onPackageAdded(packageName) }
        }
    }

}

internal class ConnectivityChangeWatcher(
    private val connMgr: ConnectivityManager,
    private val onConnectivityChanged: (Int) -> Unit
) : NetworkCallback() {

    companion object {
        const val TAG = "VpnService.ConnChange"
    }

    private val _state = MutableStateFlow<Int?>(null)
    internal val state: StateFlow<Int?> = _state.asStateFlow()

    init {
        _state.tryEmit(getTransportType())
    }

    override fun onAvailable(network: Network): Unit = runBlocking {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return@runBlocking
        val transportType =
            getTransportType(connMgr.getNetworkCapabilities(network)) ?: return@runBlocking
        Log.i(TAG, "transportType = $transportType")
        if (_state.value != null && _state.value != transportType) {
            onConnectivityChanged(transportType)
        }
        _state.emit(transportType)
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) = runBlocking {
        super.onCapabilitiesChanged(network, networkCapabilities)
        val transportType = getTransportType(networkCapabilities) ?: return@runBlocking
        Log.i(TAG, "transportType = $transportType")
        if (_state.value != null && _state.value != transportType) {
            onConnectivityChanged(transportType)
        }
        _state.emit(transportType)
    }

    private fun getTransportType(
        netCap: NetworkCapabilities? =
            connMgr.getNetworkCapabilities(connMgr.activeNetwork)
    ): Int? =
        netCap?.let {
            when {
                netCap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                    NetworkCapabilities.TRANSPORT_WIFI

                netCap.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                    NetworkCapabilities.TRANSPORT_CELLULAR

                netCap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                    NetworkCapabilities.TRANSPORT_ETHERNET

                netCap.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) ->
                    NetworkCapabilities.TRANSPORT_BLUETOOTH

                else -> -1
            }
        }
}