/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.theseafarer.safi.data.TransportRuleState
import io.github.theseafarer.safi.ui.theme.GreenAllowed
import io.github.theseafarer.safi.ui.theme.GreyUnknown
import io.github.theseafarer.safi.ui.theme.RedBlocked
import io.github.theseafarer.safi.ui.theme.YellowCustom
import io.github.theseafarer.safi.ui.theme.lessRoundedShape

@Composable
internal fun ChoiceBox(
    state: TransportRuleState,
    onStateChanged: () -> Unit
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape = lessRoundedShape)
            .clip(lessRoundedShape)
            .clickable { onStateChanged() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(8.dp))
        Text(state.name, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.width(8.dp))
        DotIndicator(modifier = Modifier.size(24.dp), state = state)
        Spacer(modifier = Modifier.width(4.dp))
    }
}

@Composable
fun DotIndicator(
    modifier: Modifier,
    state: TransportRuleState
) {
    DotIndicator(
        modifier, color = when (state) {
            TransportRuleState.BLOCKED -> RedBlocked
            TransportRuleState.ALLOWED -> GreenAllowed
            TransportRuleState.CUSTOM -> YellowCustom
            TransportRuleState.UNKNOWN -> GreyUnknown
        }
    )
}

@Composable
fun DotIndicator(
    modifier: Modifier,
    color: Color
) {
    val density = LocalDensity.current
    Canvas(modifier = modifier) {
        with(density) {
            drawCircle(color = color, radius = 6.dp.toPx())
        }
    }
}