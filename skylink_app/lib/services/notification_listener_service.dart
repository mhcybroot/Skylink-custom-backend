import 'dart:convert';
import 'package:flutter_notification_listener/flutter_notification_listener.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

@pragma('vm:entry-point')
void _notificationCallback(NotificationEvent evt) async {
  print("Intercepted notification: ${evt.packageName} - ${evt.title}");

  final prefs = await SharedPreferences.getInstance();
  final authHeader = prefs.getString('auth_header');
  
  if (authHeader == null) return; // Not logged in

  try {
    // Send intercepted notification to backend Vault
    final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://10.0.2.2:8083/api/v1';
    await http.post(
      Uri.parse('$baseUrl/extension/phone-notifications'),
      headers: {
        'Authorization': authHeader,
        'Content-Type': 'application/json',
      },
      body: jsonEncode({
        'packageName': evt.packageName ?? 'unknown',
        'title': evt.title ?? '',
        'text': evt.text ?? '',
      }),
    );
  } catch (e) {
    print("Error sending intercepted notification: $e");
  }
}

class PhoneNotificationMonitor {
  static Future<void> initialize() async {
    try {
      bool hasPermission = await NotificationsListener.hasPermission ?? false;
      if (!hasPermission) {
        print("Requesting notification listener permission...");
        await NotificationsListener.openPermissionSettings();
      }

      hasPermission = await NotificationsListener.hasPermission ?? false;
      if (hasPermission) {
        NotificationsListener.initialize(callbackHandle: _notificationCallback);
        print("Notification listener started successfully.");
      } else {
        print("Notification listener permission denied.");
      }
    } catch (e) {
      print("Failed to initialize notification listener: $e");
    }
  }
}
