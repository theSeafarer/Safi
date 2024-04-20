/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest

interface ScreenContract<STATE, EVENT, EFFECT> {
    val state: StateFlow<STATE>
    val effect: SharedFlow<EFFECT>
    fun onEvent(event: EVENT)
}

@SuppressLint("ComposableNaming")
@Composable
fun <T> SharedFlow<T>.collectInLaunchedEffect(function: suspend (value: T) -> Unit) {
    //FIXME
    val sharedFlow = this
    LaunchedEffect(key1 = sharedFlow) {
        sharedFlow.collectLatest(function)
    }
}
