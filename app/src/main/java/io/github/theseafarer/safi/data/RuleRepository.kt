/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.data

import io.github.theseafarer.safi.data.local.BasicRule
import io.github.theseafarer.safi.data.local.BasicRuleDao
import io.github.theseafarer.safi.data.local.RulePrefs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface RuleRepository {
    fun getDefaultRule(): Flow<TransportAllowList>
    suspend fun updateDefaultRule(transportAllowList: TransportAllowList)
    fun getAllAppRules(): Flow<List<Rule>>
    fun getAppTransportRules(@TransportAllowFlag transportType: Int): Flow<List<TransportRule>>
    suspend fun insertAppRule(rule: Rule)
    suspend fun deleteAppRule(packageName: String)
    fun shouldBlockByDefault(transportType: Int): Flow<Boolean>
}

class RuleRepositoryImpl(
    private val localDao: BasicRuleDao,
    private val rulePrefs: RulePrefs,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RuleRepository {

    override fun getDefaultRule(): Flow<TransportAllowList> =
        rulePrefs
            .defaultRuleState
            .flowOn(defaultDispatcher)

    override suspend fun updateDefaultRule(transportAllowList: TransportAllowList) =
        withContext(defaultDispatcher) {
            rulePrefs.setDefaultRule(transportAllowList)
        }


    override fun getAllAppRules(): Flow<List<Rule>> =
        localDao
            .getAllRules()
            .map { rules -> rules.map { it.toCommon() } }
            .flowOn(defaultDispatcher)

    override fun getAppTransportRules(@TransportAllowFlag transportType: Int): Flow<List<TransportRule>> =
        localDao
            .getAllRules()
            .map { rules -> rules.map { it.toTransportRule(transportType) } }
            .flowOn(defaultDispatcher)

    override suspend fun insertAppRule(rule: Rule) = withContext(defaultDispatcher) {
        localDao.insertRules(listOf(BasicRule.fromCommon(rule)))
    }

    override suspend fun deleteAppRule(packageName: String) = withContext(defaultDispatcher) {
        localDao.removeRule(packageName)
    }

    override fun shouldBlockByDefault(transportType: Int): Flow<Boolean> =
        rulePrefs.defaultRuleState
            .map { it.shouldBlock(transportType) }
            .flowOn(defaultDispatcher)

}