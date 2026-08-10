import 'package:flutter/material.dart';
import '../models/unidad_medida.dart';
import '../services/unidad_service.dart';

/// Gestiona el catálogo global e inmutable de unidades de medida.
///
/// A diferencia del resto de providers de la app, este catálogo es idéntico
/// para todos los negocios y no cambia en tiempo de ejecución, así que
/// [cargar] solo dispara la petición HTTP una vez por sesión (salvo que se
/// pida explícitamente con `forzar: true`).
class UnidadesProvider extends ChangeNotifier {
  List<UnidadMedida> _todas = [];
  bool _isLoading = false;
  bool _cargado = false;
  String? _error;

  List<UnidadMedida> get todas => _todas;
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Unidades que comparten el [tipo] indicado (`"MASA"`, `"VOLUMEN"` o
  /// `"UNIDAD"`). Útil para alimentar selectores restringidos a la
  /// dimensión física de un ingrediente.
  List<UnidadMedida> porTipo(String tipo) =>
      _todas.where((u) => u.tipo == tipo).toList();

  /// Carga el catálogo desde el backend. No repite la petición si ya se
  /// cargó antes, salvo que [forzar] sea `true`.
  Future<void> cargar(String token, {bool forzar = false}) async {
    if (_cargado && !forzar) return;
    _isLoading = true;
    notifyListeners();
    try {
      _todas = await UnidadService.getAll(token);
      _cargado = true;
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
