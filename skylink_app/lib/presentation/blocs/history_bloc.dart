import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:equatable/equatable.dart';
import '../../data/repositories/history_repository.dart';

// Events
abstract class HistoryEvent extends Equatable {
  @override
  List<Object?> get props => [];
}

class HistoryLoadRequested extends HistoryEvent {}

// States
abstract class HistoryState extends Equatable {
  @override
  List<Object?> get props => [];
}

class HistoryInitial extends HistoryState {}
class HistoryLoading extends HistoryState {}
class HistoryLoaded extends HistoryState {
  final Map<String, dynamic> rangeReport;
  HistoryLoaded(this.rangeReport);
  @override
  List<Object?> get props => [rangeReport];
}
class HistoryError extends HistoryState {
  final String error;
  HistoryError(this.error);
  @override
  List<Object?> get props => [error];
}

// Bloc
class HistoryBloc extends Bloc<HistoryEvent, HistoryState> {
  final HistoryRepository repository;

  HistoryBloc({required this.repository}) : super(HistoryInitial()) {
    on<HistoryLoadRequested>((event, emit) async {
      emit(HistoryLoading());
      try {
        final data = await repository.fetchHistory();
        emit(HistoryLoaded(data));
      } catch (e) {
        emit(HistoryError(e.toString()));
      }
    });
  }
}
