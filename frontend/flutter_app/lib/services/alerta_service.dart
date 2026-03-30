import '../models/alerta.dart';
import 'api_client.dart';

/// Servicio HTTP para alertas de stock. Solo jefe de cocina.
class AlertaService {
  /// GET /api/alertas/usuario — todas las alertas del usuario autenticado.
  static Future<List<Alerta>> getAllByUsuario(String token) async {
    final data = await ApiClient.get('/alertas/usuario', token: token);
    return (data as List)
        .map((e) => Alerta.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// PATCH /api/alertas/{id}/leer — marca una alerta como leída.
  static Future<void> marcarLeida(int id, String token) async {
    await ApiClient.patch('/alertas/$id/leer', {}, token: token);
  }

  /// PATCH /api/alertas/usuario/leer-todas — marca todas como leídas.
  static Future<void> marcarTodasLeidas(String token) async {
    await ApiClient.patch('/alertas/usuario/leer-todas', {}, token: token);
  }
}
