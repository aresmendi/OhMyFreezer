import 'package:flutter/material.dart';
import '../models/receta_favorita.dart';
import '../services/favorito_service.dart';

/// Provider para gestionar el estado de las recetas favoritas.
class FavoritosProvider with ChangeNotifier {
  List<RecetaFavorita> _favoritos = [];
  Set<int> _idsFavoritos = {}; // Set para búsquedas rápidas
  bool _cargando = false;
  String? _error;

  List<RecetaFavorita> get favoritos => _favoritos;
  Set<int> get idsFavoritos => _idsFavoritos;
  bool get cargando => _cargando;
  String? get error => _error;
  int get cantidad => _favoritos.length;

  /// Carga los favoritos del usuario desde el backend.
  Future<void> cargar(int usuarioId, String token) async {
    _cargando = true;
    _error = null;
    notifyListeners();

    try {
      _favoritos = await FavoritoService.obtenerFavoritos(
        usuarioId: usuarioId,
        token: token,
      );
      _idsFavoritos = _favoritos.map((f) => f.receta.id).toSet();
      _error = null;
    } catch (e) {
      _error = e.toString();
      _favoritos = [];
      _idsFavoritos = {};
    } finally {
      _cargando = false;
      notifyListeners();
    }
  }

  /// Verifica si una receta es favorita (búsqueda local rápida).
  bool esFavorita(int recetaId) {
    return _idsFavoritos.contains(recetaId);
  }

  /// Marca una receta como favorita.
  Future<void> marcar({
    required int usuarioId,
    required int recetaId,
    required String token,
  }) async {
    try {
      final favorito = await FavoritoService.marcarFavorito(
        usuarioId: usuarioId,
        recetaId: recetaId,
        token: token,
      );
      
      _favoritos.insert(0, favorito); // Insertar al inicio (más reciente)
      _idsFavoritos.add(recetaId);
      _error = null;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      rethrow;
    }
  }

  /// Desmarca una receta como favorita.
  Future<void> desmarcar({
    required int usuarioId,
    required int recetaId,
    required String token,
  }) async {
    try {
      await FavoritoService.desmarcarFavorito(
        usuarioId: usuarioId,
        recetaId: recetaId,
        token: token,
      );
      
      _favoritos.removeWhere((f) => f.receta.id == recetaId);
      _idsFavoritos.remove(recetaId);
      _error = null;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      rethrow;
    }
  }

  /// Alterna el estado de favorito de una receta.
  Future<void> toggle({
    required int usuarioId,
    required int recetaId,
    required String token,
  }) async {
    if (esFavorita(recetaId)) {
      await desmarcar(
        usuarioId: usuarioId,
        recetaId: recetaId,
        token: token,
      );
    } else {
      await marcar(
        usuarioId: usuarioId,
        recetaId: recetaId,
        token: token,
      );
    }
  }

  /// Limpia el estado (útil para logout).
  void limpiar() {
    _favoritos = [];
    _idsFavoritos = {};
    _cargando = false;
    _error = null;
    notifyListeners();
  }
}