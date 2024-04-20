/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.home

import arrow.optics.optics
import io.github.theseafarer.safi.data.TransportAllowList
import io.github.theseafarer.safi.data.TransportRuleState
import io.github.theseafarer.safi.ScreenContract

interface HomeContract :
    ScreenContract<HomeContract.UiState, HomeContract.Event, HomeContract.Effect> {

    enum class SwitchState { STOPPED, STARTING, STARTED, RESTARTING; }

    @optics
    data class RuleState(
        val wifiState: TransportRuleState = TransportRuleState.UNKNOWN,
        val mobileDataState: TransportRuleState = TransportRuleState.UNKNOWN
    ) {
        companion object {
            fun TransportAllowList.toRuleState(): RuleState =
                RuleState(
                    wifiState = if (shouldAllowWifi()) {
                        TransportRuleState.ALLOWED
                    } else {
                        TransportRuleState.BLOCKED
                    },
                    mobileDataState = if (shouldAllowCellular()) {
                        TransportRuleState.ALLOWED
                    } else {
                        TransportRuleState.BLOCKED
                    }
                )
        }
    }

    @optics
    data class AppRuleState(
        val packageName: String,
//        val displayName: String? = null,
//        val icon: Painter, //INVESTIGATE
        val rule: RuleState,
        val applied: Boolean = true,
    ) {
        companion object
    }

    @optics
    data class UiState(
        val switchState: SwitchState = SwitchState.STOPPED,
        val defaultRuleState: RuleState = RuleState(),
        val appRules: List<AppRuleState> = emptyList(),
    ) {
        companion object
    }

//    @optics
//    sealed interface RuleCreationSheetState {
//        data object Loading : RuleCreationSheetState
//        data class AppSelection(val apps: List<AppInfo>) : RuleCreationSheetState
//        data class RuleCreation(val selectedApp: AppInfo) : RuleCreationSheetState
//    }

    data class AppInfo(
        val uid: Int,
        val packageName: String,
        val displayName: String
    )

    sealed interface Event {
        data class AppListFetched(val apps: List<AppInfo>) : Event
        data class DefaultRuleEdited(val rule: RuleState) : Event
        data class AppRuleEdited(val appRule: AppRuleState) : Event
        data class AppRuleDeleted(val packageName: String) : Event
        data class SwitchClicked(val currentState: Boolean) : Event
        data class NewRuleCreated(val appRule: AppRuleState) : Event
    }

    sealed interface Effect {
        data class FetchAppList(val flags: Int) : Effect
        data class SendServiceCommand(val shouldEnable: Boolean) : Effect
    }

}