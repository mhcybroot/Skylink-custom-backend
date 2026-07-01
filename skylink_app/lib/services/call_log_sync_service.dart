import 'dart:convert';
import 'package:call_log/call_log.dart';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:workmanager/workmanager.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_dotenv/flutter_dotenv.dart';

@pragma('vm:entry-point')
void callLogCallbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    print("Background Call Log Sync Task Started: $task");
    await CallLogSyncService.syncCallLogsDirectly();
    return Future.value(true);
  });
}

class CallLogSyncService {
  static const String syncTask = "syncCallLogsTask";

  static Future<void> initialize() async {
    Workmanager().initialize(
      callLogCallbackDispatcher,
      isInDebugMode: kDebugMode,
    );

    Workmanager().registerPeriodicTask(
      "call_log_sync_1",
      syncTask,
      frequency: const Duration(minutes: 15),
      constraints: Constraints(
        networkType: NetworkType.connected,
      ),
    );
    print("WorkManager call log sync registered.");
  }
  
  static Future<void> triggerSyncNow() async {
    Workmanager().registerOneOffTask(
      "call_log_sync_oneoff",
      syncTask,
      constraints: Constraints(
        networkType: NetworkType.connected,
      ),
    );
  }

  static Future<void> syncCallLogsDirectly() async {
    print("Starting direct call log sync...");
    try {
      final prefs = await SharedPreferences.getInstance();
      final authHeader = prefs.getString('auth_header');
      if (authHeader == null) {
        print("Not logged in. Stopping sync.");
        return;
      }

      // Read call logs
      Iterable<CallLogEntry> entries = await CallLog.get();
      if (entries.isEmpty) {
        print("No call logs found.");
        return;
      }

      int lastSyncedTimestamp = prefs.getInt('last_synced_call_timestamp') ?? 0;
      List<CallLogEntry> newLogs = [];
      int latestTimestamp = lastSyncedTimestamp;

      for (var entry in entries) {
        int ts = entry.timestamp ?? 0;
        if (ts > lastSyncedTimestamp) {
          newLogs.add(entry);
          if (ts > latestTimestamp) {
            latestTimestamp = ts;
          }
        }
      }

      if (newLogs.isEmpty) {
        print("No new call logs to sync.");
        return;
      }

      List<Map<String, dynamic>> payload = newLogs.map((entry) {
        String callTypeStr = 'UNKNOWN';
        if (entry.callType == CallType.incoming) callTypeStr = 'INCOMING';
        if (entry.callType == CallType.outgoing) callTypeStr = 'OUTGOING';
        if (entry.callType == CallType.missed) callTypeStr = 'MISSED';
        if (entry.callType == CallType.rejected) callTypeStr = 'REJECTED';

        DateTime dt = DateTime.fromMillisecondsSinceEpoch(entry.timestamp ?? 0);
        
        return {
          'callerName': entry.name,
          'callNumber': entry.number,
          'callType': callTypeStr,
          'durationSeconds': entry.duration,
          'callTimestamp': dt.toIso8601String(),
        };
      }).toList();

      final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://10.0.2.2:8083/api/v1';
      var response = await http.post(
        Uri.parse('$baseUrl/extension/call-logs'),
        headers: {
          'Authorization': authHeader,
          'Content-Type': 'application/json',
        },
        body: jsonEncode(payload),
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        await prefs.setInt('last_synced_call_timestamp', latestTimestamp);
        print("Call logs synced successfully. Latest timestamp: $latestTimestamp");
      } else {
        print("Failed to sync call logs: ${response.statusCode}");
      }
    } catch (e) {
      print("Error in call log sync: $e");
    }
  }
}
