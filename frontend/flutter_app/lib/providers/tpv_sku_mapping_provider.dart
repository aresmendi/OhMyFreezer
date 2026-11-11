import 'package:flutter/material.dart';
import '../models/tpv_sku_mapping.dart';
import '../services/tpv_sku_mapping_service.dart';

/// Gestiona el estado de los mapeos SKU-TPV.
class TpvSkuMappingProvider extends ChangeNotifier {
  List<TpvSkuMapping> _mappings = [];
  bool _isLoading = false;
  String? _error;

  List<TpvSkuMapping> get mappings     => _mappings;
  bool                get isLoading    => _isLoading;
  String?             get error        => _error;

  /// Carga todos los mapeos desde el backend.
  Future<void> cargar(String token) async {
    _setLoading(true);
    try {
      _mappings = await TpvSkuMappingService.getAll(token);
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _setLoading(false);
    }
  }

  /// Asigna [recetaId] al mapping con [id] y actualiza la lista localmente.
  Future<void> asignarReceta(int id, int recetaId, String token) async {
    _setLoading(true);
    try {
      final actualizado = await TpvSkuMappingService.asignarReceta(
        id,
        recetaId,
        token,
      );
      final idx = _mappings.indexWhere((m) => m.id == id);
      if (idx != -1) _mappings[idx] = actualizado;
      _error = null;
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _setLoading(false);
    }
  }

  /// Elimina el mapeo de receta del SKU con [id] y recarga la lista.
  Future<void> desmapear(int id, String token) async {
    _setLoading(true);
    try {
      await TpvSkuMappingService.desmapear(id, token);
      // Recarga para reflejar el estado real del servidor (204 sin body)
      _mappings = await TpvSkuMappingService.getAll(token);
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
