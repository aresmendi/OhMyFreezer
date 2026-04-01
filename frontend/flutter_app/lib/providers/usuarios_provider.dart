import 'package:flutter/material.dart';
import '../models/usuario.dart';
import '../services/usuario_service.dart';

class UsuariosProvider extends ChangeNotifier {
  List<Usuario> _usuarios = [];
  bool _isLoading = false;
  String? _error;

  List<Usuario> get usuarios => _usuarios;
  bool get isLoading => _isLoading;
  String? get error => _error;

  Future<void> cargar(String token) async {
    _isLoading = true;
    notifyListeners();
    try {
      final data = await UsuarioService.obtenerTodos(token);
      _usuarios = data;
      _error = null;
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> registrar(
    String username,
    String password,
    bool esJefe,
    String token,
  ) async {
    _isLoading = true;
    notifyListeners();
    try {
      final nuevo = await UsuarioService.register(
        username: username,
        password: password,
        esJefeCocina: esJefe,
        token: token,
      );
      _usuarios.add(nuevo);
      _error = null;
    } catch (e) {
      _error = e.toString();
      rethrow;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> eliminar(int id, String token) async {
    try {
      await UsuarioService.eliminar(id, token);
      _usuarios.removeWhere((u) => u.id == id);
      _error = null;
      notifyListeners();
    } catch (e) {
      _error = e.toString();
      rethrow;
    }
  }
}
