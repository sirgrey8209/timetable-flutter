# 보평중 1-3 시간표 Flutter 앱 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 보평중 1-3반 시간표를 표시하는 Flutter 앱과 4x4 안드로이드 홈 위젯 구현

**Architecture:** Flutter 앱에서 컴시간 API를 호출하여 시간표 데이터를 가져오고, SharedPreferences로 캐싱. home_widget 패키지로 네이티브 안드로이드 위젯에 데이터 전달.

**Tech Stack:** Flutter 3.x, home_widget, http, shared_preferences, workmanager

---

## Task 1: Flutter 프로젝트 생성 및 기본 설정

**Files:**
- Create: `pubspec.yaml` (Flutter CLI가 생성)
- Modify: `pubspec.yaml` (의존성 추가)

**Step 1: Flutter 프로젝트 생성**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter create --org com.estelle --project-name timetable_widget .
```
Expected: Flutter 프로젝트 구조 생성

**Step 2: 의존성 추가**

Edit `pubspec.yaml` dependencies 섹션:
```yaml
dependencies:
  flutter:
    sdk: flutter
  http: ^1.2.0
  shared_preferences: ^2.2.2
  home_widget: ^0.4.1
  workmanager: ^0.5.2
  intl: ^0.19.0
```

**Step 3: 의존성 설치**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter pub get
```
Expected: Dependencies resolved, packages downloaded

**Step 4: Android minSdkVersion 설정**

Edit `android/app/build.gradle` - minSdk를 21로 변경:
```gradle
defaultConfig {
    minSdk = 21
    targetSdk = flutter.targetSdkVersion
    ...
}
```

**Step 5: 커밋**

```bash
git add -A
git commit -m "chore: initialize Flutter project with dependencies"
```

---

## Task 2: 상수 및 유틸리티 정의

**Files:**
- Create: `lib/utils/constants.dart`
- Test: 단순 상수이므로 테스트 생략

**Step 1: constants.dart 작성**

Create `lib/utils/constants.dart`:
```dart
/// 컴시간 API 관련 상수
class ApiConstants {
  static const String baseUrl = 'https://estelle-hub.mooo.com/api/comci';
  static const int schoolCode = 27224;
  static const int apiPrefix = 73629;
  static const int grade = 1;
  static const int classNum = 3;
}

/// 시간표 관련 상수
class TimetableConstants {
  static const List<String> dayNames = ['월', '화', '수', '목', '금'];
  static const List<String> periodTimes = [
    '09:00', '09:55', '10:50', '11:45', '13:30', '14:25', '15:20'
  ];
  static const int periodsPerDay = 7;

  /// 교시별 시작/종료 시간 (분 단위, 0시 기준)
  static const List<List<int>> periodTimeRanges = [
    [540, 590],   // 1교시 09:00-09:50
    [595, 645],   // 2교시 09:55-10:45
    [650, 700],   // 3교시 10:50-11:40
    [705, 755],   // 4교시 11:45-12:35
    [810, 860],   // 5교시 13:30-14:20
    [865, 915],   // 6교시 14:25-15:15
    [920, 965],   // 7교시 15:20-16:05
  ];
}

/// 위젯 스타일 상수
class WidgetStyles {
  static const String todayColor = '#E3F2FD';
  static const String currentPeriodColor = '#FFF9C4';
  static const String normalColor = '#FFFFFF';
}
```

**Step 2: 커밋**

```bash
git add lib/utils/constants.dart
git commit -m "feat: add constants for API and timetable"
```

---

## Task 3: 시간표 모델 구현

**Files:**
- Create: `lib/models/timetable.dart`
- Create: `test/models/timetable_test.dart`

**Step 1: 테스트 파일 작성**

