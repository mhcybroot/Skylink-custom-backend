import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import '../blocs/history_bloc.dart';

class HistoryTab extends StatefulWidget {
  @override
  _HistoryTabState createState() => _HistoryTabState();
}

class _HistoryTabState extends State<HistoryTab> {
  @override
  void initState() {
    super.initState();
    context.read<HistoryBloc>().add(HistoryLoadRequested());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Attendance History'),
      ),
      body: BlocBuilder<HistoryBloc, HistoryState>(
        builder: (context, state) {
          if (state is HistoryLoading || state is HistoryInitial) {
            return const Center(child: CircularProgressIndicator());
          }
          if (state is HistoryError) {
            return Center(child: Text('Error: ${state.error}', style: const TextStyle(color: Colors.red)));
          }
          if (state is HistoryLoaded) {
            return _buildHistoryContent(context, state.rangeReport);
          }
          return const SizedBox();
        },
      ),
    );
  }

  Widget _buildHistoryContent(BuildContext context, Map<String, dynamic> data) {
    final monthlyReports = data['monthlyReports'] as List<dynamic>? ?? [];
    if (monthlyReports.isEmpty) {
      return const Center(child: Text('No history found.'));
    }

    final currentMonth = monthlyReports.first;
    final dailyDetails = currentMonth['dailyDetails'] as List<dynamic>? ?? [];

    return RefreshIndicator(
      onRefresh: () async => context.read<HistoryBloc>().add(HistoryLoadRequested()),
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: dailyDetails.length,
        itemBuilder: (context, index) {
          final detail = dailyDetails[index];
          return _buildDailyCard(detail);
        },
      ),
    );
  }

  Widget _buildDailyCard(dynamic detail) {
    final status = detail['status'] ?? 'UNKNOWN';
    final date = detail['date'] ?? '';
    final dayOfWeek = detail['dayOfWeek'] ?? '';
    final inTime = detail['inTime'] ?? '--:--';
    final outTime = detail['outTime'] ?? '--:--';
    final activeWorkDuration = detail['activeWorkDuration'] ?? '00h 00m';

    Color statusColor = Colors.grey;
    if (status == 'PRESENT') statusColor = Colors.green;
    if (status == 'ABSENT') statusColor = Colors.red;
    if (status == 'LATE') statusColor = Colors.orange;

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  '$dayOfWeek, $date',
                  style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: statusColor.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: statusColor),
                  ),
                  child: Text(
                    status,
                    style: TextStyle(color: statusColor, fontWeight: FontWeight.bold, fontSize: 12),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                _buildTimeColumn('In Time', inTime, Icons.login, Colors.blue),
                const SizedBox(width: 24),
                _buildTimeColumn('Out Time', outTime, Icons.logout, Colors.purple),
                const Spacer(),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    const Text('Duration', style: TextStyle(color: Colors.grey, fontSize: 12)),
                    Text(
                      activeWorkDuration,
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                    ),
                  ],
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildTimeColumn(String label, String time, IconData icon, Color color) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(icon, size: 14, color: color),
            const SizedBox(width: 4),
            Text(label, style: const TextStyle(color: Colors.grey, fontSize: 12)),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          time.substring(0, 5), // '09:00:00' -> '09:00' (might need bound check)
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
        ),
      ],
    );
  }
}
