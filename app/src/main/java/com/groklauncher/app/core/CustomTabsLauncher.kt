package com.groklauncher.app.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.browser.customtabs.CustomTabsIntent

object CustomTabsLauncher {
    fun canLaunchCustomTabs(context: Context): Boolean = getCustomTabsProviderPackage(context) != null

    fun launch(context: Context, uri: Uri) {
        val provider = getCustomTabsProviderPackage(context)
        if (provider == null) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            return
        }

        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()

        intent.intent.setPackage(provider)
        intent.launchUrl(context, uri)
    }

    private fun getCustomTabsProviderPackage(context: Context): String? {
        val pm = context.packageManager
        val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))

        val resolvedActivityList = if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(
                activityIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(activityIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }

        val packagesSupportingCustomTabs = resolvedActivityList
            .map { it.activityInfo.packageName }
            .filter { pkg ->
                val serviceIntent = Intent("androidx.browser.customtabs.action.CustomTabsService").setPackage(pkg)
                if (Build.VERSION.SDK_INT >= 33) {
                    pm.resolveService(serviceIntent, PackageManager.ResolveInfoFlags.of(0)) != null
                } else {
                    @Suppress("DEPRECATION")
                    pm.resolveService(serviceIntent, 0) != null
                }
            }

        return packagesSupportingCustomTabs.firstOrNull()
    }
}

