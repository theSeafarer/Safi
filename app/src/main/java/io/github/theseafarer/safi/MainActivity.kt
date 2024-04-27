/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.theseafarer.safi.home.HomeScreen
import io.github.theseafarer.safi.home.HomeViewModel
import io.github.theseafarer.safi.ui.theme.SafiTheme
import io.github.theseafarer.safi.vpn.Command
import io.github.theseafarer.safi.vpn.VpnService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        setContent {
            // TODO: setup proper navigation
            val vpnReqLauncher =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == RESULT_OK) {
                        startService(
                            Intent(
                                this@MainActivity,
                                VpnService::class.java
                            ).apply {
                            }
                        )
                    } else {
                        Log.e("MAINMAIN", "$it")
                    }
                }

            val homeViewModel = HomeViewModel(App.ruleRepository, App.vpnServiceManager)

            SafiTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showInfo by remember { mutableStateOf(false) }

                    AnimatedContent(targetState = showInfo, label = "Main transition") {
                        if (it) {
                            InfoScreen {
                                showInfo = false
                            }
                        } else {
                            HomeScreen(homeViewModel, onInfoClicked = { showInfo = true }) {
                                val reqIntent = android.net.VpnService.prepare(this@MainActivity)
                                reqIntent?.let {
                                    vpnReqLauncher.launch(it)
                                } ?: startService(
                                    Intent(
                                        this@MainActivity,
                                        VpnService::class.java
                                    ).apply {
                                        putExtra(
                                            VpnService.EXTRA_COMMAND,
                                            if (it) {
                                                Command.START
                                            } else {
                                                Command.STOP
                                            } as Parcelable
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}