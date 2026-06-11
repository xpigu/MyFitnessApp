package com.example.myfitnessapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myfitnessapp.data.entity.AchievementBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BadgeProgressUi(
    val current: Int,
    val target: Int,
    val label: String
)

class BadgeAdapter(
    private val onBadgeClick: (AchievementBadge) -> Unit,
    private val progressProvider: (AchievementBadge) -> BadgeProgressUi?
) : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    private var badges: List<AchievementBadge> = emptyList()

    fun submitList(newBadges: List<AchievementBadge>) {
        badges = newBadges
        notifyDataSetChanged()
    }

    fun refreshProgress() {
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(badges[position], onBadgeClick, progressProvider)
    }

    override fun getItemCount(): Int = badges.size

    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_badge_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_badge_name)
        private val tvDesc: TextView = itemView.findViewById(R.id.tv_badge_desc)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_unlock_date)
        private val tvProgress: TextView = itemView.findViewById(R.id.tv_badge_progress)
        private val progressBar: android.widget.ProgressBar = itemView.findViewById(R.id.progress_badge)

        fun bind(
            badge: AchievementBadge,
            onBadgeClick: (AchievementBadge) -> Unit,
            progressProvider: (AchievementBadge) -> BadgeProgressUi?
        ) {
            tvName.text = badge.name
            tvDesc.text = badge.description
            itemView.setOnClickListener { onBadgeClick(badge) }

            val iconDrawable = AppCompatResources.getDrawable(itemView.context, badge.iconResId)?.mutate()
            if (iconDrawable != null) {
                val wrappedDrawable = DrawableCompat.wrap(iconDrawable)
                DrawableCompat.setTint(
                    wrappedDrawable,
                    ContextCompat.getColor(
                        itemView.context,
                        if (badge.isUnlocked) R.color.gold_accent else R.color.achievement_locked_icon
                    )
                )
                ivIcon.setImageDrawable(wrappedDrawable)
            } else {
                ivIcon.setImageDrawable(null)
            }

            if (badge.isUnlocked) {
                // 已解锁状态
                tvName.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                
                tvDate.visibility = View.VISIBLE
                tvProgress.visibility = View.GONE
                progressBar.visibility = View.GONE
                badge.unlockedDate?.let {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    tvDate.text = "已于 ${sdf.format(Date(it))} 解锁"
                }
            } else {
                // 未解锁状态（置灰）
                tvName.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                tvDate.visibility = View.GONE

                val progress = progressProvider(badge)
                if (progress != null && progress.target > 0) {
                    val clampedCurrent = progress.current.coerceIn(0, progress.target)
                    val percent = (clampedCurrent * 100 / progress.target).coerceIn(0, 100)
                    tvProgress.text = "${progress.label} ${clampedCurrent}/${progress.target}"
                    tvProgress.visibility = View.VISIBLE
                    progressBar.progress = percent
                    progressBar.visibility = View.VISIBLE
                } else {
                    tvProgress.visibility = View.GONE
                    progressBar.visibility = View.GONE
                }
            }
        }
    }
}
