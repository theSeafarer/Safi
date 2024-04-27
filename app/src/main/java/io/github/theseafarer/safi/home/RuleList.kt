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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import io.github.theseafarer.safi.data.TransportRuleState
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
            .padding(bottom = 16.dp)
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
//        HorizontalDivider(
//            Modifier
//                .fillMaxWidth()
//                .align(Alignment.BottomCenter)
//        )
    }
}

@Preview
@Composable
private fun RuleBoxPreview() {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .fillMaxWidth(1f)
            .height(90.dp)
    ) {
        RuleBox(
            iconPainter = rememberDrawablePainter(drawable = null),
            displayName = "Display Name",
            state = HomeContract.AppRuleState(
                "fake.package.name",
                HomeContract.RuleState(TransportRuleState.BLOCKED, TransportRuleState.ALLOWED),
                false
            ),
            onDeleteClicked = {}
        ) {

        }
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
                    36.dp,
                    12.dp,
                    Color(ColorUtils.blendARGB(MaterialTheme.colorScheme.error.toArgb(),
                        MaterialTheme.colorScheme.surfaceContainer.toArgb(),
                        0.6f))
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
        VerticalDivider() //FIXME
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

internal fun Modifier.dashBorder(color: Color) = this.drawBehind {
    drawRect(
        color = color,
        size = Size(width = size.width - 10.dp.toPx(), height = size.height - 10.dp.toPx()),
        topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
        style = Stroke(
            width = 5.dp.toPx(),
            join = StrokeJoin.Miter,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(40f, 40f),
                phase = 0f
            )
        )
    )
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