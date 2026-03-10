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
  final List<List<int>> schedule; // [요일][교시] = 코드
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
    // 시작일 파싱 (null 안전 처리)
    final startDateStr = json['시작일'] as String? ?? '';
    final startDate = startDateStr.isNotEmpty
        ? DateTime.parse(startDateStr)
        : DateTime.now();

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
    // API 구조: 자료481[학년][반][요일] (각 레벨의 인덱스 0은 메타데이터)
    // 예: classData = [5, [월요일데이터], [화요일데이터], ...]
    final scheduleData = json['자료481'];
    List<List<int>> schedule = [];

    if (scheduleData != null && scheduleData is List && scheduleData.length > 1) {
      // 1학년 = index 1
      final gradeData = scheduleData[1];

      if (gradeData != null && gradeData is List && gradeData.length > 3) {
        // 3반 = index 3 (index 0은 반 수)
        final classData = gradeData[3];

        if (classData != null && classData is List && classData.length > 1) {
          // classData[0]은 요일 수(메타), classData[1]부터 실제 요일 데이터
          for (int i = 0; i < classData.length; i++) {
            final day = classData[i];
            if (day is List) {
              // day[0]은 교시 수(메타), day[1]부터 실제 교시 데이터
              final periods = day.map((code) => code is int ? code : 0).toList().cast<int>();
              schedule.add(periods);
            } else {
              schedule.add(<int>[]);
            }
          }
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
      [540, 590],   // 1교시: 9:00-9:50
      [595, 645],   // 2교시: 9:55-10:45
      [650, 700],   // 3교시: 10:50-11:40
      [705, 755],   // 4교시: 11:45-12:35
      [810, 860],   // 5교시: 13:30-14:20
      [865, 915],   // 6교시: 14:25-15:15
      [920, 965],   // 7교시: 15:20-16:05
    ];

    for (int i = 0; i < periodRanges.length; i++) {
      if (minutes >= periodRanges[i][0] && minutes <= periodRanges[i][1]) {
        return i + 1;
      }
    }
    return -1;
  }
}
