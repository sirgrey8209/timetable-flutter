// This is a basic Flutter widget test.

import 'package:flutter_test/flutter_test.dart';

import 'package:timetable_widget/main.dart';

void main() {
  testWidgets('TimetableApp smoke test', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const TimetableApp());

    // Verify that our app title is displayed
    expect(find.text('보평중 1-3 시간표'), findsOneWidget);
  });
}
