package com.example.myfitnessapp

import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView

private const val REMINDER_FOCUS_SCALE = 1.06f

fun View.scrollIntoContainer(container: ViewGroup, topPaddingPx: Int = 0) {
    post {
        val targetY = (distanceToAncestorTop(container) - topPaddingPx).coerceAtLeast(0)
        when (container) {
            is NestedScrollView -> container.smoothScrollTo(0, targetY)
            is ScrollView -> container.smoothScrollTo(0, targetY)
            else -> container.scrollTo(0, targetY)
        }
    }
}

fun View.playReminderFocusAnimation() {
    requestFocus()
    animate()
        .scaleX(REMINDER_FOCUS_SCALE)
        .scaleY(REMINDER_FOCUS_SCALE)
        .setDuration(180)
        .setInterpolator(DecelerateInterpolator())
        .withEndAction {
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(180)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
        .start()
}

private fun View.distanceToAncestorTop(ancestor: ViewGroup): Int {
    var distance = 0
    var current: View? = this
    while (current != null && current !== ancestor) {
        distance += current.top
        current = current.parent as? View
    }
    return distance
}
