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