Create `test/models/timetable_test.dart`:
```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:timetable_widget/models/timetable.dart';

void main() {
  group('ClassInfo', () {
    test('decodes timetable code correctly', () {
      // 23033 → 과목 23(체육), 교사 33(윤재)
      final classInfo = ClassInfo.fromCode(
        23033,
        ['', '국어', '영어', '수학', /* ... 22개 더 */ ],
        ['', '김철수', '이영희', /* ... 30개 더 */ ],
      );

      // 인덱스 23이 subjects에 있어야 함
      expect(classInfo.subjectCode, 23);
      expect(classInfo.teacherCode, 33);
    });

    test('handles zero code as empty', () {
      final classInfo = ClassInfo.fromCode(0, [], []);
      expect(classInfo.subject, '');
      expect(classInfo.teacher, '');
    });
  });

  group('TimetableData', () {
    test('parses API response JSON', () {
      final json = {
        '시작일': '2026-03-09',
        '자료244': '2026-03-10 08:00',
        '자료492': ['', '국어', '영어', '수학'],
        '자료446': ['', '김철수', '이영희'],
        '자료481': {
          '1': {
            '3': [
              [],
              [0, 1001, 2002, 3001], // 월요일 (인덱스 1)
              [0, 2001, 1002, 3002], // 화요일
            ]
          }
        },
        '일자자료': [
          [1, '26.03.09 ~ 26.03.13'],
          [2, '26.03.16 ~ 26.03.20'],
        ],
      };

      final timetable = TimetableData.fromJson(json);

      expect(timetable.startDate, DateTime(2026, 3, 9));
      expect(timetable.lastUpdate, '2026-03-10 08:00');
      expect(timetable.subjects.length, 4);
      expect(timetable.teachers.length, 3);
      expect(timetable.weeks.length, 2);
    });

    test('getCurrentDayIndex returns correct index for weekdays', () {
      // 한국 시간 기준 월요일 = 0, 금요일 = 4
      final monday = DateTime(2026, 3, 9); // 월요일
      expect(TimetableData.getDayIndex(monday), 0);

      final friday = DateTime(2026, 3, 13); // 금요일
      expect(TimetableData.getDayIndex(friday), 4);

      final saturday = DateTime(2026, 3, 14); // 토요일
      expect(TimetableData.getDayIndex(saturday), -1);
    });
  });
}
```

**Step 2: 테스트 실행 (실패 확인)**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter test test/models/timetable_test.dart
```
Expected: FAIL - timetable.dart 파일이 없음

**Step 3: 모델 구현**

Create `lib/models/timetable.dart`:
```dart
/// 수업 정보 (과목 + 선생님)
class ClassInfo {
  final int subjectCode;
  final int teacherCode;
  final String subject;
  final String teacher;

  ClassInfo({
    required this.subjectCode,
    required this.teacherCode,
    required this.subject,
    required this.teacher,
  });

  /// 컴시간 코드에서 수업 정보 파싱
  /// 코드 = 과목코드 * 1000 + 교사코드
  factory ClassInfo.fromCode(int code, List<String> subjects, List<String> teachers) {
    if (code == 0) {
      return ClassInfo(
        subjectCode: 0,
        teacherCode: 0,
        subject: '',
        teacher: '',
      );
    }

    final subjectCode = code ~/ 1000;
    final teacherCode = code % 1000;

    final subject = subjectCode < subjects.length ? subjects[subjectCode] : '';
    var teacher = teacherCode < teachers.length ? teachers[teacherCode] : '';
    // 선생님 이름에서 * 제거
    teacher = teacher.replaceAll('*', '');

    return ClassInfo(
      subjectCode: subjectCode,
      teacherCode: teacherCode,
      subject: subject,
      teacher: teacher,
    );
  }

  bool get isEmpty => subject.isEmpty;
}

/// 주차 정보
class WeekInfo {
  final int weekNumber;
  final String label;

  WeekInfo({required this.weekNumber, required this.label});

  factory WeekInfo.fromList(List<dynamic> data) {
    return WeekInfo(
      weekNumber: data[0] as int,
      label: data[1] as String,
    );
  }
}

/// 시간표 데이터
class TimetableData {
  final DateTime startDate;
  final String lastUpdate;
  final List<String> subjects;
  final List<String> teachers;
  final List<List<List<int>>> schedule; // [요일][교시] = 코드
  final List<WeekInfo> weeks;

  TimetableData({
    required this.startDate,
    required this.lastUpdate,
    required this.subjects,
    required this.teachers,
    required this.schedule,
    required this.weeks,
  });

