package com.example.immunepercentageapp

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.roundToInt

/** Latest readings shared between the background service and the UI. */
object VitalsStore {
    private const val HR_STALE_MS = 30_000L

    @Volatile private var heartRate: Int? = null
    @Volatile private var heartRateTime = 0L
    @Volatile var steps: Int? = null
    @Volatile var hasHeartSensor = true
    @Volatile var hasStepSensor = true

    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private val main = Handler(Looper.getMainLooper())

    /** Heart rate, or null if there is no recent reading (e.g. watch not worn). */
    fun currentHeartRate(): Int? {
        val hr = heartRate ?: return null
        return if (System.currentTimeMillis() - heartRateTime < HR_STALE_MS) hr else null
    }

    fun updateHeartRate(bpm: Int) {
        heartRate = bpm
        heartRateTime = System.currentTimeMillis()
        notifyChanged()
    }

    fun updateSteps(count: Int) {
        steps = count
        notifyChanged()
    }

    fun addListener(l: () -> Unit) { listeners.add(l) }
    fun removeListener(l: () -> Unit) { listeners.remove(l) }
    fun notifyChanged() { main.post { listeners.forEach { it() } } }
}

/**
 * Simple wellness index (0-100) from heart rate and daily activity.
 * This is an ESTIMATE for fun, not a medical measurement of immunity.
 */
object WellnessScore {
    fun compute(heartRate: Int?, steps: Int?): Int? {
        if (heartRate == null || heartRate <= 0) return null
        val hrScore = when {
            heartRate in 50..75 -> 100.0
            heartRate < 50 -> 100.0 - (50 - heartRate) * 3.0
            else -> 100.0 - (heartRate - 75) * 1.5
        }.coerceIn(0.0, 100.0)
        if (steps == null) return hrScore.roundToInt()
        val activityScore = (steps / 8000.0).coerceAtMost(1.0) * 100.0
        return (hrScore * 0.6 + activityScore * 0.4).roundToInt()
    }
}
