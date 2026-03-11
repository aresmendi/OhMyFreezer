import '../models/estadistica.dart';
import 'api_client.dart';

/// Servicio HTTP para estadísticas. Solo jefe de cocina.
class EstadisticaService {

  /// GET /api/estadisticas/recetas
  static Future<List<EstadisticaReceta>> getEstadisticas(String token) async {
    final data = await ApiClient.get('/estadisticas/recetas', token: token);
    return (data as List)
        .map((e) => EstadisticaReceta.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}