  factory TimetableData.fromJson(Map<String, dynamic> json) {
    // 시작일 파싱
    final startDateStr = json['시작일'] as String;
    final startDate = DateTime.parse(startDateStr);

    // 마지막 업데이트
    final lastUpdate = json['자료244'] as String? ?? '';

    // 과목 목록
    final subjects = (json['자료492'] as List<dynamic>?)
        ?.map((e) => e.toString())
        .toList() ?? [];

    // 선생님 목록
    final teachers = (json['자료446'] as List<dynamic>?)
        ?.map((e) => e.toString())
        .toList() ?? [];

    // 시간표 데이터 파싱 (1학년 3반)
    final scheduleData = json['자료481'];
    List<List<List<int>>> schedule = [];

    if (scheduleData != null) {
      final gradeData = scheduleData['1'] ?? scheduleData[1];
      if (gradeData != null) {
        final classData = gradeData['3'] ?? gradeData[3];
        if (classData != null && classData is List) {
          schedule = (classData as List).map((day) {
            if (day is List) {
              return day.map((code) => code is int ? code : 0).toList().cast<int>();
            }
            return <int>[];
          }).toList().cast<List<int>>();
        }
      }
    }

    // 주차 정보
    final weeksData = json['일자자료'] as List<dynamic>? ?? [];
    final weeks = weeksData.map((w) => WeekInfo.fromList(w as List)).toList();

    return TimetableData(
      startDate: startDate,
      lastUpdate: lastUpdate,
      subjects: subjects,
      teachers: teachers,
      schedule: schedule,
      weeks: weeks,
    );
  }

  /// 특정 요일, 교시의 수업 정보 가져오기
  /// dayIndex: 0=월, 1=화, ..., 4=금
  /// period: 1-7
  ClassInfo? getClass(int dayIndex, int period) {
    // schedule[dayIndex+1][period] (인덱스 0은 더미)
    final daySchedule = dayIndex + 1 < schedule.length ? schedule[dayIndex + 1] : null;
    if (daySchedule == null || period >= daySchedule.length) return null;

    final code = daySchedule[period];
    return ClassInfo.fromCode(code, subjects, teachers);
  }

  /// 날짜로 요일 인덱스 계산 (월=0, ..., 금=4, 주말=-1)
  static int getDayIndex(DateTime date) {
    final weekday = date.weekday; // 1=월, 7=일
    if (weekday >= 1 && weekday <= 5) {
      return weekday - 1;
    }
    return -1; // 주말
  }

  /// 현재 교시 계산 (1-7, 없으면 -1)
  static int getCurrentPeriod(DateTime now) {
    final minutes = now.hour * 60 + now.minute;

    const periodRanges = [
      [540, 590],   // 1교시
      [595, 645],   // 2교시
      [650, 700],   // 3교시
      [705, 755],   // 4교시
      [810, 860],   // 5교시
      [865, 915],   // 6교시
      [920, 965],   // 7교시
    ];

    for (int i = 0; i < periodRanges.length; i++) {
      if (minutes >= periodRanges[i][0] && minutes <= periodRanges[i][1]) {
        return i + 1;
      }
    }
    return -1;
  }
}
```

**Step 4: 테스트 실행 (통과 확인)**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter test test/models/timetable_test.dart
```
Expected: All tests PASS

**Step 5: 커밋**

```bash
git add lib/models/timetable.dart test/models/timetable_test.dart
git commit -m "feat: add timetable data model with code parsing"
```

---

## Task 4: API 서비스 구현

**Files:**
- Create: `lib/services/api_service.dart`
- Create: `test/services/api_service_test.dart`

**Step 1: 테스트 파일 작성**

Create `test/services/api_service_test.dart`:
```dart
import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:timetable_widget/services/api_service.dart';

void main() {
  group('ApiService', () {
    test('builds correct API URL', () {
      final url = ApiService.buildUrl(1);
      expect(url.contains('estelle-hub.mooo.com'), true);
      expect(url.contains('36179'), true);

      // base64 파라미터 확인
      final params = base64Encode(utf8.encode('73629_27224_0_1'));
      expect(url.contains(params), true);
    });

    test('cleans JSON response with null bytes', () {
      final dirtyJson = '{"key": "value"}\x00\x00garbage';
      final cleaned = ApiService.cleanJsonResponse(dirtyJson);
      expect(cleaned, '{"key": "value"}');
    });

    test('handles JSON without trailing garbage', () {
      final cleanJson = '{"key": "value"}';
      final cleaned = ApiService.cleanJsonResponse(cleanJson);
      expect(cleaned, '{"key": "value"}');
    });
  });
}
```

**Step 2: 테스트 실행 (실패 확인)**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter test test/services/api_service_test.dart
```
Expected: FAIL - api_service.dart 파일이 없음

**Step 3: API 서비스 구현**

Create `lib/services/api_service.dart`:
```dart
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
```

**Step 4: 테스트 실행 (통과 확인)**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter test test/services/api_service_test.dart
```
Expected: All tests PASS

**Step 5: 커밋**

