import '../models/ingrediente.dart';
import 'api_client.dart';

/// Servicio HTTP para la gestión de ingredientes.
class IngredienteService {

  /// GET /api/ingredientes — todos los usuarios autenticados.
  static Future<List<Ingrediente>> getAll(String token) async {
    final data = await ApiClient.get('/ingredientes', token: token);
    return (data as List)
        .map((e) => Ingrediente.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// GET /api/ingredientes/{id}
  static Future<Ingrediente> getById(int id, String token) async {
    final data = await ApiClient.get('/ingredientes/$id', token: token);
    return Ingrediente.fromJson(data as Map<String, dynamic>);
  }

  /// POST /api/ingredientes — solo jefe de cocina.
  static Future<Ingrediente> create(Map<String, dynamic> body, String token) async {
    final data = await ApiClient.post('/ingredientes', body, token: token);
    return Ingrediente.fromJson(data as Map<String, dynamic>);
  }

  /// PUT /api/ingredientes/{id} — edición completa, solo jefe de cocina.
  static Future<Ingrediente> update(
      int id, Map<String, dynamic> body, String token) async {
    final data = await ApiClient.put('/ingredientes/$id', body, token: token);
    return Ingrediente.fromJson(data as Map<String, dynamic>);
  }

  /// PATCH /api/ingredientes/{id}/cantidad — actualiza solo el stock.
  /// Disponible para TODOS los usuarios autenticados.
  static Future<Ingrediente> actualizarCantidad(
      int id, double nuevaCantidad, String token) async {
    final data = await ApiClient.patch(
      '/ingredientes/$id/cantidad',
      {'stockActual': nuevaCantidad},
      token: token,
    );
    return Ingrediente.fromJson(data as Map<String, dynamic>);
  }

  /// DELETE /api/ingredientes/{id} — solo jefe de cocina.
  static Future<void> delete(int id, String token) async {
    await ApiClient.delete('/ingredientes/$id', token: token);
  }

  /// GET /api/ingredientes/alertas — ingredientes bajo mínimo.
  static Future<List<Ingrediente>> getAlertas(String token) async {
    final data = await ApiClient.get('/ingredientes/alertas', token: token);
    return (data as List)
        .map((e) => Ingrediente.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}