import '../models/tpv_sku_mapping.dart';
import 'api_client.dart';

/// Servicio HTTP para la gestión de mapeos SKU-TPV.
class TpvSkuMappingService {
  /// GET /api/tpv-mapeos/sku-mappings — solo jefe de cocina.
  static Future<List<TpvSkuMapping>> getAll(String token) async {
    final data = await ApiClient.get('/tpv-mapeos/sku-mappings', token: token);
    return (data as List)
        .map((e) => TpvSkuMapping.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// PUT /api/tpv-mapeos/sku-mappings/{id} — asigna una receta al SKU.
  static Future<TpvSkuMapping> asignarReceta(
    int id,
    int recetaId,
    String token,
  ) async {
    final data = await ApiClient.put(
      '/tpv-mapeos/sku-mappings/$id',
      {'recetaId': recetaId},
      token: token,
    );
    return TpvSkuMapping.fromJson(data as Map<String, dynamic>);
  }

  /// DELETE /api/tpv-mapeos/sku-mappings/{id}/receta — elimina el mapeo.
  static Future<void> desmapear(int id, String token) async {
    await ApiClient.delete(
      '/tpv-mapeos/sku-mappings/$id/receta',
      token: token,
    );
  }
}
