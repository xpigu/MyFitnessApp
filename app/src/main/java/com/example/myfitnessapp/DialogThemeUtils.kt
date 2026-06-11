package com.example.myfitnessapp

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat

fun Context.createAppAlertDialogBuilder(): AlertDialog.Builder {
    return AlertDialog.Builder(this, R.style.ThemeOverlay_MyFitnessApp_AlertDialog)
}

fun AlertDialog.applyAppDialogStyling(context: Context): AlertDialog {
    window?.setBackgroundDrawableResource(R.drawable.card_background)
    listView?.setBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
    getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
        ContextCompat.getColor(context, R.color.health_action_text)
    )
    getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
        ContextCompat.getColor(context, R.color.text_secondary)
    )
    getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(
        ContextCompat.getColor(context, R.color.text_secondary)
    )
    return this
}
