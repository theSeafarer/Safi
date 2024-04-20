/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.theseafarer.safi.R


// FIXME not happy with how this looks rn
@Composable
fun RuleDropDownMenu(
    expanded: Boolean,
    state: HomeContract.RuleState,
    packageName: String? = null,
    onDismiss: (HomeContract.RuleState) -> Unit,
    onDeleteClicked: (() -> Unit)? = null
) {
    var currState: HomeContract.RuleState by remember { mutableStateOf(state) }
    LaunchedEffect(state) {
        currState = state
    }

    DropdownMenu(
        modifier = Modifier,
        expanded = expanded,
        onDismissRequest = {
            onDismiss(currState)
        },
    ) {
        if (packageName != null) {
            DropdownMenuItem(text = { Text(packageName) }, onClick = { })
        }
        DropdownMenuItem(
            enabled = false,
            text = {
                Text(
                    "Wi-Fi",
                    color = MenuDefaults.itemColors().textColor,
                    modifier = Modifier.padding(end = 12.dp)
                )
            },
            trailingIcon = {
                ChoiceBox(currState.wifiState) {
                    currState = currState.copy(wifiState = currState.wifiState.toggle())
                }
            },
            onClick = { }
        )
        DropdownMenuItem(
            enabled = false,
            text = { Text("Mobile data", color = MenuDefaults.itemColors().textColor) },
            trailingIcon = {
                ChoiceBox(currState.mobileDataState) {
                    currState = currState.copy(mobileDataState = currState.mobileDataState.toggle())
                }
            },
            onClick = { }
        )
        if (currState != state || packageName != null) {
            DropdownMenuItem(text = { }, onClick = { }, trailingIcon = {
                Row {
                    if (currState != state) {
                        IconButton(
                            onClick = { currState = state }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_reset_changes),
                                contentDescription = "Reset changes"
                            )
                        }
                    }
                    if (packageName != null && onDeleteClicked != null) {
                        IconButton(
                            onClick = { onDeleteClicked() },
                        ) {
                            Icon(
                                imageVector = Icons.TwoTone.Delete,
                                contentDescription = "Delete rule"
                            )
                        }
                    }
                }
            })
        }

    }
}