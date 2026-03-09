import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import '../models/timetable.dart';
import '../services/api_service.dart';
import '../services/cache_service.dart';
import '../services/widget_service.dart';
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

      // 위젯 업데이트
      await WidgetService.updateWidget(timetable);

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
