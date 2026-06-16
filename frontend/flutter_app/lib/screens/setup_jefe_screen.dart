// lib/screens/setup_jefe_screen.dart

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../services/usuario_service.dart';
import '../widgets/loading_widget.dart';

class SetupJefeScreen extends StatefulWidget {
  const SetupJefeScreen({super.key});

  @override
  State<SetupJefeScreen> createState() => _SetupJefeScreenState();
}

class _SetupJefeScreenState extends State<SetupJefeScreen> {
  final _formKey = GlobalKey<FormState>();
  final _usernameCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  final _codigoCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();

  bool _passwordVisible = false;
  bool _isLoading = false;

  @override
  void dispose() {
    _usernameCtrl.dispose();
    _passwordCtrl.dispose();
    _codigoCtrl.dispose();
    _emailCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isLoading = true);

    try {
      // 1. Registro del jefe (el token no hace falta para el primer registro en el backend tal como está, pasamos un string vacío momentáneamente. NOTA: el backend espera un código en el request si esJefeCocina = true).
      // OhMyFreezer: El backend (UsuarioService.java) requiere codigoJefe en request.
      // Ocupamos pasarlo modificado de lo que UsuarioService.register de Flutter admite.

      // Así que haremos POST directamente saltándonos UsuarioService.register si este no soporta "codigoJefe".
      // Si miramos "UsuarioService.dart", register() no tiene parámetro codigoJefe.
      // Re-estructuraremos aquí el llamado para adaptarlo temporalmente,
      // o preferiblemente, modificaremos directamente UsuarioService.dart a continuación de este paso.
      // Asumiremos que hemos modificado UsuarioService.dart o llamamos directamente:

      final Map<String, dynamic> requestBody = {
        'username': _usernameCtrl.text.trim(),
        'password': _passwordCtrl.text,
        'esJefeCocina': true,
        'codigoJefe': _codigoCtrl.text.trim(),
        'email': _emailCtrl.text.trim(),
      };

      // Hacemos el request directo con el cliente base (ApiClient) para saltarnos el límite de UsuarioService:
      // import '../services/api_client.dart';
      // final response = await ApiClient.post('/usuarios/register', requestBody);

      // Simulamos la lógica correcta:
      await _registroDirectoJefe(requestBody);

      if (!mounted) return;

      // 2. Si el registro funcionó, autologin:
      final auth = context.read<AuthProvider>();

      final loginData = await UsuarioService.login(
        username: _usernameCtrl.text.trim(),
        password: _passwordCtrl.text,
      );

      final usuario = loginData['usuario'];
      final token = loginData['token'] as String;

      await auth.login(
        id: usuario.id as int,
        username: usuario.username as String,
        esJefeCocina: true,
        token: token,
        email: usuario.email as String?,
      );

      // Aseguramos que se cierra el isOnboarding
      await auth.completarOnboarding();

      if (!mounted) return;
      Navigator.pushReplacementNamed(context, '/home');
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(_mensajeError(e.toString())),
          backgroundColor: Theme.of(context).colorScheme.error,
        ),
      );
    } finally {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  // Wrapper directo porque en el UI Service actual register no lleva codigoJefe
  Future<void> _registroDirectoJefe(Map<String, dynamic> body) async {
    // Se debe importar ApiClient, ya lo importamos implicitamente por UsuarioService pero por si a caso.
    // Se puede usar la api directamente invocando la lógica que pondremos en un minuto en UsuarioService.
    await UsuarioService.registerJefeModificado(body);
  }

  String _mensajeError(String raw) {
    if (raw.contains('inválido') || raw.contains('invalido') || raw.contains('código')) {
      return 'El código de Jefe de Cocina es incorrecto.';
    }
    if (raw.contains('existe') || raw.contains('duplicado') || raw.contains('username')) {
      return 'El nombre de usuario ya está registrado. Elije otro.';
    }
    if (raw.contains('correo') || raw.contains('email')) {
      return 'El correo electrónico no es válido o ya está en uso.';
    }
    if (raw.contains('400') || raw.contains('Datos incorrectos')) {
      return 'Los datos del formulario no son válidos. Revisalos e intentalo de nuevo.';
    }
    if (raw.contains('500') || raw.contains('interno')) {
      return 'Error en el servidor. Inténtalo más tarde.';
    }
    if (raw.contains('SocketException') || raw.contains('connection') || raw.contains('Sin conexión')) {
      return 'Problema de red. No se puede conectar al servidor.';
    }
    if (raw.contains('TimeoutException') || raw.contains('timed out')) {
      return 'El servidor tardó demasiado en responder. Inténtalo de nuevo.';
    }
    return 'Error en el registro: $raw';
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text('Configurar Jefe de Cocina')),
      body: Stack(
        children: [
          SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(24.0),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Text(
                      'Únete a OhMyFreezer',
                      style: Theme.of(context).textTheme.headlineMedium
                          ?.copyWith(
                            fontWeight: FontWeight.bold,
                            color: cs.onSurface,
                          ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Crea tu cuenta administradora para empezar a gestionar la cocina. Necesitarás el código de seguridad de tu restaurante.',
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: cs.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 32),

                    TextFormField(
                      controller: _usernameCtrl,
                      decoration: const InputDecoration(
                        labelText: 'Usuario',
                        prefixIcon: Icon(Icons.person),
                      ),
                      validator: (v) => v!.isEmpty ? 'Requerido' : null,
                    ),
                    const SizedBox(height: 16),

                    TextFormField(
                      controller: _passwordCtrl,
                      obscureText: !_passwordVisible,
                      decoration: InputDecoration(
                        labelText: 'Contraseña',
                        prefixIcon: const Icon(Icons.lock),
                        suffixIcon: IconButton(
                          icon: Icon(
                            _passwordVisible
                                ? Icons.visibility_off
                                : Icons.visibility,
                          ),
                          onPressed: () => setState(
                            () => _passwordVisible = !_passwordVisible,
                          ),
                        ),
                      ),
                      validator: (v) =>
                          v!.length < 4 ? 'Mínimo 4 caracteres' : null,
                    ),
                    const SizedBox(height: 16),

                    TextFormField(
                      controller: _codigoCtrl,
                      decoration: const InputDecoration(
                        labelText: 'Código Jefe de Cocina',
                        prefixIcon: Icon(Icons.admin_panel_settings),
                      ),
                      validator: (v) =>
                          v!.isEmpty ? 'Se requiere el código' : null,
                    ),
                    const SizedBox(height: 16),

                    TextFormField(
                      controller: _emailCtrl,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(
                        labelText: 'Correo electrónico',
                        prefixIcon: Icon(Icons.email_outlined),
                        helperText: 'Obligatorio para recibir alertas de stock',
                      ),
                      validator: (v) {
                        if (v == null || v.isEmpty) {
                          return 'El correo es obligatorio';
                        }
                        if (!v.contains('@') || !v.contains('.')) {
                          return 'Correo inválido';
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: 32),

                    FilledButton(
                      onPressed: _isLoading ? null : _submit,
                      style: FilledButton.styleFrom(
                        padding: const EdgeInsets.symmetric(vertical: 16),
                      ),
                      child: const Text(
                        'Crear cuenta y Empezar',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),

          if (_isLoading)
            const Positioned.fill(
              child: LoadingWidget.overlay(mensaje: 'Registrando...'),
            ),
        ],
      ),
    );
  }
}
