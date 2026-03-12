import '../models/receta.dart';
import 'api_client.dart';

/// Servicio HTTP para la gestión de recetas.
class RecetaService {

  /// GET /api/recetas — todos los usuarios autenticados.
  static Future<List<Receta>> getAll(String token) async {
    final data = await ApiClient.get('/recetas', token: token);
    return (data as List)
        .map((e) => Receta.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// GET /api/recetas/{id}
  static Future<Receta> getById(int id, String token) async {
    final data = await ApiClient.get('/recetas/$id', token: token);
    return Receta.fromJson(data as Map<String, dynamic>);
  }

  /// POST /api/recetas — solo jefe de cocina.
  static Future<Receta> create(Map<String, dynamic> body, String token) async {
    final data = await ApiClient.post('/recetas', body, token: token);
    return Receta.fromJson(data as Map<String, dynamic>);
  }

  /// PUT /api/recetas/{id} — solo jefe de cocina.
  static Future<Receta> update(
      int id, Map<String, dynamic> body, String token) async {
    final data = await ApiClient.put('/recetas/$id', body, token: token);
    return Receta.fromJson(data as Map<String, dynamic>);
  }

  /// DELETE /api/recetas/{id} — solo jefe de cocina.
  static Future<void> delete(int id, int usuarioId, String token) async {
    await ApiClient.delete('/recetas/$id?usuarioId=$usuarioId', token: token);
  }

  /// GET /api/recetas/{id}/verificar — comprueba si hay stock suficiente.
  /// Devuelve la [Receta] con [puedeElaborarse] actualizado.
  static Future<Receta> verificar(int id, String token) async {
    final data = await ApiClient.get('/recetas/$id/verificar', token: token);
    return Receta.fromJson(data as Map<String, dynamic>);
  }

  /// POST /api/recetas/{id}/elaborar — descuenta stock y registra uso.
  static Future<void> elaborar(int id, String token) async {
    await ApiClient.post('/recetas/$id/elaborar', {}, token: token);
  }
}