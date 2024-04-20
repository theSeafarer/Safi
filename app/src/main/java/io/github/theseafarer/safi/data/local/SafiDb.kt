/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.theseafarer.safi.data.local.BasicRule
import io.github.theseafarer.safi.data.local.BasicRuleDao

@Database(
    entities = [BasicRule::class],
    version = 1,
    exportSchema = false,
)
abstract class SafiDb : RoomDatabase() {
    abstract fun basicRuleDao(): BasicRuleDao
}