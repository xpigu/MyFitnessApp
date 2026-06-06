package com.example.myfitnessapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myfitnessapp.data.entity.DailyCheckin

class CheckinHistoryAdapter : RecyclerView.Adapter<CheckinHistoryAdapter.ViewHolder>() {

    private var checkins: List<DailyCheckin> = emptyList()

    fun submitList(newCheckins: List<DailyCheckin>) {
        checkins = newCheckins
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_checkin_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(checkins[position])
    }

    override fun getItemCount(): Int = checkins.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tv_history_date)
        private val tvStreak: TextView = itemView.findViewById(R.id.tv_history_streak)

        fun bind(checkin: DailyCheckin) {
            tvDate.text = checkin.date
            tvStreak.text = "连续 ${checkin.streakCount} 天"
        }
    }
}
