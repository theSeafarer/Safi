/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.theseafarer.safi.data.local.BasicRule
import kotlinx.coroutines.flow.Flow

@Dao
interface BasicRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<BasicRule>)

    @Query("DELETE FROM basic_rule WHERE package_name = :packageName;")
    suspend fun removeRule(packageName: String)

    @Query("SELECT * FROM basic_rule ORDER BY package_name;")
    fun getAllRules(): Flow<List<BasicRule>>

//    @Query("SELECT * FROM basic_rule WHERE transport_type = :transportType;")
//    fun getRules(transportType: Int): Flow<List<BasicRule>>

    @Query("SELECT * FROM basic_rule WHERE package_name = :packageName;")
    fun getRules(packageName: String): Flow<List<BasicRule>>
}