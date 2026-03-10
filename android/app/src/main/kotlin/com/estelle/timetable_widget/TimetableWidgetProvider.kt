package com.estelle.timetable_widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

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

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, TimetableWidgetProvider::class.java)
        )

        when (intent.action) {
            ACTION_REFRESH -> {
                fetchTimetableData(context)
            }
            ACTION_PREV_WEEK -> {
                changeWeek(context, -1)
            }
            ACTION_NEXT_WEEK -> {
                changeWeek(context, 1)
            }
            ACTION_OPEN_APP -> {
                // 앱 열기
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
        private const val WIDGET_PREFS_NAME = "WidgetPreferences"

        // 액션 상수
        private const val ACTION_REFRESH = "com.estelle.timetable_widget.REFRESH"
        private const val ACTION_PREV_WEEK = "com.estelle.timetable_widget.PREV_WEEK"
        private const val ACTION_NEXT_WEEK = "com.estelle.timetable_widget.NEXT_WEEK"
        private const val ACTION_OPEN_APP = "com.estelle.timetable_widget.OPEN_APP"

        // API 설정
        private const val API_BASE_URL = "http://comci.net:4082"
        private const val SCHOOL_CODE = 27224
        private const val API_PREFIX = 73629

        // 색상 상수
        private const val COLOR_TODAY = 0xFFE3F2FD.toInt()
        private const val COLOR_CURRENT = 0xFFFFF9C4.toInt()
        private const val COLOR_TODAY_CURRENT = 0xFFC8E6C9.toInt()
        private const val COLOR_CHANGED = 0xFFFFCDD2.toInt()
        private const val COLOR_HEADER = 0xFFF8F9FA.toInt()
        private const val COLOR_TRANSPARENT = Color.TRANSPARENT
        private const val COLOR_TEXT_CHANGED = 0xFFC62828.toInt()
        private const val COLOR_TEXT_NORMAL = 0xFF333333.toInt()
        private const val COLOR_TEXT_TEACHER = 0xFF666666.toInt()
        private const val COLOR_TEXT_TODAY = 0xFF1976D2.toInt()

        // 셀 컨테이너 ID 매핑
        private val cellContainerIds = arrayOf(
            intArrayOf(R.id.cell_1_1, R.id.cell_1_2, R.id.cell_1_3, R.id.cell_1_4, R.id.cell_1_5),
            intArrayOf(R.id.cell_2_1, R.id.cell_2_2, R.id.cell_2_3, R.id.cell_2_4, R.id.cell_2_5),
            intArrayOf(R.id.cell_3_1, R.id.cell_3_2, R.id.cell_3_3, R.id.cell_3_4, R.id.cell_3_5),
            intArrayOf(R.id.cell_4_1, R.id.cell_4_2, R.id.cell_4_3, R.id.cell_4_4, R.id.cell_4_5),
            intArrayOf(R.id.cell_5_1, R.id.cell_5_2, R.id.cell_5_3, R.id.cell_5_4, R.id.cell_5_5),
            intArrayOf(R.id.cell_6_1, R.id.cell_6_2, R.id.cell_6_3, R.id.cell_6_4, R.id.cell_6_5),
            intArrayOf(R.id.cell_7_1, R.id.cell_7_2, R.id.cell_7_3, R.id.cell_7_4, R.id.cell_7_5)
        )

        private val cellSubjectIds = arrayOf(
            intArrayOf(R.id.cell_1_1_subject, R.id.cell_1_2_subject, R.id.cell_1_3_subject, R.id.cell_1_4_subject, R.id.cell_1_5_subject),
            intArrayOf(R.id.cell_2_1_subject, R.id.cell_2_2_subject, R.id.cell_2_3_subject, R.id.cell_2_4_subject, R.id.cell_2_5_subject),
            intArrayOf(R.id.cell_3_1_subject, R.id.cell_3_2_subject, R.id.cell_3_3_subject, R.id.cell_3_4_subject, R.id.cell_3_5_subject),
            intArrayOf(R.id.cell_4_1_subject, R.id.cell_4_2_subject, R.id.cell_4_3_subject, R.id.cell_4_4_subject, R.id.cell_4_5_subject),
            intArrayOf(R.id.cell_5_1_subject, R.id.cell_5_2_subject, R.id.cell_5_3_subject, R.id.cell_5_4_subject, R.id.cell_5_5_subject),
            intArrayOf(R.id.cell_6_1_subject, R.id.cell_6_2_subject, R.id.cell_6_3_subject, R.id.cell_6_4_subject, R.id.cell_6_5_subject),
            intArrayOf(R.id.cell_7_1_subject, R.id.cell_7_2_subject, R.id.cell_7_3_subject, R.id.cell_7_4_subject, R.id.cell_7_5_subject)
        )

        private val cellTeacherIds = arrayOf(
            intArrayOf(R.id.cell_1_1_teacher, R.id.cell_1_2_teacher, R.id.cell_1_3_teacher, R.id.cell_1_4_teacher, R.id.cell_1_5_teacher),
            intArrayOf(R.id.cell_2_1_teacher, R.id.cell_2_2_teacher, R.id.cell_2_3_teacher, R.id.cell_2_4_teacher, R.id.cell_2_5_teacher),
            intArrayOf(R.id.cell_3_1_teacher, R.id.cell_3_2_teacher, R.id.cell_3_3_teacher, R.id.cell_3_4_teacher, R.id.cell_3_5_teacher),
            intArrayOf(R.id.cell_4_1_teacher, R.id.cell_4_2_teacher, R.id.cell_4_3_teacher, R.id.cell_4_4_teacher, R.id.cell_4_5_teacher),
            intArrayOf(R.id.cell_5_1_teacher, R.id.cell_5_2_teacher, R.id.cell_5_3_teacher, R.id.cell_5_4_teacher, R.id.cell_5_5_teacher),
            intArrayOf(R.id.cell_6_1_teacher, R.id.cell_6_2_teacher, R.id.cell_6_3_teacher, R.id.cell_6_4_teacher, R.id.cell_6_5_teacher),
            intArrayOf(R.id.cell_7_1_teacher, R.id.cell_7_2_teacher, R.id.cell_7_3_teacher, R.id.cell_7_4_teacher, R.id.cell_7_5_teacher)
        )

        private val headerContainerIds = intArrayOf(
            R.id.header_mon, R.id.header_tue, R.id.header_wed,
            R.id.header_thu, R.id.header_fri
        )

        private val headerDateIds = intArrayOf(
            R.id.date_mon, R.id.date_tue, R.id.date_wed,
            R.id.date_thu, R.id.date_fri
        )

        private val periodIds = intArrayOf(
            R.id.period_1, R.id.period_2, R.id.period_3, R.id.period_4,
            R.id.period_5, R.id.period_6, R.id.period_7
        )

        private val executor = Executors.newSingleThreadExecutor()
        private val handler = Handler(Looper.getMainLooper())

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val widgetPrefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val views = RemoteViews(context.packageName, R.layout.timetable_widget)

            // SharedPreferences에서 데이터 읽기
            val scheduleJson = prefs.getString("schedule", null)
            val startDateStr = prefs.getString("startDate", null)
            val lastUpdate = prefs.getString("lastUpdate", null)
            val weekLabel = prefs.getString("weekLabel", "1주차 ")
            val dateRange = prefs.getString("dateRange", "")

            // 위젯 전용 주차 인덱스 (없으면 Flutter에서 설정한 값 사용)
            val currentWeekIndex = widgetPrefs.getInt("widgetWeekIndex", -1)
            val totalWeeks = widgetPrefs.getInt("totalWeeks", 0)

            // 현재 요일과 교시 계산
            val todayIndex = getCurrentDayIndex()
            val currentPeriod = getCurrentPeriod()

            // 현재 주차가 이번 주인지 확인 (오늘 하이라이트 표시 여부)
            val isCurrentWeek = widgetPrefs.getBoolean("isCurrentWeek", true)
            val effectiveTodayIndex = if (isCurrentWeek) todayIndex else -1

            // 클릭 이벤트 설정
            setupClickActions(context, views)

            // 상단 네비게이션 업데이트
            views.setTextViewText(R.id.week_label, weekLabel)
            views.setTextViewText(R.id.date_range, dateRange)

            // 헤더 날짜 및 배경색 설정
            setHeaderDates(views, startDateStr, effectiveTodayIndex)

            // 교시 배경색 설정 (현재 교시 강조)
            setPeriodColors(views, if (isCurrentWeek) currentPeriod else -1)

            // 하단 업데이트 시간 설정
            if (lastUpdate != null && lastUpdate.isNotEmpty()) {
                views.setTextViewText(R.id.last_update, "업데이트: $lastUpdate")
            } else {
                views.setTextViewText(R.id.last_update, "업데이트: --")
            }

            // 시간표 데이터가 있으면 그리드 업데이트
            if (scheduleJson != null) {
                try {
                    updateTimetableGrid(views, scheduleJson, effectiveTodayIndex, if (isCurrentWeek) currentPeriod else -1)
                } catch (e: Exception) {
                    e.printStackTrace()
                    clearTimetableGrid(views)
                }
            } else {
                clearTimetableGrid(views)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        // Android 버전에 따른 PendingIntent 플래그
        private fun getPendingIntentFlags(): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        }

        private fun setupClickActions(context: Context, views: RemoteViews) {
            val flags = getPendingIntentFlags()

            // 날짜 영역 클릭 -> 새로고침
            val refreshIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent, flags)
            views.setOnClickPendingIntent(R.id.date_container, refreshPendingIntent)

            // 이전 주차 버튼 클릭
            val prevIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                action = ACTION_PREV_WEEK
            }
            val prevPendingIntent = PendingIntent.getBroadcast(context, 1, prevIntent, flags)
            views.setOnClickPendingIntent(R.id.btn_prev, prevPendingIntent)

            // 다음 주차 버튼 클릭
            val nextIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                action = ACTION_NEXT_WEEK
            }
            val nextPendingIntent = PendingIntent.getBroadcast(context, 2, nextIntent, flags)
            views.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)

            // 시간표 영역 클릭 -> 앱 열기
            val appIntent = Intent(context, TimetableWidgetProvider::class.java).apply {
                action = ACTION_OPEN_APP
            }
            val appPendingIntent = PendingIntent.getBroadcast(context, 3, appIntent, flags)
            // 각 교시 행에 클릭 이벤트 설정
            for (period in 0 until 7) {
                for (day in 0 until 5) {
                    views.setOnClickPendingIntent(cellContainerIds[period][day], appPendingIntent)
                }
            }
        }

        private fun changeWeek(context: Context, delta: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val widgetPrefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)

            // 주차 정보 가져오기
            val weeksJson = prefs.getString("weeksData", null)
            if (weeksJson == null) {
                Toast.makeText(context, "주차 정보가 없습니다", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val weeks = JSONArray(weeksJson)
                val totalWeeks = weeks.length()
                if (totalWeeks == 0) {
                    Toast.makeText(context, "주차 정보가 없습니다", Toast.LENGTH_SHORT).show()
                    return
                }

                // 현재 위젯 주차 인덱스
                var currentIndex = widgetPrefs.getInt("widgetWeekIndex", 0)
                val newIndex = currentIndex + delta

                // 범위 체크
                if (newIndex < 0 || newIndex >= totalWeeks) {
                    return
                }

                // 새 주차 정보 가져오기
                val weekData = weeks.getJSONArray(newIndex)
                val weekNumber = weekData.getInt(0)
                val weekLabel = weekData.getString(1)

                // 현재 주차인지 확인
                val systemCurrentWeek = findCurrentWeek(weeks)
                val isCurrentWeek = (newIndex == findCurrentWeekIndex(weeks))

                // 저장
                widgetPrefs.edit()
                    .putInt("widgetWeekIndex", newIndex)
                    .putInt("widgetWeekNumber", weekNumber)
                    .putBoolean("isCurrentWeek", isCurrentWeek)
                    .apply()

                // API 호출하여 해당 주차 데이터 가져오기
                fetchTimetableDataForWeek(context, weekNumber, newIndex, weekLabel, isCurrentWeek)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "주차 변경 실패", Toast.LENGTH_SHORT).show()
            }
        }

        private fun findCurrentWeek(weeks: JSONArray): Int {
            val today = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yy-MM-dd", Locale.getDefault())

            for (i in 0 until weeks.length()) {
                try {
                    val weekData = weeks.getJSONArray(i)
                    val label = weekData.getString(1) // "26-03-09 ~ 26-03-14"
                    val parts = label.split(" ~ ")
                    if (parts.size == 2) {
                        val startDate = dateFormat.parse(parts[0])
                        val endDate = dateFormat.parse(parts[1])
                        if (startDate != null && endDate != null) {
                            val todayTime = today.time
                            if (todayTime >= startDate && todayTime <= endDate) {
                                return weekData.getInt(0)
                            }
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            return if (weeks.length() > 0) weeks.getJSONArray(0).getInt(0) else 1
        }

        private fun findCurrentWeekIndex(weeks: JSONArray): Int {
            val today = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yy-MM-dd", Locale.getDefault())

            for (i in 0 until weeks.length()) {
                try {
                    val weekData = weeks.getJSONArray(i)
                    val label = weekData.getString(1)
                    val parts = label.split(" ~ ")
                    if (parts.size == 2) {
                        val startDate = dateFormat.parse(parts[0])
                        val endDate = dateFormat.parse(parts[1])
                        if (startDate != null && endDate != null) {
                            val todayTime = today.time
                            if (todayTime >= startDate && todayTime <= endDate) {
                                return i
                            }
                        }
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            return 0
        }

        private fun fetchTimetableData(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val widgetPrefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)

            // 현재 주차 번호 가져오기
            val weekNumber = widgetPrefs.getInt("widgetWeekNumber", 1)
            val weekIndex = widgetPrefs.getInt("widgetWeekIndex", 0)
            val isCurrentWeek = widgetPrefs.getBoolean("isCurrentWeek", true)

            // 주차 레이블 가져오기
            var weekLabel = ""
            val weeksJson = prefs.getString("weeksData", null)
            if (weeksJson != null) {
                try {
                    val weeks = JSONArray(weeksJson)
                    if (weekIndex < weeks.length()) {
                        val weekData = weeks.getJSONArray(weekIndex)
                        weekLabel = weekData.getString(1)
                    }
                } catch (e: Exception) {}
            }

            fetchTimetableDataForWeek(context, weekNumber, weekIndex, weekLabel, isCurrentWeek)
        }

        private fun buildApiUrl(weekNumber: Int): String {
            // Flutter와 동일한 방식으로 URL 생성
            // params = "73629_27224_0_weekNumber"
            val params = "${API_PREFIX}_${SCHOOL_CODE}_0_$weekNumber"
            val encoded = android.util.Base64.encodeToString(
                params.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )
            return "$API_BASE_URL/36179?$encoded"
        }

        private fun fetchTimetableDataForWeek(
            context: Context,
            weekNumber: Int,
            weekIndex: Int,
            weekLabel: String,
            isCurrentWeek: Boolean
        ) {
            executor.execute {
                try {
                    val url = URL(buildApiUrl(weekNumber))
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()

                        // JSON 파싱 및 저장
                        val jsonStr = cleanJsonResponse(response.toString())
                        val json = JSONObject(jsonStr)

                        // 데이터 파싱 및 저장
                        parseAndSaveTimetable(context, json, weekIndex, weekLabel, isCurrentWeek)

                        handler.post {
                            Toast.makeText(context, "업데이트 완료", Toast.LENGTH_SHORT).show()
                            refreshWidget(context)
                        }
                    } else {
                        handler.post {
                            Toast.makeText(context, "서버 오류: $responseCode", Toast.LENGTH_SHORT).show()
                        }
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                    handler.post {
                        Toast.makeText(context, "연결 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun cleanJsonResponse(response: String): String {
            // "자료 = {...}" 형태에서 JSON만 추출
            val startIndex = response.indexOf("{")
            val endIndex = response.lastIndexOf("}")
            return if (startIndex >= 0 && endIndex > startIndex) {
                response.substring(startIndex, endIndex + 1)
            } else {
                response
            }
        }

        private fun parseAndSaveTimetable(
            context: Context,
            json: JSONObject,
            weekIndex: Int,
            weekLabel: String,
            isCurrentWeek: Boolean
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val widgetPrefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            val widgetEditor = widgetPrefs.edit()

            try {
                // 과목 목록
                val subjects = mutableListOf<String>()
                val subjectsArray = json.optJSONArray("자료492")
                if (subjectsArray != null) {
                    for (i in 0 until subjectsArray.length()) {
                        subjects.add(subjectsArray.optString(i, ""))
                    }
                }

                // 선생님 목록
                val teachers = mutableListOf<String>()
                val teachersArray = json.optJSONArray("자료446")
                if (teachersArray != null) {
                    for (i in 0 until teachersArray.length()) {
                        teachers.add(teachersArray.optString(i, "").replace("*", ""))
                    }
                }

                // 현재 시간표 (자료481)
                val schedule481 = json.optJSONArray("자료481")
                val currentSchedule = parseSchedule(schedule481, subjects, teachers)

                // 원래 시간표 (자료147) - 변경 확인용
                val schedule147 = json.optJSONArray("자료147")
                val originalSchedule = parseSchedule(schedule147, subjects, teachers)

                // 변경 여부 추가
                val scheduleWithChanges = addChangedFlags(currentSchedule, originalSchedule)

                // 시작일
                val startDate = json.optString("시작일", "")

                // 마지막 업데이트
                val lastUpdate = json.optString("자료244", "")

                // 주차 정보 저장
                val weeksArray = json.optJSONArray("일자자료")
                if (weeksArray != null) {
                    editor.putString("weeksData", weeksArray.toString())

                    // 현재 주차 인덱스 찾기
                    if (weekIndex < 0) {
                        val currentIdx = findCurrentWeekIndex(weeksArray)
                        widgetEditor.putInt("widgetWeekIndex", currentIdx)
                        widgetEditor.putBoolean("isCurrentWeek", true)
                        if (currentIdx < weeksArray.length()) {
                            val weekData = weeksArray.getJSONArray(currentIdx)
                            widgetEditor.putInt("widgetWeekNumber", weekData.getInt(0))
                        }
                    }
                }

                // 날짜 범위 포맷
                val dateRange = formatDateRange(weekLabel)

                // SharedPreferences에 저장
                editor.putString("schedule", scheduleWithChanges.toString())
                editor.putString("startDate", startDate)
                editor.putString("lastUpdate", lastUpdate)
                editor.putString("weekLabel", "${weekIndex + 1}주차 ")
                editor.putString("dateRange", dateRange)

                widgetEditor.putBoolean("isCurrentWeek", isCurrentWeek)

                editor.apply()
                widgetEditor.apply()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun parseSchedule(
            scheduleData: JSONArray?,
            subjects: List<String>,
            teachers: List<String>
        ): JSONObject {
            val result = JSONObject()
            val dayKeys = arrayOf("mon", "tue", "wed", "thu", "fri")

            if (scheduleData == null || scheduleData.length() <= 1) {
                return result
            }

            try {
                // 1학년 = index 1
                val gradeData = scheduleData.optJSONArray(1) ?: return result

                // 3반 = index 3
                if (gradeData.length() <= 3) return result
                val classData = gradeData.optJSONArray(3) ?: return result

                for (day in 0 until 5) {
                    val dayArray = JSONArray()
                    val dayIndex = day + 1 // classData[0]은 메타

                    if (dayIndex < classData.length()) {
                        val periods = classData.optJSONArray(dayIndex)
                        if (periods != null) {
                            for (period in 1..7) { // period 0은 메타
                                if (period < periods.length()) {
                                    val code = periods.optInt(period, 0)
                                    val subjectCode = code / 1000
                                    val teacherCode = code % 1000

                                    val subjectName = if (subjectCode < subjects.size) subjects[subjectCode] else ""
                                    val teacherName = if (teacherCode < teachers.size) teachers[teacherCode] else ""

                                    val periodObj = JSONObject()
                                    periodObj.put("subject", subjectName)
                                    periodObj.put("teacher", teacherName)
                                    periodObj.put("code", code)
                                    dayArray.put(periodObj)
                                } else {
                                    dayArray.put(JSONObject().apply {
                                        put("subject", "")
                                        put("teacher", "")
                                        put("code", 0)
                                    })
                                }
                            }
                        }
                    }

                    result.put(dayKeys[day], dayArray)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return result
        }

        private fun addChangedFlags(current: JSONObject, original: JSONObject): JSONObject {
            val dayKeys = arrayOf("mon", "tue", "wed", "thu", "fri")

            for (dayKey in dayKeys) {
                val currentDay = current.optJSONArray(dayKey) ?: continue
                val originalDay = original.optJSONArray(dayKey)

                for (period in 0 until currentDay.length()) {
                    val currentPeriod = currentDay.optJSONObject(period) ?: continue
                    val currentCode = currentPeriod.optInt("code", 0)

                    var isChanged = false
                    if (originalDay != null && period < originalDay.length()) {
                        val originalPeriod = originalDay.optJSONObject(period)
                        val originalCode = originalPeriod?.optInt("code", 0) ?: 0
                        isChanged = currentCode != originalCode && currentCode != 0
                    }

                    currentPeriod.put("changed", isChanged)
                }
            }

            return current
        }

        private fun formatDateRange(label: String): String {
            try {
                val parts = label.split(" ~ ")
                if (parts.size == 2) {
                    val start = formatShortDate(parts[0])
                    val end = formatShortDate(parts[1])
                    return "$start~$end"
                }
            } catch (e: Exception) {}
            return label
        }

        private fun formatShortDate(dateStr: String): String {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val month = parts[1].toIntOrNull() ?: return dateStr
                val day = parts[2].toIntOrNull() ?: return dateStr
                return "$month/$day"
            }
            return dateStr
        }

        private fun refreshWidget(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TimetableWidgetProvider::class.java)
            )
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun setHeaderDates(views: RemoteViews, startDateStr: String?, todayIndex: Int) {
            var startDate: Calendar? = null
            if (startDateStr != null && startDateStr.isNotEmpty()) {
                try {
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
                if (startDate != null) {
                    val dayDate = startDate.clone() as Calendar
                    dayDate.add(Calendar.DAY_OF_MONTH, i)
                    val dateText = "${dayDate.get(Calendar.MONTH) + 1}/${dayDate.get(Calendar.DAY_OF_MONTH)}"
                    views.setTextViewText(headerDateIds[i], dateText)
                }

                val bgColor = if (i == todayIndex) COLOR_TODAY else COLOR_TRANSPARENT
                views.setInt(headerContainerIds[i], "setBackgroundColor", bgColor)

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

                    views.setTextViewText(subjectId, subjectName)
                    views.setTextViewText(teacherId, teacherName)

                    val subjectColor = if (isChanged) COLOR_TEXT_CHANGED else COLOR_TEXT_NORMAL
                    val teacherColor = if (isChanged) COLOR_TEXT_CHANGED else COLOR_TEXT_TEACHER
                    views.setTextColor(subjectId, subjectColor)
                    views.setTextColor(teacherId, teacherColor)

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
                else -> -1
            }
        }

        private fun getCurrentPeriod(): Int {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val totalMinutes = hour * 60 + minute

            val periodRanges = arrayOf(
                540 to 590,
                595 to 645,
                650 to 700,
                705 to 755,
                810 to 860,
                865 to 915,
                920 to 970
            )

            for (i in periodRanges.indices) {
                if (totalMinutes in periodRanges[i].first..periodRanges[i].second) {
                    return i + 1
                }
            }
            return -1
        }
    }
}
