import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../../core/api_client.dart';

class AuthRepository {
  final ApiClient apiClient;

  AuthRepository({required this.apiClient});

  Future<bool> login(String employeeId, String password) async {
    final prefs = await SharedPreferences.getInstance();
    
    // Create Basic Auth string
    String credentials = '$employeeId:$password';
    Codec<String, String> stringToBase64 = utf8.fuse(base64);
    String encoded = stringToBase64.encode(credentials);
    String authHeader = 'Basic $encoded';
    
    // Temporarily save to prefs so API client uses it
    await prefs.setString('auth_header', authHeader);
    
    try {
      print("Attempting login to: ${ApiClient.baseUrl}/api/v1/extension/session-status");
      final response = await apiClient.get('/api/v1/extension/session-status');
      print("Response status: ${response.statusCode}");
      print("Response body: ${response.body}");
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['active'] == true) {
          return true;
        }
      }
    } catch (e) {
      print("Login error: $e");
    }
    
    // If failed, remove the header
    await prefs.remove('auth_header');
    return false;
  }

  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('auth_header');
  }

  Future<bool> isLoggedIn() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.containsKey('auth_header');
  }

  Future<bool> changePassword(String oldPassword, String newPassword) async {
    try {
      final response = await apiClient.post('/api/v1/extension/change-password', body: {
        'oldPassword': oldPassword,
        'newPassword': newPassword,
      });
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success'] == true) {
          final prefs = await SharedPreferences.getInstance();
          final oldHeader = prefs.getString('auth_header');
          if (oldHeader != null && oldHeader.startsWith('Basic ')) {
            // Re-encode with new password
            String decoded = utf8.decode(base64.decode(oldHeader.substring(6)));
            String employeeId = decoded.split(':')[0];
            String credentials = '$employeeId:$newPassword';
            String encoded = base64.encode(utf8.encode(credentials));
            await prefs.setString('auth_header', 'Basic $encoded');
          }
          return true;
        }
      }
      return false;
    } catch (e) {
      print("Change password error: $e");
      return false;
    }
  }
}
