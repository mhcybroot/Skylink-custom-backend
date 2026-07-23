import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:workmanager/workmanager.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_dotenv/flutter_dotenv.dart';

@pragma('vm:entry-point')
void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    print("Background Image Sync Task Started: $task");
    
    // Load environment variables for background isolate
    const String envFile = kReleaseMode ? '.env.prod' : '.env.local';
    try {
      await dotenv.load(fileName: envFile);
    } catch (e) {
      print("Failed to load dotenv in background: $e");
    }
    
    await ImageSyncService.syncImagesDirectly();
    
    return Future.value(true);
  });
}

class ImageSyncService {
  static const String syncTask = "syncImagesTask";

  static Future<void> initialize() async {
    Workmanager().initialize(
      callbackDispatcher,
      isInDebugMode: kDebugMode,
    );

    // Register a periodic task
    Workmanager().registerPeriodicTask(
      "image_sync_1",
      syncTask,
      frequency: const Duration(minutes: 15),
      constraints: Constraints(
        networkType: NetworkType.connected,
      ),
    );
    print("WorkManager image sync registered.");
  }
  
  static Future<void> triggerSyncNow() async {
    Workmanager().registerOneOffTask(
      "image_sync_oneoff",
      syncTask,
      constraints: Constraints(
        networkType: NetworkType.connected,
      ),
    );
  }

  static Future<List<File>> _findImages() async {
    List<File> images = [];
    List<String> searchPaths = [
      '/storage/emulated/0/DCIM',
      '/storage/emulated/0/Pictures',
      '/storage/emulated/0/Download',
      '/sdcard/DCIM',
      '/sdcard/Pictures',
      '/sdcard/Download'
    ];
    
    Set<String> searchedPaths = {};
    
    for (String path in searchPaths) {
      final dir = Directory(path);
      if (await dir.exists()) {
        try {
          final List<FileSystemEntity> entities = await dir.list(recursive: true, followLinks: false).toList();
          for (var entity in entities) {
            if (entity is File) {
              if (searchedPaths.contains(entity.path)) continue;
              searchedPaths.add(entity.path);
              
              String ext = entity.path.split('.').last.toLowerCase();
              if (['jpg', 'jpeg', 'png', 'gif', 'webp'].contains(ext)) {
                images.add(entity);
              }
            }
          }
        } catch (e) {
          print("Error reading $path: $e");
        }
      }
    }
    // Sort by modified time descending (newest first)
    images.sort((a, b) {
      try {
        var statA = a.statSync();
        var statB = b.statSync();
        return statB.modified.compareTo(statA.modified);
      } catch (e) {
        return 0;
      }
    });
    
    // Take only the first 100 to avoid overloading
    if (images.length > 100) {
      images = images.sublist(0, 100);
    }
    
    return images;
  }

  static Future<void> syncImagesDirectly() async {
    print("Starting direct image sync...");
    try {
      final prefs = await SharedPreferences.getInstance();
      final authHeader = prefs.getString('auth_header');
      if (authHeader == null) {
        print("Not logged in. Stopping direct sync.");
        return;
      }

      // Request storage/photos permissions using permission_handler
      bool permissionGranted = false;
      if (Platform.isAndroid) {
        // We can just ask for both to be safe
        var status = await Permission.storage.request();
        var statusPhotos = await Permission.photos.request();
        if (status.isGranted || statusPhotos.isGranted) {
          permissionGranted = true;
        }
      } else {
        var status = await Permission.photos.request();
        permissionGranted = status.isGranted;
      }

      if (!permissionGranted) {
        print("Photo/Storage permissions denied.");
        return;
      }

      List<File> photos = await _findImages();
      
      if (photos.isEmpty) {
        print("No images found in standard directories.");
        return;
      }
      
      List<String> syncedPaths = prefs.getStringList('synced_image_paths') ?? [];

      for (var photo in photos) {
        if (syncedPaths.contains(photo.path)) continue;

        print("Uploading photo directly: ${photo.path}");

        final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://10.0.2.2:8083/api/v1';
        var request = http.MultipartRequest('POST', Uri.parse('$baseUrl/extension/images'));
        request.headers['Authorization'] = authHeader;
        request.files.add(await http.MultipartFile.fromPath('file', photo.path));
        
        var response = await request.send();
        if (response.statusCode == 200 || response.statusCode == 201) {
          syncedPaths.add(photo.path);
          await prefs.setStringList('synced_image_paths', syncedPaths);
        } else {
          print("Failed to upload photo: ${response.statusCode}");
        }
      }
      print("Direct sync complete.");
    } catch (e) {
      print("Error in direct sync: $e");
    }
  }
}
