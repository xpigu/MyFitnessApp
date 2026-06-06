package com.example.myfitnessapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myfitnessapp.data.viewmodel.AchievementBadgeViewModel

class AchievementsActivity : AppCompatActivity() {

    private lateinit var viewModel: AchievementBadgeViewModel
    private lateinit var badgeAdapter: BadgeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)

        viewModel = ViewModelProvider(this).get(AchievementBadgeViewModel::class.java)

        setupViews()
        observeData()
    }

    private fun setupViews() {
        findViewById<View>(R.id.btn_back).setOnClickListener {
            finish()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rv_badges)
        recyclerView.layoutManager = GridLayoutManager(this, 2) // 2列网格布局
        badgeAdapter = BadgeAdapter()
        recyclerView.adapter = badgeAdapter
    }

    private fun observeData() {
        viewModel.allBadges.observe(this) { badges ->
            badgeAdapter.submitList(badges)
            findViewById<TextView>(R.id.tv_total_count).text = "/ ${badges.size}"
        }

        viewModel.unlockedCount.observe(this) { count ->
            findViewById<TextView>(R.id.tv_unlocked_count).text = count.toString()
        }
    }
}
