import '../models/alerta.dart';
import 'api_client.dart';

/// Servicio HTTP para alertas de stock. Solo jefe de cocina.
class AlertaService {

  /// GET /api/alertas — todas las alertas.
  static Future<List<Alerta>> getAll(String token) async {
    final data = await ApiClient.get('/alertas', token: token);
    return (data as List)
        .map((e) => Alerta.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// PATCH /api/alertas/{id}/leer — marca una alerta como leída.
  static Future<void> marcarLeida(int id, String token) async {
    await ApiClient.patch('/alertas/$id/leer', {}, token: token);
  }

  /// PATCH /api/alertas/leer-todas — marca todas como leídas.
  static Future<void> marcarTodasLeidas(String token) async {
    await ApiClient.patch('/alertas/leer-todas', {}, token: token);
  }
}