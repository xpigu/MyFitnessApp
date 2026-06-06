package com.example.myfitnessapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myfitnessapp.data.entity.AchievementBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BadgeAdapter : RecyclerView.Adapter<BadgeAdapter.BadgeViewHolder>() {

    private var badges: List<AchievementBadge> = emptyList()

    fun submitList(newBadges: List<AchievementBadge>) {
        badges = newBadges
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        holder.bind(badges[position])
    }

    override fun getItemCount(): Int = badges.size

    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_badge_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_badge_name)
        private val tvDesc: TextView = itemView.findViewById(R.id.tv_badge_desc)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_unlock_date)

        fun bind(badge: AchievementBadge) {
            tvName.text = badge.name
            tvDesc.text = badge.description
            ivIcon.setImageResource(badge.iconResId)

            if (badge.isUnlocked) {
                // 已解锁状态
                ivIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.gold_accent))
                tvName.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                
                tvDate.visibility = View.VISIBLE
                badge.unlockedDate?.let {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    tvDate.text = "已于 ${sdf.format(Date(it))} 解锁"
                }
            } else {
                // 未解锁状态（置灰）
                ivIcon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.gray_400))
                tvName.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray_600))
                tvDate.visibility = View.GONE
            }
        }
    }
}
