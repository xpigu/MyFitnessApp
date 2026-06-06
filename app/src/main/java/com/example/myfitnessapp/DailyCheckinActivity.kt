package com.example.myfitnessapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myfitnessapp.data.viewmodel.DailyCheckinViewModel

class DailyCheckinActivity : AppCompatActivity() {

    private lateinit var viewModel: DailyCheckinViewModel
    private lateinit var historyAdapter: CheckinHistoryAdapter

    private lateinit var ivStatus: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvStreak: TextView
    private lateinit var btnCheckin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_checkin)

        viewModel = ViewModelProvider(this).get(DailyCheckinViewModel::class.java)

        initViews()
        setupActions()
        observeData()
        handleReminderEntry(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderEntry(intent)
    }

    private fun initViews() {
        ivStatus = findViewById(R.id.iv_checkin_status)
        tvStatus = findViewById(R.id.tv_checkin_status)
        tvStreak = findViewById(R.id.tv_streak_count)
        btnCheckin = findViewById(R.id.btn_checkin)

        val rvHistory = findViewById<RecyclerView>(R.id.rv_checkin_history)
        rvHistory.layoutManager = LinearLayoutManager(this)
        historyAdapter = CheckinHistoryAdapter()
        rvHistory.adapter = historyAdapter
    }

    private fun setupActions() {
        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        btnCheckin.setOnClickListener {
            viewModel.checkinToday(
                onSuccess = { streak ->
                    Toast.makeText(this, "签到成功！连续签到 $streak 天", Toast.LENGTH_SHORT).show()
                },
                onAlreadyCheckedIn = {
                    Toast.makeText(this, "今天已经签到过了哦", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun observeData() {
        viewModel.isCheckedInToday.observe(this) { isCheckedIn ->
            if (isCheckedIn) {
                ivStatus.setColorFilter(ContextCompat.getColor(this, R.color.gold_accent))
                tvStatus.text = "今日已签到"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.gold_dark))
                btnCheckin.isEnabled = false
                btnCheckin.text = "已签到"
                btnCheckin.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_400))
            } else {
                ivStatus.setColorFilter(ContextCompat.getColor(this, R.color.gray_400))
                tvStatus.text = "今日未签到"
                tvStatus.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                btnCheckin.isEnabled = true
                btnCheckin.text = "立即签到"
                btnCheckin.setBackgroundColor(ContextCompat.getColor(this, R.color.indigo_primary))
            }
        }

        viewModel.currentStreak.observe(this) { streak ->
            tvStreak.text = "连续签到: $streak 天"
        }

        viewModel.allCheckins.observe(this) { checkins ->
            historyAdapter.submitList(checkins)
        }
    }

    private fun handleReminderEntry(intent: Intent?) {
        if (intent?.getBooleanExtra(ReminderScheduler.EXTRA_FROM_REMINDER, false) != true) return

        if (ReminderType.from(intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TYPE)) == ReminderType.CHECKIN) {
            val scrollView = findViewById<ScrollView>(R.id.scroll_checkin)
            btnCheckin.scrollIntoContainer(scrollView, 16.dp())
            btnCheckin.playReminderFocusAnimation()
            Toast.makeText(this, R.string.reminder_checkin_opened_hint, Toast.LENGTH_SHORT).show()
        }

        intent.removeExtra(ReminderScheduler.EXTRA_FROM_REMINDER)
        intent.removeExtra(ReminderScheduler.EXTRA_REMINDER_TYPE)
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()
}
