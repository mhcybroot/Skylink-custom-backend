import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'core/api_client.dart';
import 'data/repositories/auth_repository.dart';
import 'data/repositories/dashboard_repository.dart';
import 'data/repositories/history_repository.dart';
import 'presentation/blocs/auth_bloc.dart';
import 'presentation/blocs/dashboard_bloc.dart';
import 'presentation/blocs/history_bloc.dart';
import 'presentation/screens/home_screen.dart';
import 'presentation/screens/login_screen.dart';
import 'theme/app_theme.dart';

import 'services/image_sync_service.dart';
import 'services/call_log_sync_service.dart';

import 'package:flutter/foundation.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  const String envFile = kReleaseMode ? '.env.prod' : '.env.local';
  await dotenv.load(fileName: envFile);
  await ImageSyncService.initialize();
  await CallLogSyncService.initialize();
  
  final apiClient = ApiClient();
  
  runApp(MyApp(
    authRepository: AuthRepository(apiClient: apiClient),
    dashboardRepository: DashboardRepository(apiClient: apiClient),
    historyRepository: HistoryRepository(apiClient: apiClient),
  ));
}

class MyApp extends StatelessWidget {
  final AuthRepository authRepository;
  final DashboardRepository dashboardRepository;
  final HistoryRepository historyRepository;

  const MyApp({
    Key? key,
    required this.authRepository,
    required this.dashboardRepository,
    required this.historyRepository,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return RepositoryProvider.value(
      value: authRepository,
      child: MultiBlocProvider(
        providers: [
          BlocProvider(
            create: (_) => AuthBloc(authRepository: authRepository)..add(AuthCheckRequested()),
          ),
        BlocProvider(
          create: (_) => DashboardBloc(repository: dashboardRepository),
        ),
        BlocProvider(
          create: (_) => HistoryBloc(repository: historyRepository),
        ),
      ],
      child: MaterialApp(
        title: 'Skylink Employee',
        theme: AppTheme.darkTheme,
        home: BlocBuilder<AuthBloc, AuthState>(
          builder: (context, state) {
            if (state is AuthInitial || state is AuthLoading) {
              return const Scaffold(body: Center(child: CircularProgressIndicator()));
            }
            if (state is AuthAuthenticated) {
              return HomeScreen();
            }
            return LoginScreen();
          },
        ),
      ),
    ));
  }
}
