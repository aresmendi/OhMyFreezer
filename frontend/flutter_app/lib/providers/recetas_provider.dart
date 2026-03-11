import 'package:flutter/material.dart';
import '../models/receta.dart';
import '../services/receta_service.dart';

/// Gestiona el estado de las recetas y el flujo de elaboración.
class RecetasProvider extends ChangeNotifier {
  List<Receta> _recetas = [];
  Receta? _seleccionada;
  bool _isLoading = false;
  String? _error;

  List<Receta> get recetas => _recetas;
  Receta? get seleccionada => _seleccionada;
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Carga todas las recetas.
  Future<void> cargar(String token) async {
    _setLoading(true);
    try {
      _recetas = await RecetaService.getAll(token);
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _setLoading(false);
    }
  }

  /// Carga el detalle de una receta y la marca como seleccionada.
  Future<void> seleccionar(int id, String token) async {
    _setLoading(true);
    try {
      _seleccionada = await RecetaService.getById(id, token);
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _setLoading(false);
    }
  }

  /// Verifica stock y actualiza [puedeElaborarse] en la receta seleccionada.
  Future<void> verificar(int id, String token) async {
    _setLoading(true);
    try {
      final verificada = await RecetaService.verificar(id, token);
      _seleccionada = verificada;
      // Actualiza también en la lista general
      final idx = _recetas.indexWhere((r) => r.id == id);
      if (idx != -1) _recetas[idx] = verificada;
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _setLoading(false);
    }
  }

  /// Elabora la receta: descuenta stock y registra el uso.
  Future<void> elaborar(int id, String token) async {
    _setLoading(true);
    try {
      await RecetaService.elaborar(id, token);
      // Resetea puedeElaborarse tras elaborar (el stock ha cambiado)
      if (_seleccionada?.id == id) {
        _seleccionada = _seleccionada!.copyWith(puedeElaborarse: null);
      }
      _error = null;
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _setLoading(false);
    }
  }

  /// Crea una nueva receta. Solo jefe de cocina.
  Future<void> crear(Map<String, dynamic> body, String token) async {
    _setLoading(true);
    try {
      final nueva = await RecetaService.create(body, token);
      _recetas.add(nueva);
      _error = null;
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _setLoading(false);
    }
  }

  /// Actualiza una receta existente.Solo Jefe de Cocina
Future<void> actualizar(int id, Map<String, dynamic> body, String token) async {
  _setLoading(true);
  try {
    final actualizada = await RecetaService.update(id, body, token);

    final idx = _recetas.indexWhere((r) => r.id == id);
    if (idx != -1) _recetas[idx] = actualizada;

    if (_seleccionada?.id == id) {
      _seleccionada = actualizada;
    }

    _error = null;
  } catch (e) {
    _error = e.toString();
    rethrow;
  } finally {
    _setLoading(false);
  }
}

  /// Elimina una receta. Solo jefe de cocina.
  Future<void> eliminar(int id, String token) async {
    _setLoading(true);
    try {
      await RecetaService.delete(id, token);
      _recetas.removeWhere((r) => r.id == id);
      if (_seleccionada?.id == id) _seleccionada = null;
      _error = null;
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _setLoading(false);
    }
  }

  void limpiarSeleccion() {
    _seleccionada = null;
    notifyListeners();
  }

  void _setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }
}