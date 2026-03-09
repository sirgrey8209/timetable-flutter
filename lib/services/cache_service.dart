import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/timetable.dart';

class CacheService {
  static const String _cacheKey = 'timetable_cache';
  static const String _weekKey = 'timetable_week';
  static const String _timestampKey = 'timetable_timestamp';
  static const Duration _cacheExpiry = Duration(hours: 1);

  /// 캐시된 시간표 가져오기
  static Future<TimetableData?> getCachedTimetable() async {
    final prefs = await SharedPreferences.getInstance();
    final jsonStr = prefs.getString(_cacheKey);

    if (jsonStr == null) return null;

    try {
      final json = jsonDecode(jsonStr) as Map<String, dynamic>;
      return TimetableData.fromJson(json);
    } catch (_) {
      return null;
    }
  }

  /// 시간표 캐시 저장
  static Future<void> saveTimetable(Map<String, dynamic> json, int week) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_cacheKey, jsonEncode(json));
    await prefs.setInt(_weekKey, week);
    await prefs.setInt(_timestampKey, DateTime.now().millisecondsSinceEpoch);
  }

  /// 캐시된 주차 가져오기
  static Future<int> getCachedWeek() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getInt(_weekKey) ?? 1;
  }

  /// 캐시가 유효한지 확인
  static Future<bool> isCacheValid() async {
    final prefs = await SharedPreferences.getInstance();
    final timestamp = prefs.getInt(_timestampKey);

    if (timestamp == null) return false;

    final cacheTime = DateTime.fromMillisecondsSinceEpoch(timestamp);
    return DateTime.now().difference(cacheTime) < _cacheExpiry;
  }

  /// 캐시 삭제
  static Future<void> clearCache() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_cacheKey);
    await prefs.remove(_weekKey);
    await prefs.remove(_timestampKey);
  }
}
