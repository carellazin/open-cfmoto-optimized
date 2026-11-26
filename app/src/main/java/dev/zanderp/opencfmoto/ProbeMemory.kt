// SPDX-License-Identifier: AGPL-3.0-or-later
// Part of OpenCfMoto (performance fork). Free software under the GNU AGPL v3 or later; see LICENSE.
package dev.zanderp.opencfmoto

import android.content.Context
import java.net.Inet4Address
import java.net.InetAddress

/**
 * [perf] Remembers the last EasyConn endpoint (host + port) that accepted our MDNS_RESPOND probe, so
 * the next connect can try it first instead of waiting on NSD discovery. Purely a hint: a stale or
 * wrong entry costs one fast probe attempt and then the normal discovery runs unchanged.
 */
object ProbeMemory {
    private const val PREFS = "probe_memory"
    private const val KEY_HOST = "last_host"
    private const val KEY_PORT = "last_port"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(ctx: Context): Pair<Inet4Address, Int>? = try {
        val p = prefs(ctx)
        val host = p.getString(KEY_HOST, null)
        val port = p.getInt(KEY_PORT, 0)
        if (host.isNullOrEmpty() || port !in 1..65535) {
            null
        } else {
            // Literal IPv4 only — InetAddress.getByName on a dotted quad never does a DNS lookup.
            if (!host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) null
            else (InetAddress.getByName(host) as? Inet4Address)?.let { it to port }
        }
    } catch (_: Exception) {
        null
    }

    fun put(ctx: Context, host: Inet4Address, port: Int) {
        try {
            prefs(ctx).edit()
                .putString(KEY_HOST, host.hostAddress)
                .putInt(KEY_PORT, port)
                .apply()
        } catch (_: Exception) {
        }
    }
}
