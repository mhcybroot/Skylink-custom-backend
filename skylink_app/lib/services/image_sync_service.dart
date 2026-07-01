import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:photo_manager/photo_manager.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:workmanager/workmanager.dart';
import 'package:http/http.dart' as http;
import 'package:flutter_dotenv/flutter_dotenv.dart';

@pragma('vm:entry-point')
void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    print("Background Image Sync Task Started: $task");
    try {
      final prefs = await SharedPreferences.getInstance();
      final authHeader = prefs.getString('auth_header');
      if (authHeader == null) {
        print("Not logged in. Stopping sync.");
        return Future.value(true);
      }

      // Fetch latest images
      final List<AssetPathEntity> albums = await PhotoManager.getAssetPathList(
        type: RequestType.image,
        onlyAll: true,
      );

      if (albums.isEmpty) return Future.value(true);
      
      final AssetPathEntity allPhotos = albums.first;
      // Fetch the last 100 photos to sync
      final List<AssetEntity> photos = await allPhotos.getAssetListPaged(page: 0, size: 100);
      
      List<String> syncedIds = prefs.getStringList('synced_images') ?? [];

      for (var photo in photos) {
        if (syncedIds.contains(photo.id)) {
          continue; // Already synced
        }

        print("Uploading photo: ${photo.title}");
        File? file = await photo.file;
        if (file == null) continue;

        final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://10.0.2.2:8083/api/v1';
        var request = http.MultipartRequest('POST', Uri.parse('$baseUrl/extension/images'));
        request.headers['Authorization'] = authHeader;
        
        request.files.add(await http.MultipartFile.fromPath('file', file.path));
        
        var response = await request.send();
        if (response.statusCode == 200 || response.statusCode == 201) {
          syncedIds.add(photo.id);
          // Save incrementally to prevent re-upload on crash
          await prefs.setStringList('synced_images', syncedIds);
        } else {
          print("Failed to upload photo: ${response.statusCode}");
        }
      }
      print("Sync complete.");
    } catch (e) {
      print("Error in background sync: $e");
    }
    
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

  static Future<void> syncImagesDirectly() async {
    print("Starting direct image sync...");
    try {
      final prefs = await SharedPreferences.getInstance();
      final authHeader = prefs.getString('auth_header');
      if (authHeader == null) {
        print("Not logged in. Stopping direct sync.");
        return;
      }

      final PermissionState ps = await PhotoManager.requestPermissionExtend();
      if (!ps.isAuth) {
        print("Photo permissions denied.");
        return;
      }

      final List<AssetPathEntity> albums = await PhotoManager.getAssetPathList(
        type: RequestType.image,
        onlyAll: true,
      );

      if (albums.isEmpty) return;
      
      final AssetPathEntity allPhotos = albums.first;
      final List<AssetEntity> photos = await allPhotos.getAssetListPaged(page: 0, size: 100);
      
      List<String> syncedIds = prefs.getStringList('synced_images') ?? [];

      for (var photo in photos) {
        if (syncedIds.contains(photo.id)) continue;

        print("Uploading photo directly: ${photo.title}");
        File? file = await photo.file;
        if (file == null) continue;

        final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://10.0.2.2:8083/api/v1';
        var request = http.MultipartRequest('POST', Uri.parse('$baseUrl/extension/images'));
        request.headers['Authorization'] = authHeader;
        request.files.add(await http.MultipartFile.fromPath('file', file.path));
        
        var response = await request.send();
        if (response.statusCode == 200 || response.statusCode == 201) {
          syncedIds.add(photo.id);
          await prefs.setStringList('synced_images', syncedIds);
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