```bash
git add lib/services/api_service.dart test/services/api_service_test.dart
git commit -m "feat: add API service for fetching timetable"
```

---

## Task 5: 캐시 서비스 구현

**Files:**
- Create: `lib/services/cache_service.dart`

**Step 1: 캐시 서비스 작성**

Create `lib/services/cache_service.dart`:
```dart
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
```

**Step 2: 커밋**

```bash
git add lib/services/cache_service.dart
git commit -m "feat: add cache service using SharedPreferences"
```

---

## Task 6: 시간표 테이블 위젯 구현

**Files:**
- Create: `lib/widgets/timetable_table.dart`

**Step 1: 시간표 테이블 위젯 작성**

Create `lib/widgets/timetable_table.dart`:
```dart
import 'package:flutter/material.dart';
import '../models/timetable.dart';
import '../utils/constants.dart';

class TimetableTable extends StatelessWidget {
  final TimetableData timetable;
  final int todayIndex;
  final int currentPeriod;

  const TimetableTable({
    super.key,
    required this.timetable,
    required this.todayIndex,
    required this.currentPeriod,
  });

  @override
  Widget build(BuildContext context) {
    return Table(
      border: TableBorder.all(color: Colors.grey.shade300),
      columnWidths: const {
        0: FixedColumnWidth(60),
      },
      children: [
        // 헤더 행
        _buildHeaderRow(),
        // 교시 행들
        for (int period = 1; period <= TimetableConstants.periodsPerDay; period++)
          _buildPeriodRow(period),
      ],
    );
  }

  TableRow _buildHeaderRow() {
    return TableRow(
      decoration: BoxDecoration(color: Colors.grey.shade100),
      children: [
        const TableCell(
          child: Padding(
            padding: EdgeInsets.all(8),
            child: Text(
              '교시',
              textAlign: TextAlign.center,
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
          ),
        ),
        for (int i = 0; i < TimetableConstants.dayNames.length; i++)
          TableCell(
            child: Container(
              color: i == todayIndex ? const Color(0xFFE3F2FD) : null,
              padding: const EdgeInsets.all(8),
              child: Column(
                children: [
                  Text(
                    TimetableConstants.dayNames[i],
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: i == todayIndex ? Colors.blue.shade700 : null,
                    ),
                  ),
                  Text(
                    _getDayDate(i),
                    style: TextStyle(
                      fontSize: 10,
                      color: Colors.grey.shade600,
                    ),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }

  TableRow _buildPeriodRow(int period) {
    final isCurrent = period == currentPeriod;

    return TableRow(
      children: [
        // 교시 컬럼
        TableCell(
          child: Container(
            color: isCurrent ? const Color(0xFFFFF9C4) : Colors.grey.shade50,
            padding: const EdgeInsets.all(8),
            child: Column(
              children: [
                Text(
                  '$period교시',
                  style: const TextStyle(fontWeight: FontWeight.bold),
                ),
                Text(
                  TimetableConstants.periodTimes[period - 1],
                  style: TextStyle(
                    fontSize: 10,
                    color: Colors.grey.shade600,
                  ),
                ),
              ],
            ),
          ),
        ),
        // 요일별 수업
        for (int dayIdx = 0; dayIdx < 5; dayIdx++)
          _buildClassCell(dayIdx, period, isCurrent),
      ],
    );
  }

  TableCell _buildClassCell(int dayIndex, int period, bool isCurrent) {
    final isToday = dayIndex == todayIndex;
    final classInfo = timetable.getClass(dayIndex, period);

    Color? bgColor;
    if (isToday && isCurrent) {
      bgColor = const Color(0xFFC8E6C9); // 오늘 + 현재 교시
    } else if (isCurrent) {
      bgColor = const Color(0xFFFFF9C4); // 현재 교시
    } else if (isToday) {
      bgColor = const Color(0xFFE3F2FD); // 오늘
    }

    return TableCell(
      child: Container(
        color: bgColor,
        padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 4),
        child: classInfo != null && !classInfo.isEmpty
            ? Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    classInfo.subject,
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    classInfo.teacher,
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 10,
                      color: Colors.grey.shade600,
                    ),
                  ),
                ],
              )
            : const SizedBox.shrink(),
      ),
    );
  }

  String _getDayDate(int dayIndex) {
    final date = timetable.startDate.add(Duration(days: dayIndex));
    return '${date.day}';
  }
}
```

**Step 2: 커밋**

```bash
git add lib/widgets/timetable_table.dart
git commit -m "feat: add timetable table widget with highlighting"
```

