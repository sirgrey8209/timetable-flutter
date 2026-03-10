import 'dart:convert' show jsonDecode, utf8;
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
  bool _loading = false;
  String? _error;
  bool _offline = false;
  int _selectedWeek = 1;
  List<WeekInfo> _weeks = [];

  // 슬라이드 애니메이션용
  final PageController _pageController = PageController();
  int _currentWeekIndex = 0;
  bool _isAnimating = false;

  // 로딩 상태 표시용
  bool _isRefreshing = false;

  // 마지막 새로고침 시간
  DateTime? _lastRefreshTime;

  @override
  void initState() {
    super.initState();
    _initializeWithCache();
  }

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  /// 캐시 우선 로딩 후 백그라운드에서 새 데이터 fetch
  Future<void> _initializeWithCache() async {
    setState(() {
      _loading = true;
    });

    // 1. 캐시된 데이터 먼저 로드
    final cached = await CacheService.getCachedTimetable();
    final cachedWeek = await CacheService.getCachedWeek();

    if (cached != null) {
      // 현재 주차 확인
      final currentWeek = ApiService.findCurrentWeek(cached.weeks);

      setState(() {
        _timetable = cached;
        _weeks = cached.weeks;
        _selectedWeek = currentWeek; // 현재 주차로 설정
        _currentWeekIndex = _weeks.indexWhere((w) => w.weekNumber == _selectedWeek);
        if (_currentWeekIndex < 0) _currentWeekIndex = 0;
        _loading = false;
        _offline = true; // 일단 캐시 데이터이므로
      });
    }

    // 2. 백그라운드에서 새 데이터 fetch
    _fetchTimetableInBackground();
  }

  /// 백그라운드에서 시간표 로드 (UI 블로킹 없음)
  Future<void> _fetchTimetableInBackground() async {
    try {
      final url = ApiService.buildUrl(_selectedWeek);
      final response = await http.get(Uri.parse(url));

      if (response.statusCode != 200) {
        throw Exception('HTTP ${response.statusCode}');
      }

      // UTF-8로 디코딩
      final body = utf8.decode(response.bodyBytes);
      final cleanedBody = ApiService.cleanJsonResponse(body);
      final json = jsonDecode(cleanedBody) as Map<String, dynamic>;
      final timetable = TimetableData.fromJson(json);

      // 캐시 저장
      await CacheService.saveTimetable(json, _selectedWeek);

      // 위젯 업데이트
      await WidgetService.updateWidget(timetable);

      // UI 업데이트
      if (mounted) {
        setState(() {
          _timetable = timetable;
          _weeks = timetable.weeks;
          _offline = false;
          _loading = false;
          _isRefreshing = false;
          _lastRefreshTime = DateTime.now();
          _currentWeekIndex = _weeks.indexWhere((w) => w.weekNumber == _selectedWeek);
          if (_currentWeekIndex < 0) _currentWeekIndex = 0;
        });
      }
    } catch (e) {
      if (mounted) {
        // 캐시 데이터가 없고 네트워크 오류인 경우에만 에러 표시
        if (_timetable == null) {
          setState(() {
            _error = '시간표를 불러올 수 없습니다: $e';
            _loading = false;
            _isRefreshing = false;
          });
        } else {
          // 캐시 데이터가 있으면 오프라인 모드로 유지
          setState(() {
            _offline = true;
            _isRefreshing = false;
          });
        }
      }
    }
  }

  /// 수동 새로고침 (날짜 영역 탭 시)
  Future<void> _refreshTimetable() async {
    if (_isRefreshing) return;

    setState(() {
      _isRefreshing = true;
    });

    await _fetchTimetableInBackground();
  }

  void _goToPreviousWeek() {
    if (_isAnimating || _currentWeekIndex <= 0) return;

    setState(() {
      _isAnimating = true;
      _currentWeekIndex--;
      _selectedWeek = _weeks[_currentWeekIndex].weekNumber;
    });

    _pageController.previousPage(
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
    ).then((_) {
      _isAnimating = false;
      _fetchTimetableInBackground();
    });
  }

  void _goToNextWeek() {
    if (_isAnimating || _currentWeekIndex >= _weeks.length - 1) return;

    setState(() {
      _isAnimating = true;
      _currentWeekIndex++;
      _selectedWeek = _weeks[_currentWeekIndex].weekNumber;
    });

    _pageController.nextPage(
      duration: const Duration(milliseconds: 300),
      curve: Curves.easeInOut,
    ).then((_) {
      _isAnimating = false;
      _fetchTimetableInBackground();
    });
  }

  String _getWeekDateRange() {
    if (_weeks.isEmpty || _currentWeekIndex >= _weeks.length) {
      return '';
    }
    // "26-03-09 ~ 26-03-14" -> "3/9~3/14"
    final label = _weeks[_currentWeekIndex].label;
    try {
      final parts = label.split(' ~ ');
      if (parts.length == 2) {
        final start = _formatShortDate(parts[0]);
        final end = _formatShortDate(parts[1]);
        return '$start~$end';
      }
    } catch (_) {}
    return label;
  }

  String _formatShortDate(String dateStr) {
    // "26-03-09" -> "3/9"
    final parts = dateStr.split('-');
    if (parts.length == 3) {
      final month = int.parse(parts[1]);
      final day = int.parse(parts[2]);
      return '$month/$day';
    }
    return dateStr;
  }

  String _formatDateTime(DateTime dateTime) {
    final hour = dateTime.hour.toString().padLeft(2, '0');
    final minute = dateTime.minute.toString().padLeft(2, '0');
    final second = dateTime.second.toString().padLeft(2, '0');
    return '${dateTime.year}-${dateTime.month.toString().padLeft(2, '0')}-${dateTime.day.toString().padLeft(2, '0')} $hour:$minute:$second';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text(
          '보평중 1-3 시간표',
          style: TextStyle(fontSize: 12),
        ),
        toolbarHeight: 36,
        backgroundColor: const Color(0xFF4A90D9),
        foregroundColor: Colors.white,
      ),
      body: Column(
        children: [
          // 날짜 네비게이션 바
          _buildDateNavigator(),
          Expanded(
            child: _buildBody(),
          ),
        ],
      ),
    );
  }

  Widget _buildDateNavigator() {
    final canGoPrevious = _currentWeekIndex > 0;
    final canGoNext = _currentWeekIndex < _weeks.length - 1;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.grey.shade200,
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          // 이전 주차 버튼
          IconButton(
            onPressed: canGoPrevious ? _goToPreviousWeek : null,
            icon: Icon(
              Icons.chevron_left,
              size: 32,
              color: canGoPrevious ? const Color(0xFF4A90D9) : Colors.grey.shade300,
            ),
          ),
          // 날짜 표시 (탭하면 새로고침)
          Expanded(
            child: GestureDetector(
              onTap: _refreshTimetable,
              child: Container(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text(
                      '${_currentWeekIndex + 1}주차 ',
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey.shade500,
                      ),
                    ),
                    Text(
                      _getWeekDateRange(),
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF333333),
                      ),
                    ),
                    if (_isRefreshing) ...[
                      const SizedBox(width: 8),
                      const SizedBox(
                        width: 14,
                        height: 14,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Color(0xFF4A90D9),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
          // 다음 주차 버튼
          IconButton(
            onPressed: canGoNext ? _goToNextWeek : null,
            icon: Icon(
              Icons.chevron_right,
              size: 32,
              color: canGoNext ? const Color(0xFF4A90D9) : Colors.grey.shade300,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading && _timetable == null) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null && _timetable == null) {
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
                onPressed: _refreshTimetable,
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

    // PageView로 슬라이드 애니메이션 구현
    return PageView.builder(
      controller: _pageController,
      physics: const NeverScrollableScrollPhysics(), // 버튼으로만 이동
      itemCount: _weeks.length > 0 ? _weeks.length : 1,
      itemBuilder: (context, index) {
        return SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: TimetableTable(
            timetable: _timetable!,
            todayIndex: todayIndex,
            currentPeriod: currentPeriod,
          ),
        );
      },
    );
  }
}
