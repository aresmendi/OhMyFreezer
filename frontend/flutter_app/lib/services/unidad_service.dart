import '../models/unidad_medida.dart';
import 'api_client.dart';

/// Servicio HTTP para el catálogo global de unidades de medida.
class UnidadService {
  /// GET /api/unidades — todos los usuarios autenticados. El catálogo es
  /// idéntico para cualquier negocio (no está scoped por tenant).
  static Future<List<UnidadMedida>> getAll(String token) async {
    final data = await ApiClient.get('/unidades', token: token);
    return (data as List)
        .map((e) => UnidadMedida.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
