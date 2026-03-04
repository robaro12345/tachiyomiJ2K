package eu.kanade.tachiyomi.ui.setting

import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.util.VpnAppHelper
import uy.kohesive.injekt.injectLazy

class VpnSelectorDialog : DialogFragment() {
    private val preferences: PreferencesHelper by injectLazy()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        val vpnApps = VpnAppHelper.getAllVpnApps(context)

        // Separate installed and not installed apps
        val installedApps = vpnApps.filter { it.isInstalled }
        val notInstalledApps = vpnApps.filter { !it.isInstalled }

        // Create flat list for display (no section headers)
        val displayItems = mutableListOf<String>()
        val packageNames = mutableListOf<String>()

        if (installedApps.isNotEmpty()) {
            installedApps.forEach { vpn ->
                displayItems.add("✓ ${vpn.name}")
                packageNames.add(vpn.packageName)
            }
        }

        if (notInstalledApps.isNotEmpty()) {
            notInstalledApps.forEach { vpn ->
                displayItems.add("${vpn.name} (Not installed)")
                packageNames.add(vpn.packageName)
            }
        }

        // Add custom option at the end
        displayItems.add("─────────────────────")
        packageNames.add("")
        displayItems.add("Enter custom package name...")
        packageNames.add("__CUSTOM__")

        val currentVpn = preferences.vpnPackageName().get()
        val checkedItem = packageNames.indexOf(currentVpn).takeIf { it >= 0 } ?: -1

        return AlertDialog
            .Builder(context)
            .setTitle(R.string.pref_select_vpn_app)
            .setSingleChoiceItems(displayItems.toTypedArray(), checkedItem) { dialog, which ->
                val selectedPackage = packageNames.getOrNull(which) ?: ""

                when {
                    selectedPackage.isBlank() -> {
                        // Separator, do nothing
                    }
                    selectedPackage == "__CUSTOM__" -> {
                        showCustomPackageDialog()
                        dialog.dismiss()
                    }
                    else -> {
                        preferences.vpnPackageName().set(selectedPackage)
                        dialog.dismiss()
                    }
                }
            }.setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    private fun showCustomPackageDialog() {
        val context = requireContext()
        val input = EditText(context)
        input.hint = "com.example.vpnapp"
        input.setText(preferences.vpnPackageName().get())

        // Add padding
        val padding = (16 * context.resources.displayMetrics.density).toInt()
        input.setPadding(padding, padding, padding, padding)

        AlertDialog
            .Builder(context)
            .setTitle("Enter VPN Package Name")
            .setMessage("Enter the package name of your VPN app")
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val packageName = input.text.toString().trim()
                if (packageName.isNotEmpty()) {
                    preferences.vpnPackageName().set(packageName)
                }
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
