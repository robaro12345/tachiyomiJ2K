package eu.kanade.tachiyomi.util

import android.content.Context
import android.content.pm.PackageManager

data class VpnApp(
    val name: String,
    val packageName: String,
    val isInstalled: Boolean,
)

object VpnAppHelper {
    // Popular VPN apps with their package names
    val KNOWN_VPN_APPS =
        listOf(
            VpnApp("Cloudflare One Agent", "com.cloudflare.cloudflareoneagent", false),
            VpnApp("Cloudflare WARP", "com.cloudflare.onedotonedotonedotone", false),
            VpnApp("NordVPN", "com.nordvpn.android", false),
            VpnApp("ExpressVPN", "com.expressvpn.vpn", false),
            VpnApp("ProtonVPN", "net.protonvpn.android", false),
            VpnApp("Surfshark", "com.surfshark.vpnclient.android", false),
            VpnApp("Private Internet Access", "com.privateinternetaccess.android", false),
            VpnApp("CyberGhost VPN", "de.mobileconcepts.cyberghost", false),
            VpnApp("IPVanish VPN", "com.ixolit.ipvanish", false),
            VpnApp("TunnelBear VPN", "com.tunnelbear.android", false),
            VpnApp("Hotspot Shield", "hotspotshield.android.vpn", false),
            VpnApp("Windscribe VPN", "com.windscribe.vpn", false),
            VpnApp("Hide.me VPN", "hideme.android.vpn", false),
            VpnApp("Mullvad VPN", "net.mullvad.mullvadvpn", false),
            VpnApp("VyprVPN", "com.goldenfrog.vyprvpn.app", false),
            VpnApp("OpenVPN Connect", "net.openvpn.openvpn", false),
            VpnApp("WireGuard", "com.wireguard.android", false),
        )

    fun getInstalledVpnApps(context: Context): List<VpnApp> {
        val packageManager = context.packageManager
        val installedApps = mutableListOf<VpnApp>()

        // Check known VPN apps
        KNOWN_VPN_APPS.forEach { vpnApp ->
            val isInstalled =
                try {
                    packageManager.getPackageInfo(vpnApp.packageName, 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    false
                }

            if (isInstalled) {
                installedApps.add(vpnApp.copy(isInstalled = true))
            }
        }

        // Also scan for other VPN apps
        val allApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        allApps.forEach { appInfo ->
            // Check if app is likely a VPN (contains "vpn" in package or label)
            val packageName = appInfo.packageName.lowercase()
            val appLabel = packageManager.getApplicationLabel(appInfo).toString().lowercase()

            if ((packageName.contains("vpn") || appLabel.contains("vpn")) &&
                !installedApps.any { it.packageName == appInfo.packageName }
            ) {
                val name = packageManager.getApplicationLabel(appInfo).toString()
                installedApps.add(VpnApp(name, appInfo.packageName, true))
            }
        }

        return installedApps.sortedBy { it.name }
    }

    fun getAllVpnApps(context: Context): List<VpnApp> {
        val installed = getInstalledVpnApps(context).map { it.packageName }.toSet()

        return KNOWN_VPN_APPS.map { vpnApp ->
            vpnApp.copy(isInstalled = installed.contains(vpnApp.packageName))
        } +
            getInstalledVpnApps(context).filter {
                !KNOWN_VPN_APPS.any { known -> known.packageName == it.packageName }
            }
    }

    fun getVpnAppName(
        context: Context,
        packageName: String,
    ): String {
        // Check known apps first
        KNOWN_VPN_APPS.find { it.packageName == packageName }?.let {
            return it.name
        }

        // Try to get app label
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
