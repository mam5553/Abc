package com.abc.tvlauncher.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

data class HdmiInput(val id: String, val label: String)

/** Everything the launcher asks Android to do lives here, so the UI stays declarative. */
object Actions {
    fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    /** Starts [intent]; returns false (and shows [failMsg]) when this TV has no screen for it. */
    fun tryStart(ctx: Context, intent: Intent, failMsg: String? = null): Boolean = try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        failMsg?.let { toast(ctx, it) }; false
    } catch (_: SecurityException) {
        failMsg?.let { toast(ctx, it) }; false
    }

    fun launchApp(ctx: Context, pkg: String?): Boolean {
        if (pkg.isNullOrBlank()) return false
        val pm = ctx.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
        if (intent == null) { toast(ctx, "Can't open that app"); return false }
        return tryStart(ctx, intent, "Can't open that app")
    }

    /** The system page for an app, where Disable / Force stop / Uninstall live. */
    fun openAppSettings(ctx: Context, pkg: String) {
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
        if (!tryStart(ctx, details)) {
            if (!tryStart(ctx, Intent(Settings.ACTION_APPLICATION_SETTINGS))) {
                tryStart(ctx, Intent(Settings.ACTION_SETTINGS), "This TV blocks the app settings page")
            }
        }
    }

    /** HDMI inputs the TV knows about (includes CEC-named devices on some models). */
    fun hdmiInputs(ctx: Context): List<HdmiInput> = runCatching {
        val tm = ctx.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        tm.tvInputList
            .filter { it.type == TvInputInfo.TYPE_HDMI }
            .map { info ->
                val custom = info.loadCustomLabel(ctx)?.toString()?.takeIf { it.isNotBlank() }
                val label = info.loadLabel(ctx)?.toString().orEmpty()
                HdmiInput(info.id, custom ?: label.ifBlank { info.id })
            }
    }.getOrDefault(emptyList())

    /**
     * Switches the TV to an HDMI input. The TV itself then sends the HDMI-CEC "set stream path"
     * message, which wakes a CEC-enabled console (Xbox: Settings > General > TV & display options >
     * Device & connections > HDMI-CEC / "Power on from TV").
     * Apps can't send raw CEC commands without system permission, so this is the supported route.
     */
    fun switchToHdmi(ctx: Context, inputId: String?): Boolean {
        val id = inputId ?: hdmiInputs(ctx).firstOrNull()?.id
        if (id == null) { toast(ctx, "No HDMI input found - pick one in Settings"); return false }
        val uri = TvContract.buildChannelUriForPassthroughInput(id)
        return tryStart(ctx, Intent(Intent.ACTION_VIEW, uri), "Couldn't switch input")
    }

    fun isHomeServiceEnabled(ctx: Context): Boolean {
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { it.startsWith("${ctx.packageName}/") }
    }
}
