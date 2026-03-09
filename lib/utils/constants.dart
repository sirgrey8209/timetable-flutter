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
