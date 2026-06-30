import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import 'dart:io' show Platform;

class ApiClient {
  static String get baseUrl {
    if (Platform.isAndroid) {
      return 'http://10.0.2.2:8083';
    }
    return 'http://localhost:8083';
  }
  
  Future<Map<String, String>> _getHeaders() async {
    final prefs = await SharedPreferences.getInstance();
    final authHeader = prefs.getString('auth_header');
    
    return {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
      if (authHeader != null) 'Authorization': authHeader,
    };
  }

  Future<http.Response> get(String path) async {
    final headers = await _getHeaders();
    return http.get(Uri.parse('$baseUrl$path'), headers: headers);
  }

  Future<http.Response> post(String path, {Map<String, dynamic>? body}) async {
    final headers = await _getHeaders();
    return http.post(
      Uri.parse('$baseUrl$path'),
      headers: headers,
      body: body != null ? jsonEncode(body) : null,
    );
  }
}
