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
  static Future<void> updateWidget(TimetableData timetable) async {
    if (!_isMobile) return;
    final now = DateTime.now();
    final todayIndex = TimetableData.getDayIndex(now);
    final currentPeriod = TimetableData.getCurrentPeriod(now);

    // 시간표 데이터를 JSON으로 저장
    final scheduleData = <String, dynamic>{};

    for (int day = 0; day < 5; day++) {
      final dayKey = TimetableConstants.dayNames[day];
      final dayClasses = <Map<String, String>>[];

      for (int period = 1; period <= 7; period++) {
        final classInfo = timetable.getClass(day, period);
        dayClasses.add({
          'subject': classInfo?.subject ?? '',
          'teacher': classInfo?.teacher ?? '',
        });
      }

      scheduleData[dayKey] = dayClasses;
    }

    // 위젯 데이터 저장
    await HomeWidget.saveWidgetData('schedule', jsonEncode(scheduleData));
    await HomeWidget.saveWidgetData('todayIndex', todayIndex);
    await HomeWidget.saveWidgetData('currentPeriod', currentPeriod);
    await HomeWidget.saveWidgetData('lastUpdate', timetable.lastUpdate);
    await HomeWidget.saveWidgetData('startDate', timetable.startDate.toIso8601String());

    // 위젯 갱신 요청
    await HomeWidget.updateWidget(
      name: _androidWidgetName,
      androidName: _androidWidgetName,
    );
  }

  /// 위젯 클릭 콜백 설정
  static void registerInteractivityCallback(Future<void> Function(Uri?) callback) {
    if (!_isMobile) return;
    HomeWidget.widgetClicked.listen(callback);
  }
}