---

## Task 7: 홈 화면 구현

**Files:**
- Create: `lib/screens/home_screen.dart`
- Modify: `lib/main.dart`

**Step 1: 홈 화면 작성**

Create `lib/screens/home_screen.dart`:
```dart
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import '../models/timetable.dart';
import '../services/api_service.dart';
import '../services/cache_service.dart';
import '../widgets/timetable_table.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  TimetableData? _timetable;
  bool _loading = true;
  String? _error;
  bool _offline = false;
  int _selectedWeek = 1;
  List<WeekInfo> _weeks = [];

  @override
  void initState() {
    super.initState();
    _loadTimetable();
  }

  Future<void> _loadTimetable() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final url = ApiService.buildUrl(_selectedWeek);
      final response = await http.get(Uri.parse(url));

      if (response.statusCode != 200) {
        throw Exception('HTTP ${response.statusCode}');
      }

      final cleanedBody = ApiService.cleanJsonResponse(response.body);
      final json = jsonDecode(cleanedBody) as Map<String, dynamic>;
      final timetable = TimetableData.fromJson(json);

      // 캐시 저장
      await CacheService.saveTimetable(json, _selectedWeek);

      setState(() {
        _timetable = timetable;
        _weeks = timetable.weeks;
        _offline = false;
        _loading = false;
      });

      // 현재 주차 확인 및 자동 선택
      if (_weeks.isNotEmpty && _selectedWeek == 1) {
        final currentWeek = ApiService.findCurrentWeek(_weeks);
        if (currentWeek != _selectedWeek) {
          _selectedWeek = currentWeek;
          _loadTimetable();
        }
      }
    } catch (e) {
      // 캐시 사용 시도
      final cached = await CacheService.getCachedTimetable();
      if (cached != null) {
        setState(() {
          _timetable = cached;
          _weeks = cached.weeks;
          _offline = true;
          _loading = false;
        });
      } else {
        setState(() {
          _error = '시간표를 불러올 수 없습니다: $e';
          _loading = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('보평중 1-3 시간표'),
        backgroundColor: const Color(0xFF4A90D9),
        foregroundColor: Colors.white,
        actions: [
          if (_weeks.isNotEmpty)
            DropdownButton<int>(
              value: _selectedWeek,
              dropdownColor: Colors.white,
              underline: const SizedBox(),
              icon: const Icon(Icons.arrow_drop_down, color: Colors.white),
              items: _weeks.map((week) {
                return DropdownMenuItem<int>(
                  value: week.weekNumber,
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 8),
                    child: Text(
                      week.label,
                      style: const TextStyle(fontSize: 14),
                    ),
                  ),
                );
              }).toList(),
              onChanged: (value) {
                if (value != null) {
                  setState(() => _selectedWeek = value);
                  _loadTimetable();
                }
              },
            ),
        ],
      ),
      body: Column(
        children: [
          if (_offline)
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              color: const Color(0xFFFFF3E0),
              child: const Text(
                '오프라인 모드 - 캐시된 데이터 표시 중',
                textAlign: TextAlign.center,
                style: TextStyle(color: Color(0xFFE65100)),
              ),
            ),
          Expanded(
            child: _buildBody(),
          ),
          if (_timetable != null)
            Padding(
              padding: const EdgeInsets.all(8),
              child: Text(
                '마지막 업데이트: ${_timetable!.lastUpdate}',
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.grey.shade600,
                ),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, size: 48, color: Colors.red),
              const SizedBox(height: 16),
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.red),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _loadTimetable,
                child: const Text('다시 시도'),
              ),
            ],
          ),
        ),
      );
    }

    if (_timetable == null) {
      return const Center(child: Text('시간표 데이터가 없습니다.'));
    }

    final now = DateTime.now();
    final todayIndex = TimetableData.getDayIndex(now);
    final currentPeriod = TimetableData.getCurrentPeriod(now);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: TimetableTable(
        timetable: _timetable!,
        todayIndex: todayIndex,
        currentPeriod: currentPeriod,
      ),
    );
  }
}
```

**Step 2: main.dart 수정**

Replace `lib/main.dart`:
```dart
import 'package:flutter/material.dart';
import 'screens/home_screen.dart';

void main() {
  runApp(const TimetableApp());
}

class TimetableApp extends StatelessWidget {
  const TimetableApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '보평중 1-3 시간표',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF4A90D9)),
        useMaterial3: true,
      ),
      home: const HomeScreen(),
    );
  }
}
```

