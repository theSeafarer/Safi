/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package io.github.theseafarer.safi.data

import android.net.NetworkCapabilities.TRANSPORT_BLUETOOTH
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import androidx.annotation.IntDef

@Retention(AnnotationRetention.SOURCE)
@IntDef(TRANSPORT_CELLULAR, TRANSPORT_WIFI, TRANSPORT_BLUETOOTH, TRANSPORT_ETHERNET, -1)
annotation class TransportAllowFlag

@JvmInline
value class TransportAllowList (val value: Int) {

    constructor(
        allowCellular: Boolean = false,
        allowWifi: Boolean = false,
        allowEthernet: Boolean = false,
        allowBluetooth: Boolean = false,
        allowOther: Boolean = false
    ) : this(
        (allowCellular.toInt() shl TRANSPORT_CELLULAR)
                or (allowWifi.toInt() shl TRANSPORT_WIFI)
                or (allowEthernet.toInt() shl TRANSPORT_ETHERNET)
                or (allowBluetooth.toInt() shl TRANSPORT_BLUETOOTH)
                or (if (allowOther) 0b111111 shl 4 else 0)
    )

    private fun Int.flagged() =
        1 shl this

    fun shouldAllowCellular(): Boolean {
        val flag = TRANSPORT_CELLULAR.flagged()
        return value and flag == flag
    }

    fun shouldAllowWifi(): Boolean {
        val flag = TRANSPORT_WIFI.flagged()
        return value and flag == flag
    }

    fun shouldAllow(@TransportAllowFlag transportType: Int): Boolean {
        if (transportType == -1) return true
        val flag = transportType.flagged()
        return value and flag == flag
    }

    fun shouldBlock(@TransportAllowFlag transportType: Int): Boolean =
        !shouldAllow(transportType)

    fun toggle(@TransportAllowFlag transportType: Int): TransportAllowList =
        TransportAllowList(value xor transportType.flagged())

}