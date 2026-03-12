import 'package:flutter/material.dart';
import '../models/ingrediente.dart';
import '../services/ingrediente_service.dart';

/// Gestiona el estado del inventario de ingredientes.
///
/// - Todos los usuarios pueden ver y actualizar cantidades.
/// - Solo el jefe de cocina puede crear, editar completamente o eliminar.
class IngredientesProvider extends ChangeNotifier {
  List<Ingrediente> _ingredientes = [];
  bool _isLoading = false;
  String? _error;

  List<Ingrediente> get ingredientes => _ingredientes;
  bool get isLoading => _isLoading;
  String? get error => _error;

  /// Ingredientes con stock por debajo del mínimo.
  List<Ingrediente> get conAlertaStock =>
      _ingredientes.where((i) => i.tieneAlertaStock).toList();

  /// Carga todos los ingredientes desde el backend.
  Future<void> cargar(String token) async {
    _setLoading(true);
    try {
      _ingredientes = await IngredienteService.getAll(token);
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _setLoading(false);
    }
  }

  /// Actualiza solo la cantidad (stock) de un ingrediente.
  /// Disponible para TODOS los usuarios autenticados.
  Future<void> actualizarCantidad(
      int id, double nuevaCantidad, String token) async {
    try {
      final actualizado = await IngredienteService.actualizarCantidad(
          id, nuevaCantidad, token);
      final idx = _ingredientes.indexWhere((i) => i.id == id);
      if (idx != -1) {
        _ingredientes[idx] = actualizado;
        notifyListeners();
      }
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      rethrow;
    }
  }

  /// Crea un nuevo ingrediente. Solo jefe de cocina.
  Future<void> crear(Map<String, dynamic> body, String token) async {
    _setLoading(true);
    try {
      final nuevo = await IngredienteService.create(body, token);
      _ingredientes.add(nuevo);
      _error = null;
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _setLoading(false);
    }
  }

  /// Edición completa de un ingrediente. Solo jefe de cocina.
  Future<void> actualizar(
      int id, Map<String, dynamic> body, String token) async {
    _setLoading(true);
    try {
      final actualizado = await IngredienteService.update(id, body, token);
      final idx = _ingredientes.indexWhere((i) => i.id == id);
      if (idx != -1) _ingredientes[idx] = actualizado;
      _error = null;
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _setLoading(false);
    }
  }

  /// Elimina un ingrediente del servidor y de la lista local.
  Future<void> eliminar(int id, int usuarioId, String token) async {
    _isLoading = true;
    notifyListeners();
    try {
      await IngredienteService.delete(id, usuarioId, token);
      _ingredientes.removeWhere((i) => i.id == id);
      _error = null;
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _setLoading(false);
    }
  }

  void _setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }
}