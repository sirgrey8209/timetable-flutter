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
