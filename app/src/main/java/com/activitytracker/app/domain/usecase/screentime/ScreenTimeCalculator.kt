package com.activitytracker.app.domain.usecase.screentime

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import android.util.Log

class ScreenTimeCalculator(
    private val context: Context
) {
    // Overlay packages: transient UI that temporarily covers the real app.
    // When these fire ACTIVITY_RESUMED, we do NOT replace the foreground —
    // the user is still "using" the app behind the overlay.
    // Examples: notification shade, keyboards, permission dialogs.
    private val overlayPackages = setOf(
        "com.android.systemui",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "com.swiftkey.swiftkey",
        "com.google.android.gms",          // Play Services popups
        "com.android.providers.media",     // Media picker overlay
        
        // System and Core apps that shouldn't count towards conscious screen time
        "com.android.settings",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.android.contacts",
        "com.android.incallui",
        "com.android.phone",
        "com.android.server.telecom"
    )

    // Launcher packages: going to the home screen means the user stopped
    // using apps. These replace the foreground and stop the timer.
    private val launcherPackages = setOf(
        "com.android.launcher",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.oppo.launcher",
        "com.nothing.launcher",
        "com.realme.launcher",
    )

    fun hasUsageAccessPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun getLauncherPackage(): String? {
        return try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(
                intent, PackageManager.MATCH_DEFAULT_ONLY
            )
            resolveInfo?.activityInfo?.packageName
        } catch (_: Exception) {
            null
        }
    }

    /**
     * The Continuous Timeline Algorithm:
     * 1. Query events starting 24h before the target day to build accurate hardware state at exactly midnight.
     * 2. Track screen ON, Keyguard HIDDEN, and a Set of resumed apps.
     * 3. Flush time segments whenever an event occurs, and clamp those segments to the target calendar day.
     * This eliminates overlapping double-counts and guarantees perfect midnight boundaries.
     */
    fun calculateForegroundTimeMinutes(localDate: LocalDate): Int {
        if (!hasUsageAccessPermission()) return 0

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as UsageStatsManager? ?: return 0

        val startOfDay = localDate.atStartOfDayIn(TimeZone.currentSystemDefault())
        val endOfDay = localDate.plus(1, DateTimeUnit.DAY)
            .atStartOfDayIn(TimeZone.currentSystemDefault())

        val targetBeginTime = startOfDay.toEpochMilliseconds()
        val targetEndTime = endOfDay.toEpochMilliseconds()
        val capTime = minOf(System.currentTimeMillis(), targetEndTime)
        
        // Start querying 24 hours BEFORE the target begin time to warm up the state
        val searchStartTime = targetBeginTime - (24L * 60 * 60 * 1000)

        val usageEvents = usageStatsManager.queryEvents(searchStartTime, capTime) ?: return 0
        
        // Create an iterator that isolates the Android framework class
        val eventIterator = iterator {
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                yield(UsageEventData(event.timeStamp, event.eventType, event.packageName))
            }
        }
        
        return calculateForegroundTimeMinutesInternal(targetBeginTime, capTime, searchStartTime, eventIterator)
    }

    // Visible for testing
    internal fun calculateForegroundTimeMinutesInternal(
        targetBeginTime: Long,
        capTime: Long,
        searchStartTime: Long,
        events: Iterator<UsageEventData>
    ): Int {
        val launcherPkg = getLauncherPackage()
        
        // State Machine Flags
        var screenOn = false
        var keyguardShowing = false
        var foregroundPackage: String? = null
        
        var sessionStart = searchStartTime
        var totalMs = 0L

        fun isLauncher(pkg: String): Boolean {
            return pkg == launcherPkg || launcherPackages.contains(pkg)
        }

        fun isOverlay(pkg: String): Boolean {
            return overlayPackages.contains(pkg)
        }

        fun isValidForeground(): Boolean {
            val pkg = foregroundPackage ?: return false
            // If foreground is a launcher or overlay, it's not valid usage
            return !isLauncher(pkg) && !isOverlay(pkg)
        }

        fun flushSession(endTs: Long) {
            val shouldCount = screenOn && !keyguardShowing && isValidForeground()
            
            if (shouldCount) {
                // Clamp the session to [targetBeginTime, capTime]
                val actualStart = maxOf(sessionStart, targetBeginTime)
                val actualEnd = minOf(endTs, capTime)
                
                if (actualEnd > actualStart) {
                    val duration = actualEnd - actualStart
                    // Sanity check for impossible durations
                    if (duration < 1000L * 60 * 60 * 24) {
                        totalMs += duration
                    }
                }
            }
            sessionStart = endTs
        }

        while (events.hasNext()) {
            val event = events.next()
            val ts = event.timeStamp
            
            // If event occurs after our capTime, we stop processing
            if (ts > capTime) break
            
            // Flush the previous state segment up to this event's timestamp
            flushSession(ts)

            val pkg = event.packageName

            when (event.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> screenOn = true
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.DEVICE_SHUTDOWN -> {
                    screenOn = false
                    foregroundPackage = null
                }
                UsageEvents.Event.KEYGUARD_SHOWN -> keyguardShowing = true
                UsageEvents.Event.KEYGUARD_HIDDEN -> keyguardShowing = false
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (pkg != null) {
                        when {
                            // Overlay (keyboard, systemui, etc.): IGNORE.
                            // The user is still using the app behind the overlay.
                            isOverlay(pkg) -> { /* do not change foregroundPackage */ }
                            // Launcher: user went home → stop counting.
                            isLauncher(pkg) -> foregroundPackage = null
                            // Real app: becomes the new foreground.
                            else -> foregroundPackage = pkg
                        }
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    // Only clear if this is the current foreground app being paused.
                    // Ignore pauses from overlay/background apps.
                    if (pkg != null && pkg == foregroundPackage) {
                        foregroundPackage = null
                    }
                }
            }
        }

        // Flush any remaining session up to the capTime
        flushSession(capTime)

        val totalMinutes = (totalMs / 60000L).toInt()
        Log.d("ScreenTimeCalculator", "Calculated $totalMinutes minutes (totalMs: $totalMs)")
        return totalMinutes
    }
}

data class UsageEventData(
    val timeStamp: Long,
    val eventType: Int,
    val packageName: String?
)