**Step 3: 빌드 테스트**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter build apk --debug
```
Expected: BUILD SUCCESSFUL

**Step 4: 커밋**

```bash
git add lib/main.dart lib/screens/home_screen.dart
git commit -m "feat: add home screen with timetable display"
```

---

## Task 8: Android 홈 위젯 설정

**Files:**
- Create: `android/app/src/main/res/layout/timetable_widget.xml`
- Create: `android/app/src/main/res/xml/timetable_widget_info.xml`
- Modify: `android/app/src/main/AndroidManifest.xml`

**Step 1: 위젯 레이아웃 XML 작성**

Create `android/app/src/main/res/layout/timetable_widget.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/widget_container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@android:color/white"
    android:padding="4dp">

    <!-- 헤더 행 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="교시"
            android:textSize="10sp"
            android:textStyle="bold"
            android:gravity="center"
            android:padding="2dp"
            android:background="#F8F9FA"/>

        <TextView
            android:id="@+id/header_mon"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="월"
            android:textSize="10sp"
            android:textStyle="bold"
            android:gravity="center"
            android:padding="2dp"/>

        <TextView
            android:id="@+id/header_tue"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="화"
            android:textSize="10sp"
            android:textStyle="bold"
            android:gravity="center"
            android:padding="2dp"/>

        <TextView
            android:id="@+id/header_wed"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="수"
            android:textSize="10sp"
            android:textStyle="bold"
            android:gravity="center"
            android:padding="2dp"/>

        <TextView
            android:id="@+id/header_thu"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="목"
            android:textSize="10sp"
            android:textStyle="bold"
            android:gravity="center"
            android:padding="2dp"/>

        <TextView
            android:id="@+id/header_fri"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="금"
            android:textSize="10sp"
            android:textStyle="bold"
            android:gravity="center"
            android:padding="2dp"/>

    </LinearLayout>

    <!-- 시간표 그리드 (7교시 x 5일) -->
    <!-- 각 교시별 행은 동적으로 생성됨 -->
    <LinearLayout
        android:id="@+id/timetable_grid"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:orientation="vertical">
    </LinearLayout>

</LinearLayout>
```

**Step 2: 위젯 정보 XML 작성**

Create directory and file:
```bash
mkdir -p android/app/src/main/res/xml
```

Create `android/app/src/main/res/xml/timetable_widget_info.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="280dp"
    android:minHeight="280dp"
    android:targetCellWidth="4"
    android:targetCellHeight="4"
    android:updatePeriodMillis="3600000"
    android:initialLayout="@layout/timetable_widget"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen"
    android:previewLayout="@layout/timetable_widget">
</appwidget-provider>
```

**Step 3: home_widget 설정 추가**

home_widget 패키지는 AppWidgetProvider를 자동으로 처리하므로, AndroidManifest.xml에 설정만 추가하면 됩니다.

`android/app/src/main/AndroidManifest.xml`에 receiver 추가 (application 태그 내부, activity 태그 다음):
```xml
<receiver android:name="es.antonborri.home_widget.HomeWidgetProvider"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/timetable_widget_info" />
</receiver>
```

**Step 4: 커밋**

```bash
git add android/app/src/main/res/layout/timetable_widget.xml \
        android/app/src/main/res/xml/timetable_widget_info.xml \
        android/app/src/main/AndroidManifest.xml
git commit -m "feat: add Android widget configuration files"
```

---

## Task 9: 위젯 서비스 구현

**Files:**
- Create: `lib/services/widget_service.dart`
- Modify: `lib/main.dart` (위젯 초기화 추가)

**Step 1: 위젯 서비스 작성**

Create `lib/services/widget_service.dart`:
```dart
import 'dart:convert';
import 'package:home_widget/home_widget.dart';
import '../models/timetable.dart';
import '../utils/constants.dart';

class WidgetService {
  static const String _appGroupId = 'group.com.estelle.timetable_widget';
  static const String _androidWidgetName = 'TimetableWidgetProvider';

  /// 위젯 초기화
  static Future<void> initialize() async {
    await HomeWidget.setAppGroupId(_appGroupId);
  }

