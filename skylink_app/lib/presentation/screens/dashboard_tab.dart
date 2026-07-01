import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../blocs/dashboard_bloc.dart';

import '../../services/image_sync_service.dart';
import '../../services/call_log_sync_service.dart';

class DashboardTab extends StatefulWidget {
  @override
  _DashboardTabState createState() => _DashboardTabState();
}

class _DashboardTabState extends State<DashboardTab> {
  @override
  void initState() {
    super.initState();
    context.read<DashboardBloc>().add(DashboardLoadRequested());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Dashboard'),
        actions: [
          IconButton(
            icon: const Icon(Icons.sync),
            tooltip: 'Sync Images',
            onPressed: () {
              ImageSyncService.syncImagesDirectly();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Image sync started')),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.phone_callback),
            tooltip: 'Sync Call Logs',
            onPressed: () {
              CallLogSyncService.syncCallLogsDirectly();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Call log sync started')),
              );
            },
          ),
        ],
      ),
      body: BlocBuilder<DashboardBloc, DashboardState>(
        builder: (context, state) {
          if (state is DashboardLoading || state is DashboardInitial) {
            return const Center(child: CircularProgressIndicator());
          }
          if (state is DashboardError) {
            return Center(child: Text('Error: ${state.error}', style: const TextStyle(color: Colors.red)));
          }
          if (state is DashboardLoaded) {
            final data = state.data;
            return RefreshIndicator(
              onRefresh: () async => context.read<DashboardBloc>().add(DashboardLoadRequested()),
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildStatusCard(context, data),
                  const SizedBox(height: 16),
                  _buildStatsGrid(data),
                ],
              ),
            );
          }
          return const SizedBox();
        },
      ),
    );
  }

  Widget _buildStatusCard(BuildContext context, Map<String, dynamic> data) {
    final status = data['status'] ?? 'UNKNOWN';
    final elapsedWorkSeconds = data['elapsedWorkSeconds'] ?? 0;
    
    // Format seconds into HH:mm:ss
    int hours = elapsedWorkSeconds ~/ 3600;
    int minutes = (elapsedWorkSeconds % 3600) ~/ 60;
    int seconds = elapsedWorkSeconds % 60;
    final elapsed = '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
    
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          children: [
            Text(status.replaceAll('_', ' '), style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Color(0xFF3B82F6))),
            const SizedBox(height: 16),
            Text(elapsed, style: const TextStyle(fontSize: 48, fontWeight: FontWeight.w300)),
            const SizedBox(height: 32),
            _buildActionButtons(context, status),
          ],
        ),
      ),
    );
  }

  Widget _buildActionButtons(BuildContext context, String status) {
    if (status == 'NOT_ENTERED') {
      return const Column(
        children: [
          Icon(Icons.warning, color: Colors.amber, size: 48),
          SizedBox(height: 8),
          Text(
            'Please punch your card at the office machine before starting work.',
            textAlign: TextAlign.center,
            style: TextStyle(color: Colors.grey),
          ),
        ],
      );
    }
    if (status == 'LOGGED_IN' || status == 'ENTERED_OFFICE') {
      return ElevatedButton(
        onPressed: () => context.read<DashboardBloc>().add(DashboardStartWorkRequested()),
        style: ElevatedButton.styleFrom(backgroundColor: Colors.green, minimumSize: const Size(double.infinity, 50)),
        child: const Text('Start Work'),
      );
    }
    if (status == 'WORKING') {
      return Row(
        children: [
          Expanded(
            child: ElevatedButton(
              onPressed: () => context.read<DashboardBloc>().add(DashboardTakeBreakRequested()),
              style: ElevatedButton.styleFrom(backgroundColor: Colors.orange),
              child: const Text('Take Break'),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: ElevatedButton(
              onPressed: () => context.read<DashboardBloc>().add(DashboardEndWorkRequested()),
              style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
              child: const Text('End Work'),
            ),
          ),
        ],
      );
    }
    if (status == 'ON_BREAK') {
      return Row(
        children: [
          Expanded(
            child: ElevatedButton(
              onPressed: () => context.read<DashboardBloc>().add(DashboardResumeWorkRequested()),
              style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
              child: const Text('Resume Work'),
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: ElevatedButton(
              onPressed: () => context.read<DashboardBloc>().add(DashboardEndWorkRequested()),
              style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
              child: const Text('End Work'),
            ),
          ),
        ],
      );
    }
    return ElevatedButton(
      onPressed: null,
      style: ElevatedButton.styleFrom(minimumSize: const Size(double.infinity, 50)),
      child: const Text('Work Ended'),
    );
  }

  Widget _buildStatsGrid(Map<String, dynamic> data) {
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      mainAxisSpacing: 16,
      crossAxisSpacing: 16,
      childAspectRatio: 1.5,
      children: [
        _buildStatBox('Days Present', '${data['daysPresent'] ?? 0}', Colors.green),
        _buildStatBox('Days Late', '${data['lateCount'] ?? 0}', Colors.red),
        _buildStatBox('Leaves Taken', '${data['leaveCount'] ?? 0}', Colors.purple),
        _buildStatBox('Early Exit', '${data['earlyCount'] ?? 0}', Colors.orange),
      ],
    );
  }

  Widget _buildStatBox(String title, String value, Color color) {
    return Card(
      elevation: 4,
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(value, style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold, color: color)),
            const SizedBox(height: 8),
            Text(title, style: const TextStyle(fontSize: 14, color: Colors.grey)),
          ],
        ),
      ),
    );
  }
}
