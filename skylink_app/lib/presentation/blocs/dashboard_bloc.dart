import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../data/repositories/dashboard_repository.dart';

// Events
abstract class DashboardEvent extends Equatable {
  @override
  List<Object> get props => [];
}

class DashboardLoadRequested extends DashboardEvent {}
class DashboardStartWorkRequested extends DashboardEvent {}
class DashboardTakeBreakRequested extends DashboardEvent {}
class DashboardResumeWorkRequested extends DashboardEvent {}
class DashboardEndWorkRequested extends DashboardEvent {}

// States
abstract class DashboardState extends Equatable {
  @override
  List<Object> get props => [];
}

class DashboardInitial extends DashboardState {}
class DashboardLoading extends DashboardState {}
class DashboardLoaded extends DashboardState {
  final Map<String, dynamic> data;
  DashboardLoaded({required this.data});
  @override
  List<Object> get props => [data];
}
class DashboardError extends DashboardState {
  final String error;
  DashboardError({required this.error});
  @override
  List<Object> get props => [error];
}

// Bloc
class DashboardBloc extends Bloc<DashboardEvent, DashboardState> {
  final DashboardRepository repository;

  DashboardBloc({required this.repository}) : super(DashboardInitial()) {
    on<DashboardLoadRequested>(_onLoad);
    on<DashboardStartWorkRequested>(_onStartWork);
    on<DashboardTakeBreakRequested>(_onTakeBreak);
    on<DashboardResumeWorkRequested>(_onResumeWork);
    on<DashboardEndWorkRequested>(_onEndWork);
  }

  void _onLoad(DashboardLoadRequested event, Emitter<DashboardState> emit) async {
    emit(DashboardLoading());
    try {
      final data = await repository.fetchDashboardStatus();
      emit(DashboardLoaded(data: data));
    } catch (e) {
      emit(DashboardError(error: e.toString()));
    }
  }

  void _onStartWork(DashboardStartWorkRequested event, Emitter<DashboardState> emit) async {
    await repository.startWork();
    add(DashboardLoadRequested()); // Reload dashboard
  }

  void _onTakeBreak(DashboardTakeBreakRequested event, Emitter<DashboardState> emit) async {
    await repository.takeBreak();
    add(DashboardLoadRequested());
  }

  void _onResumeWork(DashboardResumeWorkRequested event, Emitter<DashboardState> emit) async {
    await repository.resumeWork();
    add(DashboardLoadRequested());
  }

  void _onEndWork(DashboardEndWorkRequested event, Emitter<DashboardState> emit) async {
    await repository.endWork();
    add(DashboardLoadRequested());
  }
}