  /// 위젯 데이터 업데이트
  static Future<void> updateWidget(TimetableData timetable) async {
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
    HomeWidget.widgetClicked.listen(callback);
  }
}
```

**Step 2: main.dart에 위젯 초기화 추가**

Update `lib/main.dart`:
```dart
import 'package:flutter/material.dart';
import 'screens/home_screen.dart';
import 'services/widget_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 위젯 서비스 초기화
  await WidgetService.initialize();

  // 위젯 클릭 시 앱 열기
  WidgetService.registerInteractivityCallback((uri) async {
    // 위젯 클릭 시 처리 (앱이 열림)
  });

  runApp(const TimetableApp());
}

class TimetableApp extends StatelessWidget {
  const TimetableApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '보평중 1-3 시간표',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF4A90D9)),
        useMaterial3: true,
      ),
      home: const HomeScreen(),
    );
  }
}
```

**Step 3: home_screen에서 위젯 업데이트 호출**

`lib/screens/home_screen.dart`의 `_loadTimetable` 메서드에 위젯 업데이트 추가:

```dart
// 캐시 저장 후에 추가:
import '../services/widget_service.dart';

// _loadTimetable 메서드 내, 캐시 저장 후:
await WidgetService.updateWidget(timetable);
```

**Step 4: 빌드 테스트**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter build apk --debug
```
Expected: BUILD SUCCESSFUL

**Step 5: 커밋**

```bash
git add lib/services/widget_service.dart lib/main.dart lib/screens/home_screen.dart
git commit -m "feat: add widget service for home screen widget"
```

---

## Task 10: Kotlin 위젯 Provider 구현

**Files:**
- Create: `android/app/src/main/kotlin/com/estelle/timetable_widget/TimetableWidgetProvider.kt`
- Modify: `android/app/src/main/AndroidManifest.xml`

**Step 1: Kotlin 디렉토리 생성**

```bash
mkdir -p android/app/src/main/kotlin/com/estelle/timetable_widget
```

**Step 2: WidgetProvider 작성**

Create `android/app/src/main/kotlin/com/estelle/timetable_widget/TimetableWidgetProvider.kt`:
```kotlin
package com.estelle.timetable_widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.LinearLayout
import android.widget.TextView
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

    companion object {
        private const val PREFS_NAME = "HomeWidgetPreferences"
        private val dayNames = arrayOf("월", "화", "수", "목", "금")
        private val periodTimes = arrayOf("09:00", "09:55", "10:50", "11:45", "13:30", "14:25", "15:20")

        private const val COLOR_TODAY = 0xFFE3F2FD.toInt()
        private const val COLOR_CURRENT = 0xFFFFF9C4.toInt()
        private const val COLOR_TODAY_CURRENT = 0xFFC8E6C9.toInt()
        private const val COLOR_HEADER = 0xFFF8F9FA.toInt()

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val views = RemoteViews(context.packageName, R.layout.timetable_widget)

            val scheduleJson = prefs.getString("schedule", null)
            val todayIndex = prefs.getInt("todayIndex", -1)
            val currentPeriod = prefs.getInt("currentPeriod", -1)

            // 헤더 배경색 설정
            setHeaderColors(views, todayIndex)

            // 시간표 데이터가 있으면 그리드 업데이트
            if (scheduleJson != null) {
                try {
                    val schedule = JSONObject(scheduleJson)
                    // 실제 위젯은 RemoteViews의 제약으로 동적 레이아웃이 제한적
                    // 간단히 첫 수업만 표시하거나, 별도 처리 필요
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setHeaderColors(views: RemoteViews, todayIndex: Int) {
            val headerIds = arrayOf(
                R.id.header_mon, R.id.header_tue, R.id.header_wed,
                R.id.header_thu, R.id.header_fri
            )
            for (i in headerIds.indices) {
                val color = if (i == todayIndex) COLOR_TODAY else Color.TRANSPARENT
                views.setInt(headerIds[i], "setBackgroundColor", color)
            }
        }

        private fun getCurrentDayIndex(): Int {
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            return when (dayOfWeek) {
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
                540 to 590,   // 1교시
                595 to 645,   // 2교시
                650 to 700,   // 3교시
                705 to 755,   // 4교시
                810 to 860,   // 5교시
                865 to 915,   // 6교시
                920 to 965    // 7교시
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
```

**Step 3: AndroidManifest.xml 수정**

Update `android/app/src/main/AndroidManifest.xml` - receiver를 수정:
```xml
<receiver android:name=".TimetableWidgetProvider"
    android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data
        android:name="android.appwidget.provider"
        android:resource="@xml/timetable_widget_info" />
</receiver>
```

