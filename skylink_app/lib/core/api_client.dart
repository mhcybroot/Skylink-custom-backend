import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import 'dart:io' show Platform;

import 'package:flutter_dotenv/flutter_dotenv.dart';

class ApiClient {
  static String get baseUrl {
    // API_BASE_URL contains the path like http://10.0.2.2:8083/api/v1
    // For ApiClient which might append /api/v1 manually, we need just the base URL
    // So we'll get it from dotenv and return it.
    // However, looking at the code, it returns 'http://10.0.2.2:8083'
    final fullUrl = dotenv.env['API_BASE_URL'] ?? 'http://10.0.2.2:8083/api/v1';
    return fullUrl.replaceAll('/api/v1', '');
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
