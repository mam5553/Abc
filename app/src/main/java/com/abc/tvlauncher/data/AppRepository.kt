package com.abc.tvlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

/** Reads installed apps from the system. Cheap enough to call on a background thread any time. */
class AppRepository(private val ctx: Context) {
    private val pm: PackageManager get() = ctx.packageManager

    /** Packages that act as a Home screen (stock Google TV home, Sony home, ...) other than us. */
    fun otherHomePackages(): Set<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_ALL)
            .mapNotNull { it.activityInfo?.packageName }
            .filter { it != ctx.packageName }
            .toSet()
    }

    fun loadLaunchable(): List<AppEntry> {
        val home = otherHomePackages()
        val stamps = pm.getInstalledPackages(0).associate { it.packageName to it.lastUpdateTime }
        val out = LinkedHashMap<String, AppEntry>()
        // Leanback (TV) entry points first so TV apps launch their TV activity.
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(q, 0)) {
                val ai = ri.activityInfo ?: continue
                val pkg = ai.packageName
                if (pkg == ctx.packageName || pkg in home || pkg in out) continue
                out[pkg] = AppEntry(
                    pkg = pkg,
                    label = ri.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: pkg,
                    component = ComponentName(pkg, ai.name),
                    updated = stamps[pkg] ?: 0L,
                )
            }
        }
        return out.values.sortedBy { it.label.lowercase() }
    }

    fun loadAllPackages(): List<PkgInfo> {
        val launchable = HashSet<String>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(cat), 0)
                .mapNotNullTo(launchable) { it.activityInfo?.packageName }
        }
        val updated = pm.getInstalledPackages(0).associate { it.packageName to it.lastUpdateTime }
        return pm.getInstalledApplications(0).map { ai ->
            val system = ai.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            PkgInfo(
                pkg = ai.packageName,
                label = ai.loadLabel(pm)?.toString()?.takeIf { it.isNotBlank() } ?: ai.packageName,
                preinstalled = system,
                enabled = ai.enabled,
                hasLauncher = ai.packageName in launchable,
                updated = updated[ai.packageName] ?: 0L,
                note = Bloat.noteFor(ai.packageName),
            )
        }.filter { it.pkg != ctx.packageName }
    }
}
