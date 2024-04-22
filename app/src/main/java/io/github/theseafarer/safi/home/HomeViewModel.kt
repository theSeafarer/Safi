/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.home

import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.theseafarer.safi.data.Rule
import io.github.theseafarer.safi.data.RuleRepository
import io.github.theseafarer.safi.data.TransportAllowList
import io.github.theseafarer.safi.data.TransportRuleState
import io.github.theseafarer.safi.home.HomeContract.RuleState.Companion.toRuleState
import io.github.theseafarer.safi.vpn.ServiceState
import io.github.theseafarer.safi.vpn.VpnServiceManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val ruleRepository: RuleRepository,
    private val vpnServiceManager: VpnServiceManager
) : HomeContract, ViewModel() {

    companion object {
        const val TAG = "HomeViewModel"
    }

    private val _state = MutableStateFlow(HomeContract.UiState())
    override val state: StateFlow<HomeContract.UiState> = _state.asStateFlow()
    private val _effect = MutableSharedFlow<HomeContract.Effect>(replay = 1) //INVESTIGATE
    override val effect: SharedFlow<HomeContract.Effect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            ruleRepository.getAllAppRules().collect { fetchedRules ->
                Log.i(TAG, "updated, ${fetchedRules.size}")
                _state.emit(HomeContract.UiState.appRules.modify(_state.value) { current ->
                    val unApplied = current
                        .filter { !it.applied }
                        .map { it.packageName }
                    fetchedRules.map { rule ->
                        HomeContract.AppRuleState(
                            packageName = rule.packageName,
                            applied = !unApplied.contains(rule.packageName),
                            rule = HomeContract.RuleState(
                                wifiState =
                                if (rule.transportAllowList.shouldAllow(NetworkCapabilities.TRANSPORT_WIFI)) {
                                    TransportRuleState.ALLOWED
                                } else {
                                    TransportRuleState.BLOCKED
                                },
                                mobileDataState =
                                if (rule.transportAllowList.shouldAllow(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                    TransportRuleState.ALLOWED
                                } else {
                                    TransportRuleState.BLOCKED
                                },
                            )
                        )
                    }
                })
//                launch {
//                    delay(2.seconds)
//                    _state.emit(HomeContract.UiState.appRules.modify(_state.value) { appRules ->
//                        appRules.map {
//                            it.copy(
//                                applied = true
//                            )
//                        }
//                    })
//                }
            }
        }

        viewModelScope.launch {
            ruleRepository.getDefaultRule().collect {
                _state.emit(
                    HomeContract.UiState.defaultRuleState.set(_state.value, it.toRuleState())
                )
            }
        }

        viewModelScope.launch {
            vpnServiceManager.state.collectLatest {
                val switchState = when (it) {
                    ServiceState.STOPPED -> HomeContract.SwitchState.STOPPED
                    ServiceState.STARTING -> HomeContract.SwitchState.STARTING
                    ServiceState.STARTED -> HomeContract.SwitchState.STARTED
                    ServiceState.RESTARTING -> HomeContract.SwitchState.RESTARTING
                }
                _state.emit(HomeContract.UiState.switchState.set(_state.value, switchState))
                if (switchState == HomeContract.SwitchState.STARTED || switchState == HomeContract.SwitchState.STOPPED) {
                    _state.emit(HomeContract.UiState.appRules.modify(_state.value) { appRules ->
                        appRules.map { it.copy(applied = true) }
                    })
                }
            }
        }
    }

    override fun onEvent(event: HomeContract.Event) {
        viewModelScope.launch {
            when (event) {
                is HomeContract.Event.AppRuleEdited -> {
                    val prevRuleState = _state.value.appRules.find { it.packageName == event.appRule.packageName }
                    if (prevRuleState != null && prevRuleState.rule == event.appRule.rule) return@launch
                    _state.update { currState ->
                        HomeContract.UiState.appRules.modify(currState) { rules ->
                            rules.map {
                                if (it.packageName == event.appRule.packageName
                                    && _state.value.switchState != HomeContract.SwitchState.STOPPED
                                )
                                    event.appRule.copy(
                                        applied = false
                                    ) else it
                            }
                        }
                    }
                    ruleRepository.insertAppRule(
                        Rule(
                            packageName = event.appRule.packageName,
                            transportAllowList = TransportAllowList(
                                allowWifi = event.appRule.rule.wifiState.isAllowed(),
                                allowCellular = event.appRule.rule.mobileDataState.isAllowed()
                            )
                        )
                    )
                }

                is HomeContract.Event.DefaultRuleEdited -> {
                    ruleRepository.updateDefaultRule(
                        TransportAllowList(
                            allowCellular = event.rule.mobileDataState.isAllowed(),
                            allowWifi = event.rule.wifiState.isAllowed()
                        )
                    )
                }

                is HomeContract.Event.SwitchClicked -> {
                    _effect.emit(HomeContract.Effect.SendServiceCommand(event.currentState))
                }

                is HomeContract.Event.AppListFetched -> {
                    //
                }

                is HomeContract.Event.NewRuleCreated -> with(event.appRule) {
                    ruleRepository.insertAppRule(
                        Rule(
                            packageName = packageName,
                            transportAllowList = TransportAllowList(
                                allowCellular = rule.mobileDataState.isAllowed(),
                                allowWifi = rule.wifiState.isAllowed()
                            )
                        )
                    )
                }

                is HomeContract.Event.AppRuleDeleted -> {
                    ruleRepository.deleteAppRule(event.packageName)
                }
            }
        }
    }
}