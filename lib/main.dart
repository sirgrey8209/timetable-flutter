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
