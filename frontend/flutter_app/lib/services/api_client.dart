import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;

/// Cliente HTTP centralizado para toda la aplicación OhMyFreezer.
///
/// Todas las llamadas al backend pasan por esta clase, que se encarga de:
/// - Añadir los headers comunes (`Content-Type`, `Accept`)
/// - Inyectar el token JWT en la cabecera `Authorization` si se proporciona
/// - Gestionar los timeouts
/// - Decodificar las respuestas JSON
/// - Traducir los códigos de error HTTP en excepciones legibles
///
/// Ejemplo de uso desde un service:
/// ```dart
/// final data = await ApiClient.get('/ingredientes, token: auth.token');
/// ```
class ApiClient {
  /// URL base del backend Spring Boot.
  ///
  /// Selecciona 10.0.2.2 en Android y localhost en otros (Windows, Web, etc)
  static String get baseUrl {
    try {
      if (Platform.isAndroid) return 'http://10.0.2.2:8080/api';
    } catch (_) {}
    return 'http://localhost:8080/api';
  }

  /// Construye los headers para cada petición.
  /// Si se proporciona [token], añade la cabecera `Authorization: Bearer <token>`.
  static Map<String, String> _buildHeaders({String? token}) {
    final headers = {
      'Content-Type': 'application/json',
      'Accept': 'application/json',
    };
    if (token != null) {
      headers['Authorization'] = 'Bearer $token';
    }
    return headers;
  }

  // ─── GET ────────────────────────────────────────────────────

  /// Realiza una petición GET al [endpoint] indicado.
  ///
  /// Devuelve el cuerpo de la respuesta decodificado como `dynamic`
  /// (normalmente un `Map` o una `List`).
  ///
  /// Lanza [Exception] si no hay conexión o el servidor devuelve error.
  static Future<dynamic> get(String endpoint, {String? token}) async {
    try {
      final response = await http
          .get(Uri.parse('$baseUrl$endpoint'), headers: _buildHeaders(token: token))
          .timeout(const Duration(seconds: 10));
      return _handleResponse(response);
    } on SocketException {
      throw Exception('Sin conexión con el servidor');
    } on HttpException {
      throw Exception('Error de red');
    }
  }

  // ─── POST ───────────────────────────────────────────────────

  /// Realiza una petición POST al [endpoint] con el [body] serializado a JSON.
  ///
  /// Devuelve el cuerpo de la respuesta decodificado, o `null` si está vacío.
  ///
  /// Lanza [Exception] si no hay conexión o el servidor devuelve error.
  static Future<dynamic> post(String endpoint, Map<String, dynamic> body, {String? token}) async {
    try {
      final response = await http
          .post(
            Uri.parse('$baseUrl$endpoint'),
            headers:_buildHeaders(token: token),
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 10));
      return _handleResponse(response);
    } on SocketException {
      throw Exception('Sin conexión con el servidor');
    }
  }

  // ─── PUT ────────────────────────────────────────────────────

  /// Realiza una petición PUT al [endpoint] con el [body] serializado a JSON.
  ///
  /// Usado para actualizaciones completas de un recurso.
  ///
  /// Lanza [Exception] si no hay conexión o el servidor devuelve error.
  static Future<dynamic> put(String endpoint, Map<String, dynamic> body,{String? token}) async {
    try {
      final response = await http
          .put(
            Uri.parse('$baseUrl$endpoint'),
            headers: _buildHeaders(token: token),
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 10));
      return _handleResponse(response);
    } on SocketException {
      throw Exception('Sin conexión con el servidor');
    }
  }

  // ─── PATCH ──────────────────────────────────────────────────

  /// Realiza una petición PATCH al [endpoint] con el [body] serializado a JSON.
  ///
  /// Usado para actualizaciones parciales de un recurso (ej. marcar alerta como leída).
  ///
  /// Lanza [Exception] si no hay conexión o el servidor devuelve error.
  static Future<dynamic> patch(String endpoint, Map<String, dynamic> body, {String? token}) async {
    try {
      final response = await http
          .patch(
            Uri.parse('$baseUrl$endpoint'),
            headers: _buildHeaders(token: token),
            body: jsonEncode(body),
          )
          .timeout(const Duration(seconds: 10));
      return _handleResponse(response);
    } on SocketException {
      throw Exception('Sin conexión con el servidor');
    }
  }

  // ─── DELETE ─────────────────────────────────────────────────

  /// Realiza una petición DELETE al [endpoint] indicado.
  ///
  /// No devuelve cuerpo. Lanza [Exception] si el servidor responde
  /// con un código distinto de 200 o 204.
  static Future<void> delete(String endpoint, {String? token}) async {
    try {
      final response = await http
          .delete(Uri.parse('$baseUrl$endpoint'), headers: _buildHeaders(token: token))
          .timeout(const Duration(seconds: 10));
      if (response.statusCode != 200 && response.statusCode != 204) {
        throw Exception('Error ${response.statusCode}: ${response.body}');
      }
    } on SocketException {
      throw Exception('Sin conexión con el servidor');
    }
  }

  // ─── HANDLER ────────────────────────────────────────────────

  /// Interpreta la [response] HTTP y devuelve el cuerpo decodificado.
  ///
  /// - 2xx → devuelve el JSON decodificado (o `null` si el cuerpo está vacío)
  /// - 400 → lanza excepción con el mensaje del servidor
  /// - 404 → lanza excepción "Recurso no encontrado"
  /// - 500 → lanza excepción "Error interno del servidor"
  /// - Otros → lanza excepción con el código y cuerpo raw
  static dynamic _handleResponse(http.Response response) {
    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (response.body.isEmpty) return null;
      return jsonDecode(utf8.decode(response.bodyBytes));
    }else if (response.statusCode == 401) {
      throw Exception('No autorizado: sesión expirada o token inválido');
    } else if (response.statusCode == 403) {
      throw Exception('Acceso denegado: no tienes permisos suficientes');
    } else if (response.statusCode == 404) {
      throw Exception('Recurso no encontrado');
    } else if (response.statusCode == 400) {
      throw Exception('Datos incorrectos: ${response.body}');
    } else if (response.statusCode == 500) {
      throw Exception('Error interno del servidor');
    } else {
      throw Exception('Error ${response.statusCode}: ${response.body}');
    }
  }
}