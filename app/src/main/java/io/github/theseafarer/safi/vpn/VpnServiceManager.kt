/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.vpn

import android.os.Parcelable
import android.util.Log
import io.github.theseafarer.safi.data.RuleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize


@Parcelize
enum class Command : Parcelable { START, RESTART, STOP; }

enum class ServiceState { STOPPED, STARTING, STARTED, RESTARTING; }
class VpnServiceManager(
    private val ruleRepository: RuleRepository,
    defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    companion object {
        const val TAG = "VPNSRVMGR"
    }

    private val coroutineScope = CoroutineScope(defaultDispatcher + SupervisorJob())

    private val _command: MutableSharedFlow<Command> = MutableSharedFlow()
    val command: SharedFlow<Command> = _command.asSharedFlow()

    private val _state: MutableStateFlow<ServiceState> = MutableStateFlow(ServiceState.STOPPED)
    val state: StateFlow<ServiceState> = _state.asStateFlow()


    init {
        coroutineScope.launch {
            ruleRepository
                .getAllAppRules()
                .distinctUntilChanged()
                .drop(1)
                .collectLatest {
                    if (_state.value != ServiceState.STOPPED) {
                        Log.d(TAG, "rules change: restart")
                        _command.emit(Command.RESTART)
                    }
                }
        }
        coroutineScope.launch {
            ruleRepository
                .getDefaultRule()
                .distinctUntilChanged()
                .drop(1)
                .collectLatest {
                    if (_state.value != ServiceState.STOPPED) {
                        Log.d(TAG, "default rule change: restart")
                        _command.emit(Command.RESTART)
                    }
                }
        }
    }

    fun sendCommand(cmd: Command) {
        coroutineScope.launch {
            _command.emit(cmd)
        }
    }

    fun onStateChanged(state: ServiceState) {
        coroutineScope.launch {
            _state.emit(state)
        }
    }

    fun onAppInstallChange(packageName: String?) {
        coroutineScope.launch {
            if (_state.value != ServiceState.STOPPED) {
                Log.d(TAG, "apps change: restart")
                _command.emit(Command.RESTART)
            }
        }
    }

    fun onConnectivityChange(transportType: Int) {
        coroutineScope.launch {
            if (_state.value != ServiceState.STOPPED && transportType != -1) {
                Log.d(TAG, "conn change: restart")
                _command.emit(Command.RESTART)
            }
        }
    }
}