package com.example.myfitnessapp

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

enum class FeedbackType {
    SUCCESS,
    INFO,
    WARNING
}

fun Activity.showAppFeedback(
    message: CharSequence,
    type: FeedbackType = FeedbackType.INFO,
    durationMs: Long = 1200
) {
    val root = findViewById<View>(android.R.id.content) as? ViewGroup ?: return

    val existing = root.findViewWithTag<View>("app_feedback")
    existing?.let { root.removeView(it) }

    val density = resources.displayMetrics.density
    val bottomNav = findViewById<View?>(R.id.bottom_nav)
    val bottomNavOffset = when {
        bottomNav?.visibility == View.VISIBLE && bottomNav.height > 0 -> bottomNav.height + (12 * density).toInt()
        bottomNav?.visibility == View.VISIBLE -> (72 * density).toInt()
        else -> (20 * density).toInt()
    }
    val navigationBarInset = ViewCompat.getRootWindowInsets(root)
        ?.getInsets(WindowInsetsCompat.Type.navigationBars())
        ?.bottom ?: 0

    val container = LinearLayout(this).apply {
        tag = "app_feedback"
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding((16 * density).toInt(), (12 * density).toInt(), (16 * density).toInt(), (12 * density).toInt())
        elevation = 12f

        background = when (type) {
            FeedbackType.SUCCESS -> ContextCompat.getDrawable(context, R.drawable.feedback_success_bg)
            FeedbackType.INFO -> ContextCompat.getDrawable(context, R.drawable.feedback_info_bg)
            FeedbackType.WARNING -> ContextCompat.getDrawable(context, R.drawable.feedback_warning_bg)
        }
    }

    val iconRes = when (type) {
        FeedbackType.SUCCESS -> android.R.drawable.ic_menu_agenda // 先用系统图标替代，后面换成项目内
        FeedbackType.INFO -> android.R.drawable.ic_menu_info_details
        FeedbackType.WARNING -> android.R.drawable.ic_dialog_alert
    }

    val icon = ImageView(this).apply {
        setImageResource(iconRes)
        setColorFilter(ContextCompat.getColor(context, R.color.white))
        layoutParams = LinearLayout.LayoutParams(
            (20 * density).toInt(),
            (20 * density).toInt()
        ).also {
            it.rightMargin = (8 * density).toInt()
        }
    }

    val textView = TextView(this).apply {
        text = message
        setTextColor(ContextCompat.getColor(context, R.color.white))
        textSize = 14f
        maxLines = 3
    }

    container.addView(icon)
    container.addView(textView)

    val layoutParams = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        leftMargin = (16 * density).toInt()
        rightMargin = (16 * density).toInt()
        bottomMargin = (16 * density).toInt() + navigationBarInset + bottomNavOffset
    }

    root.addView(container, layoutParams)

    container.alpha = 0f
    container.translationY = 20 * density
    container.animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(200)
        .withEndAction {
            container.postDelayed({
                container.animate()
                    .alpha(0f)
                    .translationY(20 * density)
                    .setDuration(150)
                    .withEndAction {
                        root.removeView(container)
                    }
                    .start()
            }, durationMs)
        }
        .start()
}
