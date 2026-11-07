package com.abc.tvlauncher.data

/** Pure ordering logic for the "My apps" grid (unit-tested, no Android types). */
object AppOrdering {
    /**
     * @param installed every launchable package, in default (alphabetical) order
     * @param order     the user's saved order (may contain uninstalled packages)
     * Result: pinned apps first, then the rest, each group in saved order.
     * Newly installed apps are appended to the end of their group; hidden apps are dropped.
     */
    fun visible(installed: List<String>, order: List<String>, pinned: Set<String>, hidden: Set<String>): List<String> {
        val installedSet = installed.toHashSet()
        val known = LinkedHashSet<String>()
        order.filterTo(known) { it in installedSet }
        val merged = known.toMutableList().also { list -> installed.filterTo(list) { it !in known } }
        val shown = merged.filter { it !in hidden }
        return shown.filter { it in pinned } + shown.filter { it !in pinned }
    }

    /** Moves [pkg] by [delta] slots, never crossing between the pinned and unpinned groups. */
    fun move(visible: List<String>, pkg: String, delta: Int, pinned: Set<String>): List<String> {
        val from = visible.indexOf(pkg)
        if (from < 0 || delta == 0) return visible
        val pinnedCount = visible.count { it in pinned }
        val range = if (pkg in pinned) 0 until pinnedCount else pinnedCount until visible.size
        val to = (from + delta).coerceIn(range.first, range.last)
        if (to == from) return visible
        return visible.toMutableList().apply { removeAt(from); add(to, pkg) }
    }
}
