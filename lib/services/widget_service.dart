import 'dart:convert';
import 'dart:io' show Platform;
import 'package:home_widget/home_widget.dart';
import '../models/timetable.dart';
import '../utils/constants.dart';

class WidgetService {
  static const String _appGroupId = 'group.com.estelle.timetable_widget';
  static const String _androidWidgetName = 'TimetableWidgetProvider';

  /// 모바일 플랫폼인지 확인
  static bool get _isMobile => Platform.isAndroid || Platform.isIOS;

  /// 위젯 초기화
  static Future<void> initialize() async {
    if (!_isMobile) return;
    await HomeWidget.setAppGroupId(_appGroupId);
  }

  /// 위젯 데이터 업데이트
  /// weekIndex: 현재 선택된 주차 인덱스 (0-based)
  static Future<void> updateWidget(TimetableData timetable, {int? weekIndex}) async {
    if (!_isMobile) return;
    final now = DateTime.now();
    final todayIndex = TimetableData.getDayIndex(now);
    final currentPeriod = TimetableData.getCurrentPeriod(now);

    // 시간표 데이터를 JSON으로 저장
    final scheduleData = <String, dynamic>{};
    const dayKeys = ['mon', 'tue', 'wed', 'thu', 'fri']; // Kotlin 위젯과 키 일치

    for (int day = 0; day < 5; day++) {
      final dayKey = dayKeys[day];
      final dayClasses = <Map<String, dynamic>>[];

      for (int period = 1; period <= 7; period++) {
        final classInfo = timetable.getClass(day, period);
        final isChanged = timetable.isChanged(day, period);
        dayClasses.add({
          'subject': classInfo?.subject ?? '',
          'teacher': classInfo?.teacher ?? '',
          'changed': isChanged,
        });
      }

      scheduleData[dayKey] = dayClasses;
    }

    // 주차 레이블과 날짜 범위 계산
    String weekLabel = '1주차 ';
    String dateRange = '';
    if (timetable.weeks.isNotEmpty) {
      final idx = weekIndex ?? 0;
      if (idx < timetable.weeks.length) {
        weekLabel = '${idx + 1}주차 ';
        // "26-03-09 ~ 26-03-14" -> "3/9~3/14"
        final label = timetable.weeks[idx].label;
        dateRange = _formatDateRange(label);
      }
    }

    // 위젯 데이터 저장
    await HomeWidget.saveWidgetData('schedule', jsonEncode(scheduleData));
    await HomeWidget.saveWidgetData('todayIndex', todayIndex);
    await HomeWidget.saveWidgetData('currentPeriod', currentPeriod);
    await HomeWidget.saveWidgetData('lastUpdate', timetable.lastUpdate);
    await HomeWidget.saveWidgetData('startDate', timetable.startDate.toIso8601String());
    await HomeWidget.saveWidgetData('weekLabel', weekLabel);
    await HomeWidget.saveWidgetData('dateRange', dateRange);

    // 위젯 갱신 요청
    await HomeWidget.updateWidget(
      name: _androidWidgetName,
      androidName: _androidWidgetName,
    );
  }

  /// 날짜 범위 포맷팅 ("26-03-09 ~ 26-03-14" -> "3/9~3/14")
  static String _formatDateRange(String label) {
    try {
      final parts = label.split(' ~ ');
      if (parts.length == 2) {
        final start = _formatShortDate(parts[0]);
        final end = _formatShortDate(parts[1]);
        return '$start~$end';
      }
    } catch (_) {}
    return label;
  }

  /// 날짜 포맷팅 ("26-03-09" -> "3/9")
  static String _formatShortDate(String dateStr) {
    final parts = dateStr.split('-');
    if (parts.length == 3) {
      final month = int.parse(parts[1]);
      final day = int.parse(parts[2]);
      return '$month/$day';
    }
    return dateStr;
  }

  /// 위젯 클릭 콜백 설정
  static void registerInteractivityCallback(Future<void> Function(Uri?) callback) {
    if (!_isMobile) return;
    HomeWidget.widgetClicked.listen(callback);
  }
}
