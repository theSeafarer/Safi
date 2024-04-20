/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.theseafarer.safi.data.TransportAllowList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("rule")
private val DEFAULT_RULE_KEY = intPreferencesKey("default_rule")
private val DEFAULT_RULE_VALUE = TransportAllowList()

class RulePrefs(
    appContext: Context
) {
    private val dataStore: DataStore<Preferences> = appContext.dataStore

    suspend fun setDefaultRule(transportAllowList: TransportAllowList) {
        dataStore.edit {
            it[DEFAULT_RULE_KEY] = transportAllowList.value
        }
    }

    val defaultRuleState: Flow<TransportAllowList> =
        dataStore.data
            .map { prefs ->
                prefs[DEFAULT_RULE_KEY]?.let { TransportAllowList(it) } ?: DEFAULT_RULE_VALUE
            }
}