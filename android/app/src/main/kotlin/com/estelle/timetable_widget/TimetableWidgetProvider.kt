package com.estelle.timetable_widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class TimetableWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // 첫 번째 위젯이 생성될 때 호출
    }

    override fun onDisabled(context: Context) {
        // 마지막 위젯이 제거될 때 호출
    }

    companion object {
        private const val PREFS_NAME = "HomeWidgetPreferences"

        // 색상 상수
        private const val COLOR_TODAY = 0xFFE3F2FD.toInt()       // 오늘 열 배경색 (파란색 계열)
        private const val COLOR_CURRENT = 0xFFFFF9C4.toInt()     // 현재 교시 배경색 (노란색 계열)
        private const val COLOR_TODAY_CURRENT = 0xFFC8E6C9.toInt() // 오늘 + 현재 교시 (녹색 계열)
        private const val COLOR_CHANGED = 0xFFFFCDD2.toInt()     // 변경된 수업 (연한 빨강)
        private const val COLOR_HEADER = 0xFFF8F9FA.toInt()       // 헤더 배경색
        private const val COLOR_TRANSPARENT = Color.TRANSPARENT
        private const val COLOR_TEXT_CHANGED = 0xFFC62828.toInt() // 변경된 수업 텍스트 (빨강)
        private const val COLOR_TEXT_NORMAL = 0xFF333333.toInt()  // 일반 텍스트 (어두운 회색)

        // 셀 ID 매핑 (period, day) -> R.id.cell_period_day
        private val cellIds = arrayOf(
            intArrayOf(R.id.cell_1_1, R.id.cell_1_2, R.id.cell_1_3, R.id.cell_1_4, R.id.cell_1_5),
            intArrayOf(R.id.cell_2_1, R.id.cell_2_2, R.id.cell_2_3, R.id.cell_2_4, R.id.cell_2_5),
            intArrayOf(R.id.cell_3_1, R.id.cell_3_2, R.id.cell_3_3, R.id.cell_3_4, R.id.cell_3_5),
            intArrayOf(R.id.cell_4_1, R.id.cell_4_2, R.id.cell_4_3, R.id.cell_4_4, R.id.cell_4_5),
            intArrayOf(R.id.cell_5_1, R.id.cell_5_2, R.id.cell_5_3, R.id.cell_5_4, R.id.cell_5_5),
            intArrayOf(R.id.cell_6_1, R.id.cell_6_2, R.id.cell_6_3, R.id.cell_6_4, R.id.cell_6_5),
            intArrayOf(R.id.cell_7_1, R.id.cell_7_2, R.id.cell_7_3, R.id.cell_7_4, R.id.cell_7_5)
        )

        // 헤더 ID
        private val headerIds = intArrayOf(
            R.id.header_mon, R.id.header_tue, R.id.header_wed,
            R.id.header_thu, R.id.header_fri
        )

        // 교시 ID
        private val periodIds = intArrayOf(
            R.id.period_1, R.id.period_2, R.id.period_3, R.id.period_4,
            R.id.period_5, R.id.period_6, R.id.period_7
        )

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val views = RemoteViews(context.packageName, R.layout.timetable_widget)

            // SharedPreferences에서 데이터 읽기
            val scheduleJson = prefs.getString("schedule", null)

            // 현재 요일과 교시 계산
            val todayIndex = getCurrentDayIndex()
            val currentPeriod = getCurrentPeriod()

            // 헤더 배경색 설정 (오늘 열 강조)
            setHeaderColors(views, todayIndex)

            // 교시 배경색 설정 (현재 교시 강조)
            setPeriodColors(views, currentPeriod)

            // 시간표 데이터가 있으면 그리드 업데이트
            if (scheduleJson != null) {
                try {
                    updateTimetableGrid(views, scheduleJson, todayIndex, currentPeriod)
                } catch (e: Exception) {
                    e.printStackTrace()
                    clearTimetableGrid(views)
                }
            } else {
                clearTimetableGrid(views)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setHeaderColors(views: RemoteViews, todayIndex: Int) {
            for (i in headerIds.indices) {
                val color = if (i == todayIndex) COLOR_TODAY else COLOR_TRANSPARENT
                views.setInt(headerIds[i], "setBackgroundColor", color)
            }
        }

        private fun setPeriodColors(views: RemoteViews, currentPeriod: Int) {
            for (i in periodIds.indices) {
                val color = if (i == currentPeriod - 1) COLOR_CURRENT else COLOR_HEADER
                views.setInt(periodIds[i], "setBackgroundColor", color)
            }
        }

        private fun updateTimetableGrid(
            views: RemoteViews,
            scheduleJson: String,
            todayIndex: Int,
            currentPeriod: Int
        ) {
            val schedule = JSONObject(scheduleJson)
            val dayKeys = arrayOf("mon", "tue", "wed", "thu", "fri")

            for (period in 0 until 7) {
                for (day in 0 until 5) {
                    val cellId = cellIds[period][day]
                    val dayKey = dayKeys[day]

                    // 과목명과 변경 여부 가져오기
                    var subjectName = ""
                    var isChanged = false
                    if (schedule.has(dayKey)) {
                        val daySchedule = schedule.getJSONArray(dayKey)
                        if (period < daySchedule.length()) {
                            val periodData = daySchedule.opt(period)
                            when (periodData) {
                                is JSONObject -> {
                                    subjectName = periodData.optString("subject", "")
                                    isChanged = periodData.optBoolean("changed", false)
                                }
                                is String -> subjectName = periodData
                            }
                        }
                    }

                    // 텍스트 설정
                    views.setTextViewText(cellId, subjectName)

                    // 텍스트 색상 설정 (변경된 수업은 빨간색)
                    val textColor = if (isChanged) COLOR_TEXT_CHANGED else COLOR_TEXT_NORMAL
                    views.setTextColor(cellId, textColor)

                    // 배경색 설정 (변경된 수업이 우선)
                    val backgroundColor = when {
                        isChanged -> COLOR_CHANGED
                        day == todayIndex && period == currentPeriod - 1 -> COLOR_TODAY_CURRENT
                        day == todayIndex -> COLOR_TODAY
                        period == currentPeriod - 1 -> COLOR_CURRENT
                        else -> COLOR_TRANSPARENT
                    }
                    views.setInt(cellId, "setBackgroundColor", backgroundColor)
                }
            }
        }

        private fun clearTimetableGrid(views: RemoteViews) {
            for (period in 0 until 7) {
                for (day in 0 until 5) {
                    val cellId = cellIds[period][day]
                    views.setTextViewText(cellId, "")
                    views.setInt(cellId, "setBackgroundColor", COLOR_TRANSPARENT)
                }
            }
        }

        private fun getCurrentDayIndex(): Int {
            val calendar = Calendar.getInstance()
            return when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                else -> -1  // 주말
            }
        }

        private fun getCurrentPeriod(): Int {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val totalMinutes = hour * 60 + minute

            // 교시별 시간 범위 (시작~종료)
            val periodRanges = arrayOf(
                540 to 590,   // 1교시: 09:00~09:50
                595 to 645,   // 2교시: 09:55~10:45
                650 to 700,   // 3교시: 10:50~11:40
                705 to 755,   // 4교시: 11:45~12:35
                810 to 860,   // 5교시: 13:30~14:20
                865 to 915,   // 6교시: 14:25~15:15
                920 to 970    // 7교시: 15:20~16:10
            )

            for (i in periodRanges.indices) {
                if (totalMinutes in periodRanges[i].first..periodRanges[i].second) {
                    return i + 1  // 1-indexed
                }
            }
            return -1  // 수업 시간이 아님
        }
    }
}
