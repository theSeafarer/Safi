/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.home

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.theseafarer.safi.ui.theme.roundedShape

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.RuleListBox(
    rules: List<HomeContract.AppRuleState>,
    onAddRuleClicked: () -> Unit,
    onRuleDeleted: (String) -> Unit,
    onRuleEdited: (HomeContract.AppRuleState) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .navigationBarsPadding()
            .align(Alignment.CenterHorizontally)
            .background(MaterialTheme.colorScheme.surfaceContainerLow, roundedShape)
            .clip(roundedShape),
    ) {
        LazyColumn {
            if (rules.isEmpty()) {
                item {
                    Text(
                        modifier = Modifier.padding(24.dp),
                        text = "No rules yet. tap on the + button to add one",
                        style = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.secondary),
                    )
                }
            } else {
                stickyHeader { RuleListHeader() }
                items(rules, key = { it.packageName }) { rule ->
                    val pm = LocalContext.current.packageManager
                    var displayName by remember { mutableStateOf("") }
                    var iconDrawable: Drawable? by remember { mutableStateOf(null) }
                    LaunchedEffect(rule.packageName) {
                        try {
                            displayName =
                                pm.getApplicationInfo(rule.packageName, 0).loadLabel(pm).toString()
                            iconDrawable = pm.getApplicationIcon(rule.packageName)
                        } catch (e: Exception) {
                            Log.e("HomeScreen", "shieeet ${e.javaClass.name}: ${e.message}") //FIXME
                        }
                    }
                    RuleBox(
                        rememberDrawablePainter(drawable = iconDrawable),
                        displayName,
                        rule,
                        onRuleDeleted
                    ) {
                        onRuleEdited(rule.copy(rule = it))
                    }
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddRuleClicked() }
                        .padding(vertical = 16.dp)
                ) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(contentAlignment = Alignment.Center) {
                        val colorScheme = MaterialTheme.colorScheme
                        Canvas(modifier = Modifier.size(48.dp)) {
                            drawCircle(color = colorScheme.tertiary)
                        }
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "",
                            tint = colorScheme.onTertiary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "Add a rule", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun RuleListHeader() {
    Box(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Spacer(modifier = Modifier.width(80.dp))
            Text(
                text = "App name", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(3f)
            )
            Text(
                text = "Wi-Fi", style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Cell", style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        HorizontalDivider(
            Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun RuleBox(
    iconPainter: Painter,
    displayName: String,
    state: HomeContract.AppRuleState,
    onDeleteClicked: (String) -> Unit,
    onDismiss: (HomeContract.RuleState) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showMenu = true }
            .then(
                if (state.applied) Modifier else Modifier.diagonalStripes(
                    60.dp,
                    24.dp,
                    Color.Gray
                )
            )
            .padding(vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Image(
            painter = iconPainter,
            contentDescription = "",
            modifier = Modifier
                .padding(6.dp)
                .size(36.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = displayName, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(3f)
        )
        DotIndicator(
            modifier = Modifier
                .size(12.dp)
                .weight(1f)
                .aspectRatio(1f), state = state.rule.wifiState
        )
        VerticalDivider(Modifier.padding(vertical = 12.dp)) //FIXME
        DotIndicator(
            modifier = Modifier
                .size(12.dp)
                .weight(1f)
                .aspectRatio(1f),
            state = state.rule.mobileDataState
        )
        Spacer(modifier = Modifier.width(16.dp))
        RuleDropDownMenu(
            expanded = showMenu,
            state = state.rule,
            packageName = state.packageName,
            onDeleteClicked = { onDeleteClicked(state.packageName) },
            onDismiss = {
                showMenu = false
                onDismiss(it)
            })
    }
}