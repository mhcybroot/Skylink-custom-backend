import 'dart:convert';
import '../../core/api_client.dart';

class HistoryRepository {
  final ApiClient apiClient;

  HistoryRepository({required this.apiClient});

  Future<Map<String, dynamic>> fetchHistory() async {
    final response = await apiClient.get('/api/v1/extension/attendance-history');
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to load history');
    }
  }
}
