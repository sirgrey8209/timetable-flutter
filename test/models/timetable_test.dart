import 'package:flutter_test/flutter_test.dart';
import 'package:timetable_widget/models/timetable.dart';

void main() {
  group('ClassInfo', () {
    test('decodes timetable code correctly', () {
      // 23033 → 과목 23(체육), 교사 33(윤재)
      final classInfo = ClassInfo.fromCode(
        23033,
        ['', '국어', '영어', '수학', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '체육'],
        ['', '김철수', '이영희', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '', '윤재'],
      );

      // 인덱스 23이 subjects에 있어야 함
      expect(classInfo.subjectCode, 23);
      expect(classInfo.teacherCode, 33);
      expect(classInfo.subject, '체육');
      expect(classInfo.teacher, '윤재');
    });

    test('handles zero code as empty', () {
      final classInfo = ClassInfo.fromCode(0, [], []);
      expect(classInfo.subject, '');
      expect(classInfo.teacher, '');
    });

    test('removes asterisk from teacher name', () {
      final classInfo = ClassInfo.fromCode(
        1001,
        ['', '국어'],
        ['', '김철수*'],
      );
      expect(classInfo.teacher, '김철수');
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

    test('getClass returns correct ClassInfo for day and period', () {
      final json = {
        '시작일': '2026-03-09',
        '자료244': '2026-03-10 08:00',
        '자료492': ['', '국어', '영어', '수학'],
        '자료446': ['', '김철수', '이영희', '박민수'],
        '자료481': {
          '1': {
            '3': [
              [],
              [0, 1001, 2002, 3003], // 월요일 (인덱스 1)
              [0, 2001, 1002, 3002], // 화요일
            ]
          }
        },
        '일자자료': [],
      };

      final timetable = TimetableData.fromJson(json);

      // 월요일 1교시: 1001 → 과목1(국어), 교사1(김철수)
      final mondayFirst = timetable.getClass(0, 1);
      expect(mondayFirst?.subject, '국어');
      expect(mondayFirst?.teacher, '김철수');

      // 월요일 2교시: 2002 → 과목2(영어), 교사2(이영희)
      final mondaySecond = timetable.getClass(0, 2);
      expect(mondaySecond?.subject, '영어');
      expect(mondaySecond?.teacher, '이영희');
    });

    test('getCurrentPeriod returns correct period', () {
      // 1교시: 9:00-9:50 (540-590분)
      expect(TimetableData.getCurrentPeriod(DateTime(2026, 3, 9, 9, 30)), 1);

      // 2교시: 9:55-10:45 (595-645분)
      expect(TimetableData.getCurrentPeriod(DateTime(2026, 3, 9, 10, 0)), 2);

      // 수업 시간 아닐 때
      expect(TimetableData.getCurrentPeriod(DateTime(2026, 3, 9, 8, 0)), -1);
      expect(TimetableData.getCurrentPeriod(DateTime(2026, 3, 9, 17, 0)), -1);
    });
  });

  group('WeekInfo', () {
    test('parses from list correctly', () {
      final weekInfo = WeekInfo.fromList([1, '26.03.09 ~ 26.03.13']);
      expect(weekInfo.weekNumber, 1);
      expect(weekInfo.label, '26.03.09 ~ 26.03.13');
    });
  });
}
