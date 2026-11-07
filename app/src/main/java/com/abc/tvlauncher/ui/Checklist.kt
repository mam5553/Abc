package com.abc.tvlauncher.ui

import android.content.Intent
import android.provider.Settings

/** How to open a settings page. Tried in order; first one the TV accepts wins. */
sealed interface Open {
    data class Action(val action: String) : Open {
        fun intent() = Intent(action)
    }
    data class App(val pkg: String) : Open
}

data class ChecklistItem(
    val id: String,
    val title: String,
    val why: String,
    /** Where to tap if the button can't open the page on this TV. */
    val path: String,
    val open: List<Open> = emptyList(),
)

/**
 * TV settings that cut background work. Edit this list to add more - the Optimize screen
 * renders whatever is here. Android only lets apps open some settings pages; the rest are
 * "follow this path" notes.
 */
object Checklist {
    val items: List<ChecklistItem> = listOf(
        ChecklistItem(
            "autoupdate", "Turn off automatic app updates",
            "Updates download and install in the background and make the TV sluggish.",
            "Play Store > your profile > Settings > Auto-update apps > Don't auto-update apps",
            listOf(Open.App("com.android.vending")),
        ),
        ChecklistItem(
            "dev", "Turn on Developer options (needed for the next two)",
            "Unlocks the background and animation limits below.",
            "Settings > System > About > tap \"Android TV OS build\" 7 times",
            listOf(Open.Action(Settings.ACTION_DEVICE_INFO_SETTINGS)),
        ),
        ChecklistItem(
            "bglimit", "Limit background processes",
            "Developer options > Background process limit. \"At most 4 processes\" is a safe step down; \"No background processes\" can break apps.",
            "Settings > System > Developer options > Background process limit",
            listOf(Open.Action(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)),
        ),
        ChecklistItem(
            "anim", "Halve the animation scales",
            "Set Window, Transition and Animator duration scale to 0.5x (or off) - everything feels faster.",
            "Settings > System > Developer options > Window / Transition / Animator duration scale",
            listOf(Open.Action(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)),
        ),
        ChecklistItem(
            "bloat", "Disable bloat apps",
            "Use the Apps tab here: open an app's page and choose Disable or Force stop.",
            "Settings > Apps > See all apps > pick an app > Disable / Force stop",
            listOf(Open.Action(Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS), Open.Action(Settings.ACTION_APPLICATION_SETTINGS)),
        ),
        ChecklistItem(
            "unused", "Uninstall apps you never use",
            "Every installed app can wake up in the background.",
            "Settings > Apps > See all apps",
            listOf(Open.Action(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)),
        ),
        ChecklistItem(
            "storage", "Keep storage under ~85% full",
            "A nearly full TV gets slow and apps crash.",
            "Settings > System > Storage",
            listOf(Open.Action(Settings.ACTION_INTERNAL_STORAGE_SETTINGS), Open.Action(Settings.ACTION_MEMORY_CARD_SETTINGS)),
        ),
        ChecklistItem(
            "diag", "Turn off usage & diagnostics sharing",
            "Stops telemetry uploads.",
            "Settings > Privacy > Usage & diagnostics",
            listOf(Open.Action(Settings.ACTION_PRIVACY_SETTINGS)),
        ),
        ChecklistItem(
            "ads", "Reset / opt out of ad personalization",
            "Less ad-profile syncing.",
            "Settings > Privacy > Ads",
            listOf(Open.Action("com.google.android.gms.settings.ADS_PRIVACY"), Open.Action(Settings.ACTION_PRIVACY_SETTINGS)),
        ),
        ChecklistItem(
            "location", "Turn off location",
            "A TV doesn't need to know where it is.",
            "Settings > Privacy > Location",
            listOf(Open.Action(Settings.ACTION_LOCATION_SOURCE_SETTINGS)),
        ),
        ChecklistItem(
            "dream", "Screensaver: shorten or turn off",
            "The ambient screensaver keeps loading photos and weather.",
            "Settings > System > Ambient mode (or Screen saver)",
            listOf(Open.Action(Settings.ACTION_DREAM_SETTINGS)),
        ),
        ChecklistItem(
            "voice", "Turn off the voice assistant hotword if you don't use it",
            "Always-listening services use memory.",
            "Settings > System > Voice / Assistant",
            listOf(Open.Action(Settings.ACTION_VOICE_INPUT_SETTINGS)),
        ),
        ChecklistItem(
            "home", "Stop the stock home screen's promos and recommendations",
            "Android can't open this page from an app. With this launcher on top you rarely see it, but turning off its content saves data.",
            "Settings > Accounts & Sign-in > your account > (Google TV) Personalization / Apps > turn off promos; or Settings > Home screen > Channels",
        ),
        ChecklistItem(
            "cec", "Keep HDMI control (CEC) on",
            "Not junk - the XBOX tile needs it to wake your Xbox.",
            "Settings > Device Preferences > Inputs > HDMI control (CEC)",
            listOf(Open.Action(Settings.ACTION_SETTINGS)),
        ),
        ChecklistItem(
            "restart", "Restart the TV once a week",
            "Clears leaked memory. Power > Restart.",
            "Settings > System > Restart (or hold Power on the remote)",
        ),
    )
}
