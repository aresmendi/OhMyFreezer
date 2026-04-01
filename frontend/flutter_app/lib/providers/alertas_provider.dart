import 'dart:async';
import 'package:flutter/material.dart';
import '../models/alerta.dart';
import '../services/alerta_service.dart';

/// Gestiona alertas de stock con polling cada 5 segundos.
/// Solo activo para el jefe de cocina.
class AlertasProvider extends ChangeNotifier {
  List<Alerta> _alertas = [];
  bool _isLoading = false;
  String? _error;
  Timer? _pollingTimer;

  List<Alerta> get alertas => _alertas;
  List<Alerta> get noLeidas => _alertas.where((a) => !a.leida).toList();
  int get contadorNoLeidas => noLeidas.length;
  bool get hayAlertas => contadorNoLeidas > 0;
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Inicia el polling. Llamar solo si [esJefeCocina] == true.
  void iniciarPolling(String token) {
    detenerPolling();
    _cargar(token);
    _pollingTimer = Timer.periodic(
      const Duration(seconds: 5),
      (_) => _cargar(token),
    );
  }

  /// Detiene el polling. Llamar en logout o dispose.
  void detenerPolling() {
    _pollingTimer?.cancel();
    _pollingTimer = null;
  }

  /// Recarga manual (pull-to-refresh).
  Future<void> recargar(String token) => _cargar(token);

  Future<void> _cargar(String token) async {
    try {
      _alertas = await AlertaService.getAllByUsuario(token);
      _error = null;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  /// Marca una alerta como leída localmente y sincroniza con el backend.
  Future<void> marcarLeida(int id, String token) async {
    try {
      await AlertaService.marcarLeida(id, token);
      final idx = _alertas.indexWhere((a) => a.id == id);
      if (idx != -1) {
        _alertas[idx] = _alertas[idx].copyWith(leida: true);
        notifyListeners();
      }
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  /// Marca todas las alertas como leídas.
  Future<void> marcarTodasLeidas(String token) async {
    try {
      await AlertaService.marcarTodasLeidas(token);
      _alertas = _alertas.map((a) => a.copyWith(leida: true)).toList();
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
    }
  }

  @override
  void dispose() {
    detenerPolling();
    super.dispose();
  }
}
