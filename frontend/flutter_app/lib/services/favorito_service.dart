import '../models/receta_favorita.dart';
import 'api_client.dart';

/// Servicio HTTP para la gestión de recetas favoritas.
class FavoritoService {
  /// POST /api/favoritos — Marcar una receta como favorita.
  static Future<RecetaFavorita> marcarFavorito({
    required int usuarioId,
    required int recetaId,
    required String token,
  }) async {
    final body = {
      'usuarioId': usuarioId,
      'recetaId': recetaId,
    };
    
    final data = await ApiClient.post('/favoritos', body, token: token);
    return RecetaFavorita.fromJson(data as Map<String, dynamic>);
  }

  /// DELETE /api/favoritos — Desmarcar una receta como favorita.
  static Future<void> desmarcarFavorito({
    required int usuarioId,
    required int recetaId,
    required String token,
  }) async {
    final body = {
      'usuarioId': usuarioId,
      'recetaId': recetaId,
    };
    
    await ApiClient.delete('/favoritos', token: token, body: body);
  }

  /// GET /api/favoritos/usuario/{usuarioId} — Obtener favoritos de un usuario.
  static Future<List<RecetaFavorita>> obtenerFavoritos({
    required int usuarioId,
    required String token,
  }) async {
    final data = await ApiClient.get('/favoritos/usuario/$usuarioId', token: token);
    return (data as List)
        .map((e) => RecetaFavorita.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// GET /api/favoritos/verificar — Verificar si una receta es favorita.
  static Future<bool> verificarFavorito({
    required int usuarioId,
    required int recetaId,
    required String token,
  }) async {
    final data = await ApiClient.get(
      '/favoritos/verificar?usuarioId=$usuarioId&recetaId=$recetaId',
      token: token,
    );
    return data['esFavorita'] as bool;
  }

  /// GET /api/favoritos/usuario/{usuarioId}/ids — IDs de favoritos.
  static Future<List<int>> obtenerIdsFavoritos({
    required int usuarioId,
    required String token,
  }) async {
    final data = await ApiClient.get('/favoritos/usuario/$usuarioId/ids', token: token);
    return (data as List).map((e) => e as int).toList();
  }

  /// Alternar favorito: marca si no lo está, desmarca si lo está.
  static Future<bool> toggleFavorito({
    required int usuarioId,
    required int recetaId,
    required bool esFavoritaActualmente,
    required String token,
  }) async {
    if (esFavoritaActualmente) {
      await desmarcarFavorito(
        usuarioId: usuarioId,
        recetaId: recetaId,
        token: token,
      );
      return false;
    } else {
      await marcarFavorito(
        usuarioId: usuarioId,
        recetaId: recetaId,
        token: token,
      );
      return true;
    }
  }
}