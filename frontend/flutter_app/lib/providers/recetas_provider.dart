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
  Future<void> verificar(int id, int usuarioId, String token) async {
    _setLoading(true);
    try {
      final disponible = await RecetaService.verificar(id, usuarioId, token);
      if (_seleccionada?.id == id) {
        _seleccionada = _seleccionada!.copyWith(puedeElaborarse: disponible);
      }
      final idx = _recetas.indexWhere((r) => r.id == id);
      if (idx != -1)
        _recetas[idx] = _recetas[idx].copyWith(puedeElaborarse: disponible);
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _setLoading(false);
    }
  }

  /// Elabora la receta: descuenta stock y registra el uso.
  Future<void> elaborar(
    int id,
    int usuarioId,
    String token, {
    bool? completada,
  }) async {
    _setLoading(true);
    try {
      // Si usuarioId es 0 o null, el backend fallará al buscar el usuario
      if (usuarioId <= 0) throw Exception("ID de usuario no válido");

      await RecetaService.elaborar(
        id,
        usuarioId,
        token,
        completada: completada,
      );

      // Después de elaborar, revalidar stock de los ingredientes de la receta
      // seleccionada para actualizar el estado de las demás recetas que la usan.
      if (_seleccionada?.id == id && _seleccionada!.ingredientes.isNotEmpty) {
        final futures = _seleccionada!.ingredientes
            .map(
              (ri) =>
                  verificarPorIngrediente(ri.ingrediente.id, usuarioId, token),
            )
            .toList();
        await Future.wait(futures);
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
  Future<void> crear(Receta receta, String token) async {
    _setLoading(true);
    try {
      final nueva = await RecetaService.create(receta, token);
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
  Future<void> actualizar(int id, Receta receta, String token) async {
    _setLoading(true);
    try {
      final actualizada = await RecetaService.update(id, receta, token);

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
  Future<void> eliminar(int id, int usuarioId, String token) async {
    _setLoading(true);
    try {
      await RecetaService.delete(id, usuarioId, token);
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

  /// Actualiza el estado de favorito de una receta localmente.
  /// Útil para reflejar cambios sin recargar todas las recetas.
  void actualizarFavoritoLocal(int recetaId, bool esFavorita) {
    final index = _recetas.indexWhere((r) => r.id == recetaId);
    if (index != -1) {
      _recetas[index] = _recetas[index].copyWith(esFavorita: esFavorita);
      notifyListeners();
    }
  }

  /// Sincroniza el campo esFavorita de todas las recetas con los IDs favoritos.
  void sincronizarFavoritos(Set<int> idsFavoritos) {
    bool changed = false;
    for (int i = 0; i < _recetas.length; i++) {
      final receta = _recetas[i];
      final esFavorita = idsFavoritos.contains(receta.id);
      if (receta.esFavorita != esFavorita) {
        _recetas[i] = receta.copyWith(esFavorita: esFavorita);
        changed = true;
      }
    }
    if (changed) notifyListeners();
  }

  /// Verifica la disponibilidad de todas las recetas que contienen el ingrediente
  /// con ID [ingredienteId]. Esto permite refrescar de forma selectiva las
  /// recetas afectadas tras cambiar el stock de un ingrediente.
  Future<void> verificarPorIngrediente(
    int ingredienteId,
    int usuarioId,
    String token,
  ) async {
    // Filtrar recetas que usan el ingrediente indicado
    final idsAfectados = _recetas
        .where(
          (r) => r.ingredientes.any((ri) => ri.ingrediente.id == ingredienteId),
        )
        .map((r) => r.id)
        .toList();

    if (idsAfectados.isEmpty) return;

    // Verificar todas las recetas afectadas en paralelo
    final futures = idsAfectados.map((id) => verificar(id, usuarioId, token));
    await Future.wait(futures);
  }
}
