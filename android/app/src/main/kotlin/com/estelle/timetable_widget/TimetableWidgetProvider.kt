package com.estelle.timetable_widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH -> {
                // 새로고침: 앱을 실행하여 데이터 갱신
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
            ACTION_PREV_WEEK, ACTION_NEXT_WEEK -> {
                // 주차 변경: 앱을 실행
                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
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

        // 액션 상수
        private const val ACTION_REFRESH = "com.estelle.timetable_widget.REFRESH"
        private const val ACTION_PREV_WEEK = "com.estelle.timetable_widget.PREV_WEEK"
        private const val ACTION_NEXT_WEEK = "com.estelle.timetable_widget.NEXT_WEEK"

        // 색상 상수
        private const val COLOR_TODAY = 0xFFE3F2FD.toInt()       // 오늘 열 배경색 (파란색 계열)
        private const val COLOR_CURRENT = 0xFFFFF9C4.toInt()     // 현재 교시 배경색 (노란색 계열)
        private const val COLOR_TODAY_CURRENT = 0xFFC8E6C9.toInt() // 오늘 + 현재 교시 (녹색 계열)
        private const val COLOR_CHANGED = 0xFFFFCDD2.toInt()     // 변경된 수업 (연한 빨강)
        private const val COLOR_HEADER = 0xFFF8F9FA.toInt()       // 헤더 배경색
        private const val COLOR_TRANSPARENT = Color.TRANSPARENT
        private const val COLOR_TEXT_CHANGED = 0xFFC62828.toInt() // 변경된 수업 텍스트 (빨강)
        private const val COLOR_TEXT_NORMAL = 0xFF333333.toInt()  // 일반 텍스트 (어두운 회색)
        private const val COLOR_TEXT_TEACHER = 0xFF666666.toInt() // 선생님 텍스트 (회색)
        private const val COLOR_TEXT_TODAY = 0xFF1976D2.toInt()   // 오늘 헤더 텍스트 (파란색)

        // 셀 컨테이너 ID 매핑 (period, day) -> R.id.cell_period_day
        private val cellContainerIds = arrayOf(
            intArrayOf(R.id.cell_1_1, R.id.cell_1_2, R.id.cell_1_3, R.id.cell_1_4, R.id.cell_1_5),
            intArrayOf(R.id.cell_2_1, R.id.cell_2_2, R.id.cell_2_3, R.id.cell_2_4, R.id.cell_2_5),
            intArrayOf(R.id.cell_3_1, R.id.cell_3_2, R.id.cell_3_3, R.id.cell_3_4, R.id.cell_3_5),
            intArrayOf(R.id.cell_4_1, R.id.cell_4_2, R.id.cell_4_3, R.id.cell_4_4, R.id.cell_4_5),
            intArrayOf(R.id.cell_5_1, R.id.cell_5_2, R.id.cell_5_3, R.id.cell_5_4, R.id.cell_5_5),
            intArrayOf(R.id.cell_6_1, R.id.cell_6_2, R.id.cell_6_3, R.id.cell_6_4, R.id.cell_6_5),
            intArrayOf(R.id.cell_7_1, R.id.cell_7_2, R.id.cell_7_3, R.id.cell_7_4, R.id.cell_7_5)
        )

        // 셀 과목 ID 매핑
        private val cellSubjectIds = arrayOf(
            intArrayOf(R.id.cell_1_1_subject, R.id.cell_1_2_subject, R.id.cell_1_3_subject, R.id.cell_1_4_subject, R.id.cell_1_5_subject),
            intArrayOf(R.id.cell_2_1_subject, R.id.cell_2_2_subject, R.id.cell_2_3_subject, R.id.cell_2_4_subject, R.id.cell_2_5_subject),
            intArrayOf(R.id.cell_3_1_subject, R.id.cell_3_2_subject, R.id.cell_3_3_subject, R.id.cell_3_4_subject, R.id.cell_3_5_subject),
            intArrayOf(R.id.cell_4_1_subject, R.id.cell_4_2_subject, R.id.cell_4_3_subject, R.id.cell_4_4_subject, R.id.cell_4_5_subject),
            intArrayOf(R.id.cell_5_1_subject, R.id.cell_5_2_subject, R.id.cell_5_3_subject, R.id.cell_5_4_subject, R.id.cell_5_5_subject),
            intArrayOf(R.id.cell_6_1_subject, R.id.cell_6_2_subject, R.id.cell_6_3_subject, R.id.cell_6_4_subject, R.id.cell_6_5_subject),
            intArrayOf(R.id.cell_7_1_subject, R.id.cell_7_2_subject, R.id.cell_7_3_subject, R.id.cell_7_4_subject, R.id.cell_7_5_subject)
        )

        // 셀 선생님 ID 매핑
        private val cellTeacherIds = arrayOf(
            intArrayOf(R.id.cell_1_1_teacher, R.id.cell_1_2_teacher, R.id.cell_1_3_teacher, R.id.cell_1_4_teacher, R.id.cell_1_5_teacher),
            intArrayOf(R.id.cell_2_1_teacher, R.id.cell_2_2_teacher, R.id.cell_2_3_teacher, R.id.cell_2_4_teacher, R.id.cell_2_5_teacher),
            intArrayOf(R.id.cell_3_1_teacher, R.id.cell_3_2_teacher, R.id.cell_3_3_teacher, R.id.cell_3_4_teacher, R.id.cell_3_5_teacher),
            intArrayOf(R.id.cell_4_1_teacher, R.id.cell_4_2_teacher, R.id.cell_4_3_teacher, R.id.cell_4_4_teacher, R.id.cell_4_5_teacher),
            intArrayOf(R.id.cell_5_1_teacher, R.id.cell_5_2_teacher, R.id.cell_5_3_teacher, R.id.cell_5_4_teacher, R.id.cell_5_5_teacher),
            intArrayOf(R.id.cell_6_1_teacher, R.id.cell_6_2_teacher, R.id.cell_6_3_teacher, R.id.cell_6_4_teacher, R.id.cell_6_5_teacher),
            intArrayOf(R.id.cell_7_1_teacher, R.id.cell_7_2_teacher, R.id.cell_7_3_teacher, R.id.cell_7_4_teacher, R.id.cell_7_5_teacher)
        )

        // 헤더 컨테이너 ID
        private val headerContainerIds = intArrayOf(
            R.id.header_mon, R.id.header_tue, R.id.header_wed,
            R.id.header_thu, R.id.header_fri
        )

        // 헤더 날짜 ID
        private val headerDateIds = intArrayOf(
            R.id.date_mon, R.id.date_tue, R.id.date_wed,
            R.id.date_thu, R.id.date_fri
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
            val startDateStr = prefs.getString("startDate", null)
            val lastUpdate = prefs.getString("lastUpdate", null)
            val weekLabel = prefs.getString("weekLabel", "1주차")
            val dateRange = prefs.getString("dateRange", "")

            // 현재 요일과 교시 계산
            val todayIndex = getCurrentDayIndex()
            val currentPeriod = getCurrentPeriod()

            // 클릭 이벤트 설정
            setupClickActions(context, views)

            // 상단 네비게이션 업데이트
            views.setTextViewText(R.id.week_label, weekLabel)
            views.setTextViewText(R.id.date_range, dateRange)

            // 헤더 날짜 및 배경색 설정
            setHeaderDates(views, startDateStr, todayIndex)

            // 교시 배경색 설정 (현재 교시 강조)
            setPeriodColors(views, currentPeriod)

            // 하단 업데이트 시간 설정
            if (lastUpdate != null) {
                views.setTextViewText(R.id.last_update, "업데이트: $lastUpdate")
            } else {
                views.setTextViewText(R.id.last_update, "업데이트: --")
            }

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

        private fun setupClickActions(context: Context, views: RemoteViews) {
            // 날짜 영역 클릭 -> 새로고침 (앱 실행)
            val refreshIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context, 0, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.date_container, refreshPendingIntent)

            // 이전 주차 버튼 클릭
            val prevIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                action = ACTION_PREV_WEEK
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context, 1, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_prev, prevPendingIntent)

            // 다음 주차 버튼 클릭
            val nextIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                action = ACTION_NEXT_WEEK
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context, 2, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)

            // 전체 위젯 클릭 -> 앱 실행
            val appIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (appIntent != null) {
                val appPendingIntent = PendingIntent.getActivity(
                    context, 3, appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_container, appPendingIntent)
            }
        }

        private fun setHeaderDates(views: RemoteViews, startDateStr: String?, todayIndex: Int) {
            // 시작 날짜 파싱
            var startDate: Calendar? = null
            if (startDateStr != null) {
                try {
                    // ISO 8601 형식: 2025-03-10T00:00:00.000
                    val parts = startDateStr.split("T")[0].split("-")
                    if (parts.size == 3) {
                        startDate = Calendar.getInstance().apply {
                            set(Calendar.YEAR, parts[0].toInt())
                            set(Calendar.MONTH, parts[1].toInt() - 1)
                            set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            for (i in 0 until 5) {
                // 날짜 설정
                if (startDate != null) {
                    val dayDate = startDate.clone() as Calendar
                    dayDate.add(Calendar.DAY_OF_MONTH, i)
                    val dateText = "${dayDate.get(Calendar.MONTH) + 1}/${dayDate.get(Calendar.DAY_OF_MONTH)}"
                    views.setTextViewText(headerDateIds[i], dateText)
                }

                // 오늘 열 하이라이트
                val bgColor = if (i == todayIndex) COLOR_TODAY else COLOR_TRANSPARENT
                views.setInt(headerContainerIds[i], "setBackgroundColor", bgColor)

                // 오늘 날짜 텍스트 색상 변경
                val textColor = if (i == todayIndex) COLOR_TEXT_TODAY else 0xFF888888.toInt()
                views.setTextColor(headerDateIds[i], textColor)
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
                    val containerId = cellContainerIds[period][day]
                    val subjectId = cellSubjectIds[period][day]
                    val teacherId = cellTeacherIds[period][day]
                    val dayKey = dayKeys[day]

                    // 과목명, 선생님, 변경 여부 가져오기
                    var subjectName = ""
                    var teacherName = ""
                    var isChanged = false

                    if (schedule.has(dayKey)) {
                        val daySchedule = schedule.getJSONArray(dayKey)
                        if (period < daySchedule.length()) {
                            val periodData = daySchedule.opt(period)
                            when (periodData) {
                                is JSONObject -> {
                                    subjectName = periodData.optString("subject", "")
                                    teacherName = periodData.optString("teacher", "")
                                    isChanged = periodData.optBoolean("changed", false)
                                }
                                is String -> subjectName = periodData
                            }
                        }
                    }

                    // 과목명 설정
                    views.setTextViewText(subjectId, subjectName)

                    // 선생님 설정
                    views.setTextViewText(teacherId, teacherName)

                    // 텍스트 색상 설정 (변경된 수업은 빨간색)
                    val subjectColor = if (isChanged) COLOR_TEXT_CHANGED else COLOR_TEXT_NORMAL
                    val teacherColor = if (isChanged) COLOR_TEXT_CHANGED else COLOR_TEXT_TEACHER
                    views.setTextColor(subjectId, subjectColor)
                    views.setTextColor(teacherId, teacherColor)

                    // 배경색 설정 (변경된 수업이 우선)
                    val backgroundColor = when {
                        isChanged -> COLOR_CHANGED
                        day == todayIndex && period == currentPeriod - 1 -> COLOR_TODAY_CURRENT
                        day == todayIndex -> COLOR_TODAY
                        period == currentPeriod - 1 -> COLOR_CURRENT
                        else -> COLOR_TRANSPARENT
                    }
                    views.setInt(containerId, "setBackgroundColor", backgroundColor)
                }
            }
        }

        private fun clearTimetableGrid(views: RemoteViews) {
            for (period in 0 until 7) {
                for (day in 0 until 5) {
                    val containerId = cellContainerIds[period][day]
                    val subjectId = cellSubjectIds[period][day]
                    val teacherId = cellTeacherIds[period][day]

                    views.setTextViewText(subjectId, "")
                    views.setTextViewText(teacherId, "")
                    views.setInt(containerId, "setBackgroundColor", COLOR_TRANSPARENT)
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
