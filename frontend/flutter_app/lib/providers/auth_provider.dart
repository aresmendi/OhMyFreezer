import 'package:flutter/material.dart';
import 'package:hive/hive.dart';

/// Proveedor de estado que gestiona la sesión del usuario autenticado.
///
/// Implementa [ChangeNotifier] para notificar a los widgets cuando
/// el estado de autenticación cambia (login, logout, carga de sesión).
///
/// La sesión se persiste en [Hive] para que el usuario
/// no tenga que volver a loguearse al cerrar y reabrir la app.
/// El token JWT también se persiste para enviarlo en cada petición.
///
/// Ejemplo de uso en un widget:
/// ```dart
/// final auth = context.read<AuthProvider>();
/// if (auth.esJefeCocina) { ... }
/// ```
class AuthProvider extends ChangeNotifier {
  /// ID del usuario autenticado. `null` si no hay sesión activa.
  int? _usuarioId;

  /// Token JWT recibido del backend tras el login.
  /// Se incluye en la cabecera `Authorization` de cada petición.
  String? _token;

  /// Nombre de usuario del usuario autenticado.
  String? _username;

  /// `true` si el usuario autenticado tiene rol de jefe de cocina.
  bool _esJefeCocina = false;

  /// `true` mientras se está procesando una operación asíncrona (login, logout).
  bool _isLoading = false;

  /// Variable que indica si es la primera vez que se lanza la app.
  bool _isFirstLaunch = true;

  final box = Hive.box('authBox'); // 👈 acceso a Hive

  // ─── Getters públicos ────────────────────────────────────────

  /// `true` si es el primer inicio de la app.
  bool get isFirstLaunch => _isFirstLaunch;

  /// ID del usuario autenticado. `null` si no hay sesión activa.
  int? get usuarioId => _usuarioId;

  /// Nombre de usuario del usuario autenticado.
  String? get username => _username;

  /// `true` si el usuario autenticado tiene rol de jefe de cocina.
  ///
  /// Usado para mostrar u ocultar funcionalidades exclusivas del jefe:
  /// crear recetas, ver estadísticas, recibir alertas de stock.
  bool get esJefeCocina => _esJefeCocina;

  /// `true` si hay un usuario con sesión activa.
  bool get isLoggedIn => _usuarioId != null;

  /// `true` mientras se procesa una operación asíncrona.
  ///
  /// Útil para mostrar indicadores de carga en los formularios de login.
  bool get isLoading => _isLoading;

  /// Token JWT activo. `null` si no hay sesión.
  /// Usado por [ApiClient] para construir la cabecera Authorization.
  String? get token => _token;

  // ─── Sesión persistente ──────────────────────────────────────

  /// Recupera la sesión guardada en [Hive] al arrancar la app.
  /// Si existe una sesión previa, restaura [usuarioId], [token] [username] y
  /// [esJefeCocina] sin necesidad de volver a hacer login.
  /// Llamar desde [SplashScreen] antes de decidir la pantalla inicial.
  Future<void> cargarSesion() async {
    _isFirstLaunch = box.get('isFirstLaunch',defaultValue: true);

    final id = box.get('usuarioId');
    if (id != null){
      _usuarioId = id;
      _username = box.get('username');
      _esJefeCocina = box.get('esJefeCocina',defaultValue: false);
      _token = box.get('token');
    }
    notifyListeners();
  }

  /// Limpia la bandera de "primer inicio", útil tras pasar por Onboarding o Login Exitoso.
  Future<void> completarOnboarding() async {
    _isFirstLaunch = false;
    await box.put('isFirstLaunch', false);
    notifyListeners();
  }

  // ─── Login ───────────────────────────────────────────────────

  /// Inicia sesión con los datos recibidos del backend tras autenticación exitosa.
  /// 
  /// Guarda los datos en memoria y los persiste en [Hive].
  /// Notifica a todos los widgets suscritos para que se reconstruyan.
  ///
  /// Parámetros:
  /// - [id]: ID del usuario en la base de datos
  /// - [username]: nombre de usuario
  /// - [esJefeCocina]: `true` si el usuario tiene rol de jefe de cocina
  Future<void> login({
    required int id,
    required String username,
    required bool esJefeCocina,
    required String token,
  }) async {
    _usuarioId = id;
    _username = username;
    _esJefeCocina = esJefeCocina;
    _token = token;

    // Asegurarse de que marcamos como false si el usuario se loguea de todas formas
    _isFirstLaunch = false;

    
    await box.put('usuarioId', id);
    await box.put('username', username);
    await box.put('esJefeCocina', esJefeCocina);
    await box.put('token',token);
    await box.put('isFirstLaunch', false);

    notifyListeners();
  }

  // ─── Logout ──────────────────────────────────────────────────

  /// Cierra la sesión del usuario actual.
  ///
  /// Limpia los datos en memoria y borra todas las claves de [Hive].
  /// OJO: isFirstLaunch no se borra, ya que la app ya se inició antes.
  /// Notifica a los widgets para que redirijan al login.
  Future<void> logout() async {
    _usuarioId = null;
    _username = null;
    _esJefeCocina = false;
    _token = null;

    await box.delete('usuarioId');
    await box.delete('username');
    await box.delete('esJefeCocina');
    await box.delete('token');
    // No eliminamos isFirstLaunch a propósito.

    notifyListeners();
  }

  // ─── Loading ─────────────────────────────────────────────────

  /// Actualiza el estado de carga y notifica a los widgets suscritos.
  ///
  /// Usar [setLoading(true)] antes de una operación asíncrona y
  /// [setLoading(false)] al terminar, tanto en éxito como en error.
  void setLoading(bool value) {
    _isLoading = value;
    notifyListeners();
  }
}