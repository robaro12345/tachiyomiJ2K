package eu.kanade.tachiyomi.network

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class UniversalVpnAutoTriggerInterceptor(
    private val context: Context,
    private val latencyThresholdMs: Long = 5000,
    private val vpnPackageName: String = "",
    private val enabled: Boolean = true,
) : Interceptor {
    companion object {
        private const val SLOW_REQUEST_THRESHOLD = 3
        private const val VPN_TRIGGER_COOLDOWN = 60_000L // 1 minute
    }

    private val consecutiveSlowRequests = AtomicInteger(0)
    private val lastVpnTriggerTime = AtomicLong(0L)

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!enabled || vpnPackageName.isBlank()) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val startTime = System.nanoTime()

        val response =
            try {
                chain.proceed(request)
            } catch (e: Exception) {
                Timber.e(e, "Request failed")
                throw e
            }

        val endTime = System.nanoTime()
        val durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime)

        Timber.d("Request to ${request.url.host} took ${durationMs}ms")

        if (durationMs > latencyThresholdMs) {
            val currentSlow = consecutiveSlowRequests.incrementAndGet()
            Timber.w("Slow request detected: ${durationMs}ms ($currentSlow/$SLOW_REQUEST_THRESHOLD)")

            if (shouldTriggerVpn(currentSlow)) {
                Timber.w("Network is consistently slow. Opening VPN: $vpnPackageName")
                openVpnApp()
                consecutiveSlowRequests.set(0)
                lastVpnTriggerTime.set(System.currentTimeMillis())
            }
        } else {
            decrementSlowRequests()
        }

        return response
    }

    private fun decrementSlowRequests() {
        while (true) {
            val current = consecutiveSlowRequests.get()
            if (current <= 0) break
            if (consecutiveSlowRequests.compareAndSet(current, current - 1)) break
        }
    }

    private fun shouldTriggerVpn(currentSlow: Int): Boolean {
        if (currentSlow < SLOW_REQUEST_THRESHOLD) {
            return false
        }

        val now = System.currentTimeMillis()
        val lastTrigger = lastVpnTriggerTime.get()
        val timeSinceLastTrigger = now - lastTrigger
        if (timeSinceLastTrigger < VPN_TRIGGER_COOLDOWN) {
            Timber.d("VPN trigger on cooldown (${timeSinceLastTrigger}ms / ${VPN_TRIGGER_COOLDOWN}ms)")
            return false
        }

        return true
    }

    private fun openVpnApp() {
        try {
            // Check if app is installed
            if (!isAppInstalled(vpnPackageName)) {
                Timber.w("VPN app not installed: $vpnPackageName")
                openPlayStore()
                return
            }

            // Try to open the VPN app
            val intent = context.packageManager.getLaunchIntentForPackage(vpnPackageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Timber.i("Opened VPN app: $vpnPackageName")
            } else {
                Timber.w("No launch intent found for: $vpnPackageName")
                openPlayStore()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to open VPN app: $vpnPackageName")
            openPlayStore()
        }
    }

    private fun isAppInstalled(packageName: String): Boolean =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    private fun openPlayStore() {
        try {
            val playStoreIntent =
                Intent(Intent.ACTION_VIEW).apply {
                    data = "market://details?id=$vpnPackageName".toUri()
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(playStoreIntent)
            Timber.i("Opened Play Store for: $vpnPackageName")
        } catch (e: Exception) {
            Timber.e(e, "Failed to open Play Store")
        }
    }
}
