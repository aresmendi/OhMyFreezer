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
  static Future<Receta> create(Receta receta, String token) async {
    final body = {
      'nombre': receta.nombre,
      'descripcion': receta.descripcion,
      'creadaPorId': receta.creadaPorId,
      'pasos': receta.pasos
          .map((p) => {'orden': p.orden, 'descripcion': p.descripcion})
          .toList(),
      'ingredientes': receta.ingredientes
          .map(
            (i) => {
              'ingredienteId':
                  i.ingrediente.id, // Enviamos solo el ID como pide el DTO
              'cantidadNecesaria': i.cantidadNecesaria,
            },
          )
          .toList(),
    };
    final data = await ApiClient.post('/recetas', body, token: token);
    return Receta.fromJson(data as Map<String, dynamic>);
  }

  /// PUT /api/recetas/{id} — solo jefe de cocina.
  static Future<Receta> update(int id, Receta receta, String token) async {
    final body = {
      'nombre': receta.nombre,
      'descripcion': receta.descripcion,
      'creadaPorId': receta.creadaPorId,
      'pasos': receta.pasos
          .map((p) => {'orden': p.orden, 'descripcion': p.descripcion})
          .toList(),
      'ingredientes': receta.ingredientes
          .map(
            (i) => {
              'ingredienteId': i.ingrediente.id,
              'cantidadNecesaria': i.cantidadNecesaria,
            },
          )
          .toList(),
    };
    final data = await ApiClient.put('/recetas/$id', body, token: token);
    return Receta.fromJson(data as Map<String, dynamic>);
  }

  /// DELETE /api/recetas/{id} — solo jefe de cocina.
  static Future<void> delete(int id, int usuarioId, String token) async {
    await ApiClient.delete('/recetas/$id?usuarioId=$usuarioId', token: token);
  }

  /// GET /api/recetas/{id}/verificar — comprueba si hay stock suficiente.
  static Future<bool> verificar(int id, int usuarioId, String token) async {
    final body = {'usuarioId': usuarioId};
    final data = await ApiClient.post(
      '/recetas/$id/verificar',
      body,
      token: token,
    );
    // El backend devuelve VerificarRecetaResponse que tiene el campo 'disponible'
    return data['disponible'] as bool;
  }

  /// POST /api/recetas/{id}/elaborar — descuenta stock y registra uso.
  static Future<void> elaborar(int id, int usuarioId, String token) async {
    print(
      'DEBUG: Elaborando receta ID: $id para usuario: $usuarioId',
    ); // Log de depuración

    final body = {
      'usuarioId':
          usuarioId, // Este campo DEBE coincidir con ElaborarRecetaRequest.java
    };

    try {
      await ApiClient.post('/recetas/$id/elaborar', body, token: token);
      print('DEBUG: Elaboración exitosa en backend');
    } catch (e) {
      print('DEBUG: Error en ApiClient.post elaborar: $e');
      rethrow;
    }
  }
}
