import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:workmanager/workmanager.dart';
import '../models/timetable.dart';
import 'api_service.dart';
import 'cache_service.dart';
import 'widget_service.dart';

const String backgroundTaskName = 'timetableUpdate';

@pragma('vm:entry-point')
void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    try {
      // 캐시된 주차 가져오기
      final week = await CacheService.getCachedWeek();

      // API 호출
      final url = ApiService.buildUrl(week);
      final response = await http.get(Uri.parse(url));

      if (response.statusCode == 200) {
        final cleanedBody = ApiService.cleanJsonResponse(response.body);
        final json = jsonDecode(cleanedBody) as Map<String, dynamic>;
        final timetable = TimetableData.fromJson(json);

        // 캐시 저장
        await CacheService.saveTimetable(json, week);

        // 위젯 업데이트
        await WidgetService.updateWidget(timetable);
      }

      return true;
    } catch (e) {
      print('Background task failed: $e');
      return false;
    }
  });
}

class BackgroundService {
  /// 백그라운드 작업 초기화
  static Future<void> initialize() async {
    await Workmanager().initialize(
      callbackDispatcher,
      isInDebugMode: false,
    );
  }

  /// 주기적 갱신 등록 (1시간마다)
  static Future<void> registerPeriodicTask() async {
    await Workmanager().registerPeriodicTask(
      'timetable-update-task',
      backgroundTaskName,
      frequency: const Duration(hours: 1),
      constraints: Constraints(
        networkType: NetworkType.connected,
      ),
    );
  }

  /// 즉시 갱신
  static Future<void> runImmediateTask() async {
    await Workmanager().registerOneOffTask(
      'timetable-immediate-update',
      backgroundTaskName,
      constraints: Constraints(
        networkType: NetworkType.connected,
      ),
    );
  }
}
