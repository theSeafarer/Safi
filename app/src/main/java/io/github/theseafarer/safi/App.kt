/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi

import android.app.Application
import androidx.room.Room
import io.github.theseafarer.safi.data.RuleRepository
import io.github.theseafarer.safi.data.RuleRepositoryImpl
import io.github.theseafarer.safi.data.local.RulePrefs
import io.github.theseafarer.safi.data.local.SafiDb
import io.github.theseafarer.safi.vpn.VpnServiceManager

class App : Application() {

    companion object {
        lateinit var db: SafiDb
        lateinit var rulePrefs: RulePrefs
        lateinit var ruleRepository: RuleRepository
        lateinit var vpnServiceManager: VpnServiceManager
    }

    override fun onCreate() {
        super.onCreate()
        //init
        db = Room.databaseBuilder(this, SafiDb::class.java, "thing")
            .build()
        rulePrefs = RulePrefs(this)
        ruleRepository = RuleRepositoryImpl(db.basicRuleDao(), rulePrefs)
        vpnServiceManager = VpnServiceManager(ruleRepository)
    }
}