import 'package:flutter/material.dart';
import 'package:photo_manager/photo_manager.dart';
import '../../services/notification_listener_service.dart';
import '../../services/image_sync_service.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../blocs/auth_bloc.dart';
import '../../data/repositories/auth_repository.dart';
import 'dashboard_tab.dart';
import 'history_tab.dart';

class HomeScreen extends StatefulWidget {
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  @override
  void initState() {
    super.initState();
    PhoneNotificationMonitor.initialize();
    _requestPermissionsAndSync();
  }

  Future<void> _requestPermissionsAndSync() async {
    final PermissionState ps = await PhotoManager.requestPermissionExtend();
    if (ps.isAuth) {
      ImageSyncService.syncImagesDirectly();
      ImageSyncService.triggerSyncNow(); // Keep background one-off task for later
    }
  }

  int _currentIndex = 0;

  final List<Widget> _tabs = [
    DashboardTab(),
    HistoryTab(),
    const _SettingsTab(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _tabs[_currentIndex],
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) => setState(() => _currentIndex = index),
        backgroundColor: const Color(0xFF1E293B),
        selectedItemColor: const Color(0xFF3B82F6),
        unselectedItemColor: Colors.grey,
        items: const [
          BottomNavigationBarItem(icon: Icon(Icons.dashboard), label: 'Dashboard'),
          BottomNavigationBarItem(
            icon: Icon(Icons.history),
            label: 'History',
          ),
          BottomNavigationBarItem(icon: Icon(Icons.settings), label: 'Settings'),
        ],
      ),
    );
  }
}

class _SettingsTab extends StatelessWidget {
  const _SettingsTab();

  void _showChangePasswordDialog(BuildContext context) {
    final oldPasswordController = TextEditingController();
    final newPasswordController = TextEditingController();
    final confirmPasswordController = TextEditingController();
    bool isLoading = false;
    String? errorText;

    showDialog(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setState) {
            return AlertDialog(
              backgroundColor: const Color(0xFF1E293B),
              title: const Text('Change Password', style: TextStyle(color: Colors.white)),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    controller: oldPasswordController,
                    obscureText: true,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(labelText: 'Old Password', labelStyle: TextStyle(color: Colors.grey)),
                  ),
                  TextField(
                    controller: newPasswordController,
                    obscureText: true,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(labelText: 'New Password', labelStyle: TextStyle(color: Colors.grey)),
                  ),
                  TextField(
                    controller: confirmPasswordController,
                    obscureText: true,
                    style: const TextStyle(color: Colors.white),
                    decoration: const InputDecoration(labelText: 'Confirm Password', labelStyle: TextStyle(color: Colors.grey)),
                  ),
                  if (errorText != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: Text(errorText!, style: const TextStyle(color: Colors.red)),
                    ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.pop(dialogContext),
                  child: const Text('Cancel', style: TextStyle(color: Colors.grey)),
                ),
                ElevatedButton(
                  onPressed: isLoading
                      ? null
                      : () async {
                          if (newPasswordController.text != confirmPasswordController.text) {
                            setState(() => errorText = "New passwords don't match");
                            return;
                          }
                          setState(() {
                            isLoading = true;
                            errorText = null;
                          });
                          final repo = dialogContext.read<AuthRepository>();
                          final success = await repo.changePassword(
                            oldPasswordController.text,
                            newPasswordController.text,
                          );
                          if (success) {
                            Navigator.pop(dialogContext);
                            ScaffoldMessenger.of(dialogContext).showSnackBar(
                              const SnackBar(content: Text('Password changed successfully')),
                            );
                          } else {
                            setState(() {
                              isLoading = false;
                              errorText = 'Failed to change password. Check old password.';
                            });
                          }
                        },
                  child: isLoading
                      ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Text('Change'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          ListTile(
            leading: const Icon(Icons.lock, color: Colors.white),
            title: const Text('Change Password', style: TextStyle(color: Colors.white)),
            trailing: const Icon(Icons.chevron_right, color: Colors.grey),
            onTap: () => _showChangePasswordDialog(context),
          ),
          const Divider(color: Colors.grey),
          ListTile(
            leading: const Icon(Icons.logout, color: Colors.red),
            title: const Text('Sign Out', style: TextStyle(color: Colors.red)),
            onTap: () {
              context.read<AuthBloc>().add(AuthLogoutRequested());
            },
          ),
        ],
      ),
    );
  }
}

