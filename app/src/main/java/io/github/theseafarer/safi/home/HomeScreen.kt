/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.home

import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.theseafarer.safi.App
import io.github.theseafarer.safi.R
import io.github.theseafarer.safi.collectInLaunchedEffect
import io.github.theseafarer.safi.data.TransportRuleState
import io.github.theseafarer.safi.ui.theme.lessRoundedShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeContract = HomeViewModel(App.ruleRepository, App.vpnServiceManager),
    sendServiceCommand: (Boolean) -> Unit,
) {
    val context = LocalContext.current

    val uiState by viewModel.state.collectAsState()
    viewModel.effect.collectInLaunchedEffect { effect ->
        when (effect) {
            is HomeContract.Effect.FetchAppList -> withContext(Dispatchers.Default) {
                val apps = context.packageManager.getInstalledApplications(effect.flags)
                val infos = apps.map {
                    HomeContract.AppInfo(
                        it.uid,
                        it.packageName,
                        it.loadLabel(context.packageManager).toString()
                    )
                }
                viewModel.onEvent(HomeContract.Event.AppListFetched(infos))
            }

            is HomeContract.Effect.SendServiceCommand -> sendServiceCommand(effect.shouldEnable)
        }
    }

    var shouldShowRuleCreationSheet by remember { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .consumeWindowInsets(WindowInsets.statusBars)
                .padding(WindowInsets.statusBars.asPaddingValues())
        ) {
            minWidth
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    actions = {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(Icons.TwoTone.Info, "")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
                Box(
                    Modifier
                        .height(140.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    MainSwitch(state = uiState.switchState) {
                        viewModel.onEvent(HomeContract.Event.SwitchClicked(it))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                DefaultRuleBox(uiState.defaultRuleState) {
                    viewModel.onEvent(
                        HomeContract.Event.DefaultRuleEdited(
                            it
                        )
                    )
                }
                Spacer(modifier = Modifier.height(36.dp))
                RuleListBox(
                    rules = uiState.appRules,
                    onRuleDeleted = { viewModel.onEvent(HomeContract.Event.AppRuleDeleted(it)) },
                    onAddRuleClicked = { shouldShowRuleCreationSheet = true }) {
                    viewModel.onEvent(
                        HomeContract.Event.AppRuleEdited(
                            it
                        )
                    )
                }
            }
            if (shouldShowRuleCreationSheet) {
                RuleCreationSheet(
                    defaultRules = uiState.defaultRuleState,
                    getApps = {
                        withContext(Dispatchers.Default) {
                            context.packageManager
                                .getInstalledApplications(0)
                                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                                .map {
                                    HomeContract.AppInfo(
                                        it.uid,
                                        it.packageName,
                                        it.loadLabel(context.packageManager).toString()
                                    )
                                }
                        }
                    },
                    loadAppIcon = {
                        withContext(Dispatchers.Default) {
                            context.packageManager
                                .runCatching { getApplicationIcon(it) }
                                .getOrNull()
                        }
                    },
                    onDismissed = { shouldShowRuleCreationSheet = false }
                ) {
                    shouldShowRuleCreationSheet = false
                    viewModel.onEvent(HomeContract.Event.NewRuleCreated(it))
                }
            }
        }
    }
}

@Composable
private fun BoxScope.MainSwitch(
    state: HomeContract.SwitchState,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        modifier = Modifier
            .align(Alignment.Center)
            .scale(2.8f),
        checked = state != HomeContract.SwitchState.STOPPED,
        onCheckedChange = onCheckedChange,
        thumbContent = {
            when (state) {
                HomeContract.SwitchState.STOPPED -> {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        modifier = Modifier.scale(0.7f),
                        contentDescription = null
                    )
                }

                HomeContract.SwitchState.STARTING -> {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        modifier = Modifier
                            .scale(0.7f)
                            .rotate(90f),
                        contentDescription = null
                    )
                }

                HomeContract.SwitchState.STARTED -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        modifier = Modifier.scale(0.7f),
                        contentDescription = null
                    )
                }

                HomeContract.SwitchState.RESTARTING -> {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        modifier = Modifier
                            .scale(0.7f)
                            .rotate(90f),
                        contentDescription = null
                    )
                }
            }
        }
    )
}

@Composable
fun ColumnScope.DefaultRuleBox(
    defaultRule: HomeContract.RuleState,
    onDismiss: (HomeContract.RuleState) -> Unit
) {
    SideEffect {
        Log.d("DROPDROP", "DefaultRuleBox: $defaultRule")
    }
    var showMenu: Boolean by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth(0.9f)
            .height(70.dp)
            .align(Alignment.CenterHorizontally)
            .clip(lessRoundedShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { showMenu = true },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .weight(3f)
                .padding(start = 16.dp),
            text = "Default Rule",
            style = MaterialTheme.typography.titleMedium
        )

        DotIndicator(modifier = Modifier.weight(1f), state = defaultRule.wifiState)
        VerticalDivider(Modifier.padding(vertical = 12.dp))
        DotIndicator(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f),
            state = defaultRule.mobileDataState
        )

        RuleDropDownMenu(expanded = showMenu, state = defaultRule, onDismiss = {
            showMenu = false
            onDismiss(it)
        })
    }
}

//FIXME
internal fun Modifier.diagonalStripes(width: Dp, startingOffset: Dp, color: Color): Modifier =
    drawBehind {
        val actualWidth = width.toPx()
        val actualOffset = startingOffset.toPx()

        var x = 0

        clipRect {
            while (size.width - (actualOffset + (actualWidth * x) + (actualWidth / 5f)) >= 0) {
                drawLine(
                    color = color,
                    start = Offset(
                        y = size.height + actualWidth,
                        x = actualOffset + (x * actualWidth) + (actualWidth / 5f)
                    ),
                    end = Offset(
                        y = 0f - actualWidth,
                        x = actualOffset + (x * actualWidth) + actualWidth
                    ),
                    strokeWidth = actualWidth
                )
                x += 2
            }
        }
    }

@Preview(apiLevel = 34)
@Composable
private fun HomeScreenPreview() {
    HomeScreen(object : HomeContract {
        override val state: StateFlow<HomeContract.UiState> = MutableStateFlow(
            HomeContract.UiState(
                defaultRuleState = HomeContract.RuleState(
                    TransportRuleState.BLOCKED,
                    TransportRuleState.ALLOWED
                )
            )
        )
        override val effect: SharedFlow<HomeContract.Effect> = MutableSharedFlow()

        override fun onEvent(event: HomeContract.Event) {}
    }) { }
}