/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import io.github.theseafarer.safi.data.Rule
import io.github.theseafarer.safi.data.TransportRule
import io.github.theseafarer.safi.data.TransportRuleState
import io.github.theseafarer.safi.data.TransportAllowFlag
import io.github.theseafarer.safi.data.TransportAllowList


@Entity(
    tableName = "basic_rule",
    primaryKeys = ["package_name"]
)
data class BasicRule(
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "allow_list")
    val transportAllowList: TransportAllowList
//    @ColumnInfo(name = "transport_type")
//    val transportType: Int, //FIXME
//    @ColumnInfo(name = "is_blocked")
//    val isBlocked: Boolean
) {
    //FIXME
    fun toCommon() =
        Rule(packageName, transportAllowList)

    fun toTransportRule(@TransportAllowFlag transportType: Int) =
        TransportRule(
            packageName,
            if (transportAllowList.shouldAllow(transportType)) {
                TransportRuleState.ALLOWED
            } else  {
                TransportRuleState.BLOCKED
            }
        )

    companion object {
        fun fromCommon(rule: Rule) =
            BasicRule(
                rule.packageName,
                rule.transportAllowList
            )
    }
}

//class RuleConverters {
//    @TypeConverter
//    fun fromBasicRule(rule: BasicRule): Int =
//}