/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.data


data class Rule(
    val packageName: String,
//    val transportType: Int, //FIXME
//    val isBlocked: Boolean
    val transportAllowList: TransportAllowList
)

enum class TransportRuleState {
    BLOCKED, ALLOWED, CUSTOM, UNKNOWN;

    fun toggle(): TransportRuleState =
        when (this) {
            BLOCKED -> ALLOWED
            ALLOWED -> BLOCKED
            CUSTOM -> BLOCKED
            UNKNOWN -> BLOCKED
        }

    fun isAllowed(): Boolean =
        this == ALLOWED
}

data class TransportRule(
    val packageName: String,
    val state: TransportRuleState
)