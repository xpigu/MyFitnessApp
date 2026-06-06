package com.example.myfitnessapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myfitnessapp.data.WorkoutRecordHelper
import com.example.myfitnessapp.data.entity.WorkoutRecord
import com.example.myfitnessapp.data.viewmodel.PBCardRecord
import com.example.myfitnessapp.data.viewmodel.PBMetricType
import java.util.Locale

/**
 * 最佳成绩卡片适配器
 */
class PBRecordsAdapter(
    private val records: List<PBCardRecord>,
    private val onItemClick: (WorkoutRecord) -> Unit
) : RecyclerView.Adapter<PBRecordsAdapter.PBViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PBViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pb_record, parent, false)
        return PBViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: PBViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    class PBViewHolder(
        itemView: View,
        private val onItemClick: (WorkoutRecord) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val iconView: ImageView = itemView.findViewById(R.id.iv_pb_icon)
        private val titleView: TextView = itemView.findViewById(R.id.tv_pb_title)
        private val valueView: TextView = itemView.findViewById(R.id.tv_pb_value)

        fun bind(record: PBCardRecord) {
            val display = buildDisplay(record)
            iconView.setImageResource(display.iconResId)
            titleView.text = display.title
            valueView.text = display.value
            itemView.setOnClickListener { onItemClick(record.record) }
        }

        private fun buildDisplay(record: PBCardRecord): DisplayCard {
            return when (record.metricType) {
                PBMetricType.RUN_LONGEST_DISTANCE -> DisplayCard(
                    title = "跑步最长距离",
                    value = String.format(Locale.getDefault(), "%.2f km", record.record.totalDistance),
                    iconResId = WorkoutRecordHelper.getIconRes("RUN")
                )
                PBMetricType.CYCLING_LONGEST_DISTANCE -> DisplayCard(
                    title = "骑行最长距离",
                    value = String.format(Locale.getDefault(), "%.2f km", record.record.totalDistance),
                    iconResId = WorkoutRecordHelper.getIconRes("CYCLING")
                )
                PBMetricType.STRENGTH_MAX_WEIGHT -> DisplayCard(
                    title = "力量最大完成重量",
                    value = String.format(Locale.getDefault(), "%.1f kg", record.record.strengthMaxWeight),
                    iconResId = WorkoutRecordHelper.getIconRes("STRENGTH")
                )
                PBMetricType.SWIMMING_LONGEST_DISTANCE -> DisplayCard(
                    title = "游泳最长距离",
                    value = "${record.record.swimDistanceM} m",
                    iconResId = WorkoutRecordHelper.getIconRes("SWIMMING")
                )
                PBMetricType.JUMP_ROPE_MAX_COUNT -> DisplayCard(
                    title = "跳绳最多总个数",
                    value = "${record.record.jumpCount} 个",
                    iconResId = WorkoutRecordHelper.getIconRes("JUMP_ROPE")
                )
                PBMetricType.JUMP_ROPE_MAX_FREQUENCY -> DisplayCard(
                    title = "跳绳最高平均每分钟",
                    value = "${record.record.jumpFrequency} 个/分",
                    iconResId = WorkoutRecordHelper.getIconRes("JUMP_ROPE")
                )
            }
        }
    }

    private data class DisplayCard(
        val title: String,
        val value: String,
        val iconResId: Int
    )
}
