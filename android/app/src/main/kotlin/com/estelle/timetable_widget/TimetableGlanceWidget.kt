package com.estelle.timetable_widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class TimetableGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            TimetableContent(context)
        }
    }

    @Composable
    private fun TimetableContent(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val widgetPrefs = context.getSharedPreferences(WIDGET_PREFS_NAME, Context.MODE_PRIVATE)

        val scheduleJson = prefs.getString("schedule", null)
        val startDateStr = prefs.getString("startDate", null)
        val lastUpdate = prefs.getString("lastUpdate", null)
        val weekLabel = prefs.getString("weekLabel", "1주차 ")
        val dateRange = prefs.getString("dateRange", "")
        val isCurrentWeek = widgetPrefs.getBoolean("isCurrentWeek", true)

        val todayIndex = if (isCurrentWeek) getCurrentDayIndex() else -1
        val currentPeriod = if (isCurrentWeek) getCurrentPeriod() else -1

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .padding(4.dp)
        ) {
            // 상단 네비게이션 바
            NavigationBar(weekLabel ?: "", dateRange ?: "")

            // 헤더 (날짜 + 요일)
            HeaderRow(startDateStr, todayIndex)

            // 시간표 그리드
            if (scheduleJson != null) {
                TimetableGrid(scheduleJson, todayIndex, currentPeriod)
            } else {
                Box(
                    modifier = GlanceModifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("앱을 열어 데이터를 로드하세요")
                }
            }

            // 하단 업데이트 시간
            Text(
                text = "업데이트: ${lastUpdate ?: "--"}",
                style = TextStyle(
                    fontSize = 8.sp,
                    color = ColorProvider(Color(0xFF888888))
                ),
                modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp),
            )
        }
    }

    @Composable
    private fun NavigationBar(weekLabel: String, dateRange: String) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 이전 주차 버튼
            Box(
                modifier = GlanceModifier
                    .width(40.dp)
                    .height(32.dp)
                    .clickable(actionRunCallback<PrevWeekAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "◀",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = ColorProvider(Color(0xFF4A90D9))
                    )
                )
            }

            // 날짜 표시 (클릭하면 새로고침)
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionRunCallback<RefreshAction>()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$weekLabel$dateRange",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorProvider(Color(0xFF333333))
                    )
                )
            }

            // 다음 주차 버튼
            Box(
                modifier = GlanceModifier
                    .width(40.dp)
                    .height(32.dp)
                    .clickable(actionRunCallback<NextWeekAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "▶",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = ColorProvider(Color(0xFF4A90D9))
                    )
                )
            }
        }
    }

    @Composable
    private fun HeaderRow(startDateStr: String?, todayIndex: Int) {
        val dayNames = listOf("월", "화", "수", "목", "금")
        val dates = getWeekDates(startDateStr)

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color(0xFFF8F9FA))
        ) {
            // 빈 셀 (교시 열)
            Box(
                modifier = GlanceModifier.width(24.dp).padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("")
            }

            // 요일 헤더
            for (i in 0 until 5) {
                val bgColor = if (i == todayIndex) Color(0xFFE3F2FD) else Color.Transparent
                val textColor = if (i == todayIndex) Color(0xFF1976D2) else Color(0xFF888888)

                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .background(bgColor)
                        .padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dates.getOrNull(i) ?: "",
                        style = TextStyle(fontSize = 8.sp, color = ColorProvider(textColor))
                    )
                    Text(
                        text = dayNames[i],
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color(0xFF333333))
                        )
                    )
                }
            }
        }
    }

    @Composable
    private fun TimetableGrid(scheduleJson: String, todayIndex: Int, currentPeriod: Int) {
        val schedule = try { JSONObject(scheduleJson) } catch (e: Exception) { null }
        val dayKeys = listOf("mon", "tue", "wed", "thu", "fri")

        Column(modifier = GlanceModifier.fillMaxWidth()) {
            for (period in 0 until 7) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().height(32.dp)
                ) {
                    // 교시 번호
                    val periodBgColor = if (period == currentPeriod - 1) Color(0xFFFFF9C4) else Color(0xFFF8F9FA)
                    Box(
                        modifier = GlanceModifier
                            .width(24.dp)
                            .fillMaxHeight()
                            .background(periodBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${period + 1}",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(Color(0xFF333333))
                            )
                        )
                    }

                    // 각 요일 셀
                    for (day in 0 until 5) {
                        val dayKey = dayKeys[day]
                        var subject = ""
                        var teacher = ""
                        var isChanged = false

                        if (schedule != null && schedule.has(dayKey)) {
                            val daySchedule = schedule.optJSONArray(dayKey)
                            if (daySchedule != null && period < daySchedule.length()) {
                                val periodData = daySchedule.opt(period)
                                if (periodData is JSONObject) {
                                    subject = periodData.optString("subject", "")
                                    teacher = periodData.optString("teacher", "")
                                    isChanged = periodData.optBoolean("changed", false)
                                }
                            }
                        }

                        val bgColor = when {
                            isChanged -> Color(0xFFFFCDD2)
                            day == todayIndex && period == currentPeriod - 1 -> Color(0xFFC8E6C9)
                            day == todayIndex -> Color(0xFFE3F2FD)
                            period == currentPeriod - 1 -> Color(0xFFFFF9C4)
                            else -> Color.Transparent
                        }
                        val textColor = if (isChanged) Color(0xFFC62828) else Color(0xFF333333)
                        val teacherColor = if (isChanged) Color(0xFFC62828) else Color(0xFF666666)

                        Column(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .background(bgColor)
                                .clickable(actionStartActivity<MainActivity>()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subject,
                                style = TextStyle(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorProvider(textColor)
                                ),
                                maxLines = 1
                            )
                            if (teacher.isNotEmpty()) {
                                Text(
                                    text = teacher,
                                    style = TextStyle(
                                        fontSize = 7.sp,
                                        color = ColorProvider(teacherColor)
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getWeekDates(startDateStr: String?): List<String> {
        if (startDateStr == null) return emptyList()
        return try {
            val parts = startDateStr.split("T")[0].split("-")
            if (parts.size == 3) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, parts[0].toInt())
                    set(Calendar.MONTH, parts[1].toInt() - 1)
                    set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                }
                (0 until 5).map { i ->
                    val dayCal = cal.clone() as Calendar
                    dayCal.add(Calendar.DAY_OF_MONTH, i)
                    "${dayCal.get(Calendar.MONTH) + 1}/${dayCal.get(Calendar.DAY_OF_MONTH)}"
                }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getCurrentDayIndex(): Int {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            else -> -1
        }
    }

    private fun getCurrentPeriod(): Int {
        val cal = Calendar.getInstance()
        val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val ranges = listOf(
            540 to 590, 595 to 645, 650 to 700, 705 to 755,
            810 to 860, 865 to 915, 920 to 970
        )
        for ((i, range) in ranges.withIndex()) {
            if (minutes in range.first..range.second) return i + 1
        }
        return -1
    }

    companion object {
        const val PREFS_NAME = "HomeWidgetPreferences"
        const val WIDGET_PREFS_NAME = "WidgetPreferences"
        const val API_BASE_URL = "http://comci.net:4082"
        const val SCHOOL_CODE = 27224
        const val API_PREFIX = 73629
    }
}

// 새로고침 액션
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        fetchAndUpdateWidget(context, glanceId, 0)
    }
}

// 이전 주차 액션
class PrevWeekAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        fetchAndUpdateWidget(context, glanceId, -1)
    }
}