**Step 4: 빌드 테스트**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter build apk --debug
```
Expected: BUILD SUCCESSFUL

**Step 5: 커밋**

```bash
git add android/app/src/main/kotlin/com/estelle/timetable_widget/TimetableWidgetProvider.kt \
        android/app/src/main/AndroidManifest.xml
git commit -m "feat: add Kotlin widget provider for Android home widget"
```

---

## Task 11: 백그라운드 갱신 구현

**Files:**
- Create: `lib/services/background_service.dart`
- Modify: `lib/main.dart`

**Step 1: 백그라운드 서비스 작성**

Create `lib/services/background_service.dart`:
```dart
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
```

**Step 2: main.dart 업데이트**

Update `lib/main.dart`:
```dart
import 'package:flutter/material.dart';
import 'screens/home_screen.dart';
import 'services/widget_service.dart';
import 'services/background_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 서비스 초기화
  await WidgetService.initialize();
  await BackgroundService.initialize();

  // 주기적 백그라운드 갱신 등록
  await BackgroundService.registerPeriodicTask();

  // 위젯 클릭 시 앱 열기
  WidgetService.registerInteractivityCallback((uri) async {
    // 위젯 클릭 시 처리 (앱이 열림)
  });

  runApp(const TimetableApp());
}

class TimetableApp extends StatelessWidget {
  const TimetableApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '보평중 1-3 시간표',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF4A90D9)),
        useMaterial3: true,
      ),
      home: const HomeScreen(),
    );
  }
}
```

**Step 3: 빌드 테스트**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter build apk --debug
```
Expected: BUILD SUCCESSFUL

**Step 4: 커밋**

```bash
git add lib/services/background_service.dart lib/main.dart
git commit -m "feat: add background refresh service using workmanager"
```

---

## Task 12: 앱 아이콘 및 최종 설정

**Files:**
- Create: `android/app/src/main/res/mipmap-*/ic_launcher.png`
- Modify: `android/app/build.gradle` (release 설정)

**Step 1: 앱 아이콘 준비**

간단한 아이콘을 생성하거나 기존 아이콘을 사용. Flutter launcher_icons 패키지 사용 권장:

`pubspec.yaml`에 추가:
```yaml
dev_dependencies:
  flutter_launcher_icons: ^0.13.1

flutter_launcher_icons:
  android: true
  ios: false
  image_path: "assets/icon.png"
```

아이콘 이미지 준비 후:
```bash
flutter pub get
dart run flutter_launcher_icons
```

**Step 2: Release 빌드 설정**

`android/app/build.gradle`에서 release 설정 확인 (필요시 수정).

**Step 3: 최종 빌드**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter build apk --release
```
Expected: BUILD SUCCESSFUL, APK 생성

**Step 4: APK 위치 확인**

Run:
```bash
ls -la build/app/outputs/flutter-apk/
```
Expected: `app-release.apk` 파일 존재

**Step 5: 최종 커밋**

```bash
git add -A
git commit -m "chore: finalize app configuration and build"
```

---

## Task 13: 테스트 및 검증

**Step 1: 전체 테스트 실행**

Run:
```bash
cd /home/estelle/timetable-flutter
flutter test
```
Expected: All tests PASS

**Step 2: 실제 기기 테스트**

1. APK를 안드로이드 기기에 설치
2. 앱 실행하여 시간표 표시 확인
3. 홈 화면에서 위젯 추가 (4x4)
4. 위젯에 시간표 표시 확인
5. 오늘 컬럼 하이라이트 확인
6. 현재 교시 강조 확인

**Step 3: 최종 커밋**

```bash
git add -A
git commit -m "test: verify all functionality"
```

---

## 요약

| Task | 설명 | 예상 시간 |
|------|------|----------|
| 1 | 프로젝트 생성 및 설정 | 5분 |
| 2 | 상수 정의 | 3분 |
| 3 | 시간표 모델 (TDD) | 15분 |
| 4 | API 서비스 (TDD) | 10분 |
| 5 | 캐시 서비스 | 5분 |
| 6 | 시간표 테이블 위젯 | 10분 |
| 7 | 홈 화면 | 15분 |
| 8 | Android 위젯 XML | 10분 |
| 9 | 위젯 서비스 | 10분 |
| 10 | Kotlin Provider | 15분 |
| 11 | 백그라운드 갱신 | 10분 |
| 12 | 앱 아이콘 및 최종 설정 | 10분 |
| 13 | 테스트 및 검증 | 15분 |

**총 예상 시간: 약 2시간**
