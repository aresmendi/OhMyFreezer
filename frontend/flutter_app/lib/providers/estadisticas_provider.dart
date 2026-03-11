import 'package:flutter/material.dart';
import '../models/estadistica.dart';
import '../services/estadistica_service.dart';

/// Gestiona las estadísticas de elaboración. Solo jefe de cocina.
class EstadisticasProvider extends ChangeNotifier {
  List<EstadisticaReceta> _estadisticas = [];
  bool _isLoading = false;
  String? _error;

  List<EstadisticaReceta> get estadisticas => _estadisticas;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Future<void> cargar(String token) async {
    _isLoading = true;
    notifyListeners();
    try {
      _estadisticas = await EstadisticaService.getEstadisticas(token);
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}