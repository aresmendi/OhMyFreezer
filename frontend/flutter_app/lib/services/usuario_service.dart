// lib/services/usuario_service.dart

import '../models/usuario.dart';
import 'api_client.dart';

/// Servicio HTTP para autenticación y gestión de usuarios.
///
/// El backend devuelve un objeto con el token JWT y los datos del usuario
/// en el mismo response de login/register.
class UsuarioService {
  /// POST /api/usuarios/login
  ///
  /// Devuelve un [Map] con:
  /// - `'token'`       → String JWT
  /// - `'usuario'`     → [Usuario] deserializado
  static Future<Map<String, dynamic>> login({
    required String username,
    required String password,
  }) async {
    final data = await ApiClient.post('/usuarios/login', {
      'username': username,
      'password': password,
    });
    return {
      'token': data['token'] as String,
      'usuario': Usuario(
        id: data['id'] as int,
        username: data['username'] as String,
        esJefeCocina: data['esJefeCocina'] as bool,
        fechaRegistro: '',
      ),
    };
  }

  /// POST /api/usuarios/register
  ///
  /// Solo el jefe de cocina puede registrar nuevos usuarios (controlado en backend).
  /// Devuelve el [Usuario] creado.
  static Future<Usuario> register({
    required String username,
    required String password,
    required bool esJefeCocina,
    required String token,
  }) async {
    final data = await ApiClient.post('/usuarios/register', {
      'username': username,
      'password': password,
      'esJefeCocina': esJefeCocina,
    }, token: token);
    return Usuario.fromJson(data as Map<String, dynamic>);
  }

  /// POST /api/usuarios/register (Primer Inicio)
  ///
  /// Método específico para registrar la cuenta administradora
  /// Incluye `codigoJefe`, que es requerido por el backend.
  static Future<Usuario> registerJefeModificado(
    Map<String, dynamic> body,
  ) async {
    final data = await ApiClient.post(
      '/usuarios/register',
      body,
      // No mandamos token porque aún no hay sesión
    );
    return Usuario.fromJson(data as Map<String, dynamic>);
  }

  /// GET /api/usuarios — perfil del usuario autenticado.
  static Future<Usuario> getById(int id, String token) async {
    final data = await ApiClient.get('/usuarios/$id', token: token);
    return Usuario.fromJson(data as Map<String, dynamic>);
  }

  /// GET /api/usuarios — listado de personal (solo para jefe de cocina).
  static Future<List<Usuario>> obtenerTodos(String token) async {
    final data = await ApiClient.get('/usuarios', token: token);
    return (data as List)
        .map((e) => Usuario.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// DELETE /api/usuarios/{id} — eliminar un empleado (solo jefe de cocina).
  static Future<void> eliminar(int id, String token) async {
    await ApiClient.delete('/usuarios/$id', token: token);
  }
}
