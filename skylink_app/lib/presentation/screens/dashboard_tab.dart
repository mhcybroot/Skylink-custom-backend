import 'dart:async';
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
                  StatusCard(data: data),
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

class StatusCard extends StatefulWidget {
  final Map<String, dynamic> data;
  const StatusCard({Key? key, required this.data}) : super(key: key);

  @override
  _StatusCardState createState() => _StatusCardState();
}

class _StatusCardState extends State<StatusCard> {
  Timer? _timer;
  late int _displaySeconds;
  late String _status;
  late bool _isCountingDown;

  @override
  void initState() {
    super.initState();
    _updateFromData();
  }

  @override
  void didUpdateWidget(StatusCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    _updateFromData();
  }

  void _updateFromData() {
    _status = widget.data['status'] ?? 'UNKNOWN';
    _timer?.cancel();
    
    if (_status == 'WORKING') {
      _displaySeconds = widget.data['remainingSeconds'] ?? 0;
      _isCountingDown = true;
      _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
        setState(() {
          if (_displaySeconds > 0) _displaySeconds--;
        });
      });
    } else if (_status == 'ON_BREAK') {
      int totalBreakSeconds = widget.data['totalBreakSeconds'] ?? 0;
      String? serverTimeISO = widget.data['serverTimeISO'];
      String? breakStartISO = widget.data['breakStartISO'];
      
      int activeBreakSeconds = 0;
      if (serverTimeISO != null && breakStartISO != null) {
        try {
          DateTime serverTime = DateTime.parse(serverTimeISO);
          DateTime breakStart = DateTime.parse(breakStartISO);
          activeBreakSeconds = serverTime.difference(breakStart).inSeconds;
          if (activeBreakSeconds < 0) activeBreakSeconds = 0;
        } catch (e) {
          // Ignore parsing errors
        }
      }
      
      _displaySeconds = totalBreakSeconds + activeBreakSeconds;
      _isCountingDown = false;
      
      _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
        setState(() {
          _displaySeconds++;
        });
      });
    } else {
      _displaySeconds = 0;
      _isCountingDown = false;
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    if (_status == 'NOT_ENTERED' || _status == 'ENTERED_OFFICE' || _status == 'LOGGED_IN') {
      return _buildCardWrapper(
        context,
        display: "Standby",
        subtext: "Start work to begin your 8-hour shift countdown!",
        displayColor: Colors.black54,
        isTime: false,
      );
    }
    
    if (_status == 'ENDED_WORK' || _status == 'LEFT_WITHOUT_PUNCH' || _status == 'COMPLETED_DAY') {
      return _buildCardWrapper(
        context,
        display: "Shift Ended",
        subtext: "Great work today! Go enjoy your evening.",
        displayColor: Colors.green,
        isTime: false,
      );
    }

    int hours = _displaySeconds ~/ 3600;
    int minutes = (_displaySeconds % 3600) ~/ 60;
    int seconds = _displaySeconds % 60;
    final elapsed = '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}';
    
    if (_status == 'ON_BREAK') {
      return _buildCardWrapper(
        context,
        display: elapsed,
        subtext: "You are currently on break. Timer is pausing shift.",
        displayColor: Colors.orange,
        isTime: true,
      );
    }
    
    // WORKING
    return _buildCardWrapper(
      context,
      display: elapsed,
      subtext: "Time remaining until 8 hours is complete!",
      displayColor: const Color(0xFF3B82F6),
      isTime: true,
    );
  }

  Widget _buildCardWrapper(BuildContext context, {required String display, required String subtext, required Color displayColor, required bool isTime}) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          children: [
            Text(_status.replaceAll('_', ' '), style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: const Color(0xFF3B82F6))),
            const SizedBox(height: 16),
            Text(display, style: TextStyle(fontSize: isTime ? 48 : 36, fontWeight: isTime ? FontWeight.w300 : FontWeight.bold, color: displayColor)),
            const SizedBox(height: 8),
            Text(subtext, style: const TextStyle(fontSize: 14, color: Colors.grey), textAlign: TextAlign.center),
            const SizedBox(height: 24),
            _buildActionButtons(context, _status),
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
}
