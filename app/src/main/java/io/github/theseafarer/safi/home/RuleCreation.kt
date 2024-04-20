/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.home

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.theseafarer.safi.data.TransportRuleState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


sealed interface RuleCreationSheetState {
    data object Loading : RuleCreationSheetState
    data class AppSelection(val apps: List<HomeContract.AppInfo>) : RuleCreationSheetState
    data class RuleCreation(val selectedApp: HomeContract.AppInfo) : RuleCreationSheetState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleCreationSheet(
    defaultRules: HomeContract.RuleState,
    getApps: suspend () -> List<HomeContract.AppInfo>,
    loadAppIcon: suspend (packageName: String) -> Drawable?,
    onDismissed: () -> Unit,
    onRuleCreated: (HomeContract.AppRuleState) -> Unit
) {
    var state: RuleCreationSheetState by remember {
        mutableStateOf(RuleCreationSheetState.Loading)
    }

    val sheetState = rememberModalBottomSheetState()
    val isSheetFullyExpanded by remember {
        derivedStateOf {
            false//sheetState.currentValue == SheetValue.Expanded FIXME
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val apps = getApps()
            if (state == RuleCreationSheetState.Loading && sheetState.hasExpandedState) {
                state = RuleCreationSheetState.AppSelection(apps)
            }
            //FIXME think of the errors! the errors!!
        }
    }

    val lazyColumnState = rememberLazyListState()
    val colorScheme = MaterialTheme.colorScheme
    val titleBarColor by remember {
        derivedStateOf {
            if (state is RuleCreationSheetState.AppSelection
                && (lazyColumnState.firstVisibleItemIndex > 0
                        || lazyColumnState.firstVisibleItemScrollOffset > 0)
            ) {
                colorScheme.surfaceContainerHighest
            } else {
                colorScheme.surfaceContainer
            }
        }
    }
    ModalBottomSheet(
        containerColor = titleBarColor,
        onDismissRequest = { onDismissed() },
        sheetState = sheetState,
        shape = if (isSheetFullyExpanded) RoundedCornerShape(
            0,
            0
        ) else BottomSheetDefaults.ExpandedShape
    ) {
        // FIXME animation glitches
        AnimatedContent(targetState = state, label = "sheet") {
            Column {
                when (it) {
                    RuleCreationSheetState.Loading -> {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                        ) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }

                    is RuleCreationSheetState.AppSelection -> {
                        RuleCreationSheetAppList(
                            apps = it.apps,
                            loadAppIcon = loadAppIcon,
                            lazyListState = lazyColumnState
                        ) { appInfo ->
                            state = RuleCreationSheetState.RuleCreation(appInfo)
                        }
                    }

                    is RuleCreationSheetState.RuleCreation -> {
                        RuleCreationSheetRules(
                            appInfo = it.selectedApp,
                            loadAppIcon = loadAppIcon,
                            defaultRules = defaultRules
                        ) {
                            onRuleCreated(it)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.RuleCreationSheetAppList(
    lazyListState: LazyListState,
    apps: List<HomeContract.AppInfo>,
    loadAppIcon: suspend (packageName: String) -> Drawable?,
    onAppSelected: (HomeContract.AppInfo) -> Unit
) {
//    var colored: Boolean by remember { mutableStateOf(false) }

    Text(
        modifier = Modifier
            .padding(horizontal = 24.dp),
        text = "Select an app",
        style = MaterialTheme.typography.titleLarge
    )
    Spacer(modifier = Modifier.height(16.dp))
    LazyColumn(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
        state = lazyListState
    ) {
        itemsIndexed(apps, key = { ix, _ -> ix }) { _, item ->
            var drawable: Drawable? by remember { mutableStateOf(null) }
            val iconPainter = rememberDrawablePainter(drawable = drawable)
            LaunchedEffect(item.packageName) {
                withContext(Dispatchers.Default) {
                    drawable = loadAppIcon(item.packageName)
                }
            }
            AppListingItem(item.displayName, iconPainter) {
                onAppSelected(item)
            }
        }
    }

}

@Composable
private fun AppListingItem(displayName: String, icon: Painter, onClick: () -> Unit) {
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.width(24.dp))
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
//                val colorScheme = MaterialTheme.colorScheme
//            Canvas(modifier = Modifier.size(48.dp)) {
//                drawCircle(color = colorScheme.tertiary)
//            }
                Image(
                    painter = icon,
                    contentDescription = "$displayName icon",
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = displayName, style = MaterialTheme.typography.titleMedium)
        }
        HorizontalDivider(
            modifier = Modifier
                .padding(start = (48 + 16 + 12).dp)
                .fillMaxWidth()
                .align(Alignment.BottomEnd),
        )
    }
}

//FIXME
@Composable
private fun ColumnScope.RuleCreationSheetRules(
    appInfo: HomeContract.AppInfo,
    loadAppIcon: suspend (packageName: String) -> Drawable?,
    defaultRules: HomeContract.RuleState,
    onAddClicked: (HomeContract.AppRuleState) -> Unit
) {

    var rules by remember { mutableStateOf(defaultRules) }
    var drawable: Drawable? by remember { mutableStateOf(null) }

    LaunchedEffect(appInfo.packageName) {
        rules = defaultRules
        withContext(Dispatchers.Main) {
            drawable = loadAppIcon(appInfo.packageName)
        }
    }

    Row(
        Modifier
            .fillMaxWidth(0.8f)
            .align(Alignment.CenterHorizontally)
    ) {
        val iconPainter = rememberDrawablePainter(drawable = drawable)
        Image(painter = iconPainter, contentDescription = "", Modifier.size(60.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = appInfo.displayName, modifier = Modifier.align(Alignment.CenterVertically))
    }
    Spacer(modifier = Modifier.height(24.dp))
    Card(
        modifier = Modifier
            .padding(horizontal = 36.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(top = 16.dp, bottom = 8.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Wi-Fi", modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically)
                    .weight(3f)
            )
            ChoiceBox(rules.wifiState) { rules = rules.copy(wifiState = rules.wifiState.toggle()) }
        }
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = 16.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Mobile data", modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.CenterVertically)
                    .weight(3f)
            )
            ChoiceBox(rules.mobileDataState) {
                rules = rules.copy(mobileDataState = rules.mobileDataState.toggle())
            }
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = {
            onAddClicked(
                HomeContract.AppRuleState(
                    packageName = appInfo.packageName,
                    rule = rules
                )
            )
        },
        modifier = Modifier
            .align(Alignment.End)
            .padding(end = 36.dp, bottom = 16.dp),
    ) {
        Text("Add rule")
    }
}

@Preview
@Composable
private fun RuleCreationSheetRulesPreview() {
    Surface {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            RuleCreationSheetRules(
                appInfo = HomeContract.AppInfo(10, "com.something.other", "App name"),
                loadAppIcon = { null },
                defaultRules = HomeContract.RuleState(

                )
            ) {

            }
        }
    }
}