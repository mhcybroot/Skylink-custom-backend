import 'dart:convert';
import '../../core/api_client.dart';

class DashboardRepository {
  final ApiClient apiClient;

  DashboardRepository({required this.apiClient});

  Future<Map<String, dynamic>> fetchDashboardStatus() async {
    final response = await apiClient.get('/api/v1/extension/dashboard-status');
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    } else {
      throw Exception('Failed to load dashboard status');
    }
  }

  Future<void> startWork() async {
    await apiClient.post('/employee/work-status/start-work');
  }

  Future<void> takeBreak() async {
    await apiClient.post('/employee/work-status/start-break');
  }

  Future<void> resumeWork() async {
    await apiClient.post('/employee/work-status/restart-work');
  }

  Future<void> endWork() async {
    await apiClient.post('/employee/work-status/end-work');
  }
}