// 다음 주차 액션
class NextWeekAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: androidx.glance.action.ActionParameters
    ) {
        fetchAndUpdateWidget(context, glanceId, 1)
    }
}

private suspend fun fetchAndUpdateWidget(context: Context, glanceId: GlanceId, delta: Int) {
    val prefs = context.getSharedPreferences(TimetableGlanceWidget.PREFS_NAME, Context.MODE_PRIVATE)
    val widgetPrefs = context.getSharedPreferences(TimetableGlanceWidget.WIDGET_PREFS_NAME, Context.MODE_PRIVATE)

    val weeksJson = prefs.getString("weeksData", null)
    if (weeksJson == null) {
        return
    }

    val weeks = try { JSONArray(weeksJson) } catch (e: Exception) { return }
    if (weeks.length() == 0) return

    var currentIndex = widgetPrefs.getInt("widgetWeekIndex", findCurrentWeekIndex(weeks))
    val newIndex = (currentIndex + delta).coerceIn(0, weeks.length() - 1)

    if (delta != 0 && newIndex == currentIndex) return

    val weekData = weeks.optJSONArray(newIndex) ?: return
    val weekNumber = weekData.optInt(0, 1)
    val weekLabel = weekData.optString(1, "")

    val systemCurrentWeekIndex = findCurrentWeekIndex(weeks)
    val isCurrentWeek = (newIndex == systemCurrentWeekIndex)

    widgetPrefs.edit()
        .putInt("widgetWeekIndex", newIndex)
        .putInt("widgetWeekNumber", weekNumber)
        .putBoolean("isCurrentWeek", isCurrentWeek)
        .apply()

    withContext(Dispatchers.IO) {
        try {
            val params = "${TimetableGlanceWidget.API_PREFIX}_${TimetableGlanceWidget.SCHOOL_CODE}_0_$weekNumber"
            val encoded = android.util.Base64.encodeToString(
                params.toByteArray(Charsets.UTF_8),
                android.util.Base64.NO_WRAP
            )
            val url = URL("${TimetableGlanceWidget.API_BASE_URL}/36179?$encoded")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val jsonStr = response.toString().let {
                    val start = it.indexOf("{")
                    val end = it.lastIndexOf("}")
                    if (start >= 0 && end > start) it.substring(start, end + 1) else it
                }
                val json = JSONObject(jsonStr)

                parseAndSaveTimetable(context, json, newIndex, weekLabel, isCurrentWeek)
            }
            connection.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    TimetableGlanceWidget().update(context, glanceId)
}

private fun findCurrentWeekIndex(weeks: JSONArray): Int {
    val today = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yy-MM-dd", Locale.getDefault())

    for (i in 0 until weeks.length()) {
        try {
            val weekData = weeks.optJSONArray(i) ?: continue
            val label = weekData.optString(1, "")
            val parts = label.split(" ~ ")
            if (parts.size == 2) {
                val startDate = dateFormat.parse(parts[0])
                val endDate = dateFormat.parse(parts[1])
                if (startDate != null && endDate != null) {
                    if (today.time >= startDate && today.time <= endDate) {
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

private fun parseAndSaveTimetable(
    context: Context,
    json: JSONObject,
    weekIndex: Int,
    weekLabel: String,
    isCurrentWeek: Boolean
) {
    val prefs = context.getSharedPreferences(TimetableGlanceWidget.PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit()

    val subjects = mutableListOf<String>()
    val subjectsArray = json.optJSONArray("자료492")
    if (subjectsArray != null) {
        for (i in 0 until subjectsArray.length()) {
            subjects.add(subjectsArray.optString(i, ""))
        }
    }

    val teachers = mutableListOf<String>()
    val teachersArray = json.optJSONArray("자료446")
    if (teachersArray != null) {
        for (i in 0 until teachersArray.length()) {
            teachers.add(teachersArray.optString(i, "").replace("*", ""))
        }
    }

    val schedule481 = json.optJSONArray("자료481")
    val schedule147 = json.optJSONArray("자료147")
    val currentSchedule = parseSchedule(schedule481, subjects, teachers)
    val originalSchedule = parseSchedule(schedule147, subjects, teachers)

    addChangedFlags(currentSchedule, originalSchedule)

    val startDate = json.optString("시작일", "")
    val lastUpdate = json.optString("자료244", "")
    val dateRange = formatDateRange(weekLabel)

    editor.putString("schedule", currentSchedule.toString())
    editor.putString("startDate", startDate)
    editor.putString("lastUpdate", lastUpdate)
    editor.putString("weekLabel", "${weekIndex + 1}주차 ")
    editor.putString("dateRange", dateRange)
    editor.apply()
}

private fun parseSchedule(
    scheduleData: JSONArray?,
    subjects: List<String>,
    teachers: List<String>
): JSONObject {
    val result = JSONObject()
    val dayKeys = listOf("mon", "tue", "wed", "thu", "fri")

    if (scheduleData == null || scheduleData.length() <= 1) return result

    val gradeData = scheduleData.optJSONArray(1) ?: return result
    if (gradeData.length() <= 3) return result
    val classData = gradeData.optJSONArray(3) ?: return result

    for (day in 0 until 5) {
        val dayArray = JSONArray()
        val dayIndex = day + 1

        if (dayIndex < classData.length()) {
            val periods = classData.optJSONArray(dayIndex)
            if (periods != null) {
                for (period in 1..7) {
                    if (period < periods.length()) {
                        val code = periods.optInt(period, 0)
                        val subjectCode = code / 1000
                        val teacherCode = code % 1000

                        val subjectName = subjects.getOrNull(subjectCode) ?: ""
                        val teacherName = teachers.getOrNull(teacherCode) ?: ""

                        dayArray.put(JSONObject().apply {
                            put("subject", subjectName)
                            put("teacher", teacherName)
                            put("code", code)
                        })
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
    return result
}

private fun addChangedFlags(current: JSONObject, original: JSONObject) {
    val dayKeys = listOf("mon", "tue", "wed", "thu", "fri")
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
}

private fun formatDateRange(label: String): String {
    return try {
        val parts = label.split(" ~ ")
        if (parts.size == 2) {
            "${formatShortDate(parts[0])}~${formatShortDate(parts[1])}"
        } else label
    } catch (e: Exception) {
        label
    }
}

private fun formatShortDate(dateStr: String): String {
    val parts = dateStr.split("-")
    return if (parts.size == 3) {
        "${parts[1].toIntOrNull() ?: 0}/${parts[2].toIntOrNull() ?: 0}"
    } else dateStr
}
