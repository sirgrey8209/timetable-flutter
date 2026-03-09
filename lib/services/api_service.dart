import 'dart:convert';
import 'package:http/http.dart' as http;
import '../models/timetable.dart';
import '../utils/constants.dart';

class ApiService {
  /// API URL 생성
  static String buildUrl(int weekNumber) {
    final params = '${ApiConstants.apiPrefix}_${ApiConstants.schoolCode}_0_$weekNumber';
    final encoded = base64Encode(utf8.encode(params));
    return '${ApiConstants.baseUrl}/36179?$encoded';
  }

  /// JSON 응답에서 null 바이트 및 trailing garbage 제거
  static String cleanJsonResponse(String response) {
    final lastBrace = response.lastIndexOf('}');
    if (lastBrace == -1) return response;
    return response.substring(0, lastBrace + 1);
  }

  /// 시간표 데이터 가져오기
  static Future<TimetableData> fetchTimetable(int weekNumber) async {
    final url = buildUrl(weekNumber);

    final response = await http.get(Uri.parse(url));

    if (response.statusCode != 200) {
      throw Exception('Failed to load timetable: ${response.statusCode}');
    }

    final cleanedBody = cleanJsonResponse(response.body);
    final json = jsonDecode(cleanedBody) as Map<String, dynamic>;

    return TimetableData.fromJson(json);
  }

  /// 현재 주차 찾기
  static int findCurrentWeek(List<WeekInfo> weeks) {
    final now = DateTime.now();

    for (final week in weeks) {
      // 라벨: "26.03.09 ~ 26.03.13"
      final parts = week.label.split(' ~ ');
      if (parts.length != 2) continue;

      try {
        final startDate = _parseShortDate(parts[0]);
        final endDate = _parseShortDate(parts[1]);

        if (now.isAfter(startDate.subtract(const Duration(days: 1))) &&
            now.isBefore(endDate.add(const Duration(days: 1)))) {
          return week.weekNumber;
        }
      } catch (_) {
        continue;
      }
    }

    return weeks.isNotEmpty ? weeks.first.weekNumber : 1;
  }

  /// 짧은 날짜 형식 파싱 (26.03.09 -> 2026-03-09)
  static DateTime _parseShortDate(String dateStr) {
    final parts = dateStr.split('.');
    if (parts.length != 3) throw FormatException('Invalid date: $dateStr');

    final year = 2000 + int.parse(parts[0]);
    final month = int.parse(parts[1]);
    final day = int.parse(parts[2]);

    return DateTime(year, month, day);
  }
}
