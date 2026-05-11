// lib/screens/login_screen.dart

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../services/usuario_service.dart';
import '../widgets/loading_widget.dart';

/// Pantalla de login de OhMyFreezer.
///
/// Flujo:
/// 1. El usuario introduce username + contraseña.
/// 2. Se llama a [UsuarioService.login].
/// 3. Si OK → [AuthProvider.login] guarda la sesión y navega a `/home`.
/// 4. Si error → SnackBar con el mensaje del backend.
///
/// La pantalla es accesible sin autenticación previa.
/// Si ya hay sesión activa, [SplashScreen] redirige directamente a `/home`.
class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen>
    with SingleTickerProviderStateMixin {
  // ─── Form ────────────────────────────────────────────────────
  final _formKey = GlobalKey<FormState>();
  final _usernameCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  bool _passwordVisible = false;

  // ─── Animación de entrada ────────────────────────────────────
  late final AnimationController _animCtrl;
  late final Animation<double> _fadeAnim;
  late final Animation<Offset> _slideAnim;

  @override
  void initState() {
    super.initState();
    _animCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 600),
    );
    _fadeAnim = CurvedAnimation(parent: _animCtrl, curve: Curves.easeOut);
    _slideAnim = Tween<Offset>(
      begin: const Offset(0, 0.08),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: _animCtrl, curve: Curves.easeOut));

    _animCtrl.forward();
  }

  @override
  void dispose() {
    _animCtrl.dispose();
    _usernameCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  // ─── Lógica de login ─────────────────────────────────────────
  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    final auth = context.read<AuthProvider>();
    auth.setLoading(true);

    try {
      final result = await UsuarioService.login(
        username: _usernameCtrl.text.trim(),
        password: _passwordCtrl.text,
      );

      final usuario = result['usuario'];
      final token = result['token'] as String;

      await auth.login(
        id: usuario.id as int,
        username: usuario.username as String,
        esJefeCocina: usuario.esJefeCocina as bool,
        token: token,
        email: usuario.email as String?,
      );

      if (!mounted) return;
      Navigator.pushReplacementNamed(context, '/home');
    } catch (e) {
      if (!mounted) return;
      _mostrarError(_mensajeError(e.toString()));
    } finally {
      if (mounted) auth.setLoading(false);
    }
  }

  /// Convierte el mensaje de excepción en texto legible para el usuario.
  String _mensajeError(String raw) {
    if (raw.contains('401') || raw.contains('credenciales') || raw.contains('No autorizado')) {
      return 'Usuario o contraseña incorrectos.';
    }
    if (raw.contains('403') || raw.contains('Acceso denegado')) {
      return 'Tu cuenta no tiene permisos para acceder.';
    }
    if (raw.contains('404') || raw.contains('no encontrado')) {
      return 'El usuario no existe.';
    }
    if (raw.contains('400') || raw.contains('Datos incorrectos')) {
      return 'Los datos introducidos no son válidos.';
    }
    if (raw.contains('500') || raw.contains('interno')) {
      return 'Error en el servidor. Inténtalo más tarde.';
    }
    if (raw.contains('SocketException') || raw.contains('connection') || raw.contains('Sin conexión')) {
      return 'No se puede conectar con el servidor.\nComprueba tu red.';
    }
    if (raw.contains('TimeoutException') || raw.contains('timed out')) {
      return 'El servidor tardó demasiado en responder. Inténtalo de nuevo.';
    }
    return 'Error inesperado: $raw';
  }

  void _mostrarError(String mensaje) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(mensaje),
        backgroundColor: Theme.of(context).colorScheme.error,
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(16),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    );
  }

  // ─── UI ──────────────────────────────────────────────────────
  @override
  Widget build(BuildContext context) {
    final isLoading = context.watch<AuthProvider>().isLoading;
    final cs = Theme.of(context).colorScheme;
    final size = MediaQuery.sizeOf(context);

    return Scaffold(
      body: Stack(
        children: [
          // ── Fondo degradado ──────────────────────────────────
          _Fondo(cs: cs),

          // ── Contenido scrollable ─────────────────────────────
          SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 28),
                child: ConstrainedBox(
                  constraints: BoxConstraints(
                    minHeight: size.height * 0.75,
                    maxWidth: 480,
                  ),
                  child: FadeTransition(
                    opacity: _fadeAnim,
                    child: SlideTransition(
                      position: _slideAnim,
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const SizedBox(height: 48),
                          _Logo(cs: cs),
                          const SizedBox(height: 40),
                          _FormCard(
                            formKey: _formKey,
                            usernameCtrl: _usernameCtrl,
                            passwordCtrl: _passwordCtrl,
                            passwordVisible: _passwordVisible,
                            onTogglePassword: () => setState(
                              () => _passwordVisible = !_passwordVisible,
                            ),
                            onSubmit: isLoading ? null : _submit,
                            isLoading: isLoading,
                            cs: cs,
                          ),
                          const SizedBox(height: 32),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),

          // ── Overlay de carga ─────────────────────────────────
          if (isLoading)
            const Positioned.fill(
              child: LoadingWidget.overlay(mensaje: 'Iniciando sesión…'),
            ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Subwidgets privados
// ─────────────────────────────────────────────────────────────────────────────

/// Degradado de fondo que usa los colores del tema.
class _Fondo extends StatelessWidget {
  final ColorScheme cs;
  const _Fondo({required this.cs});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            cs.primaryContainer.withOpacity(0.6),
            cs.surface,
            cs.secondaryContainer.withOpacity(0.3),
          ],
        ),
      ),
    );
  }
}

/// Logo + nombre de la app + tagline.
class _Logo extends StatelessWidget {
  final ColorScheme cs;
  const _Logo({required this.cs});

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;

    return Column(
      children: [
        // Icono principal 
        Container(
          width: 90,
          height: 90,
          decoration: BoxDecoration(
            color: cs.primaryContainer,
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: cs.primary.withOpacity(0.25),
                blurRadius: 20,
                offset: const Offset(0, 8),
              ),
            ],
          ),
          child: Icon(
            Icons.kitchen_rounded,
            size: 48,
            color: cs.onPrimaryContainer,
          ),
        ),
        const SizedBox(height: 20),
        Text(
          'OhMyFreezer',
          style: tt.headlineMedium?.copyWith(
            fontWeight: FontWeight.w800,
            color: cs.onSurface,
            letterSpacing: -0.5,
          ),
        ),
        const SizedBox(height: 6),
        Text(
          'Gestión inteligente de cocina',
          style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
        ),
      ],
    );
  }
}

/// Tarjeta con el formulario de login.
class _FormCard extends StatelessWidget {
  final GlobalKey<FormState> formKey;
  final TextEditingController usernameCtrl;
  final TextEditingController passwordCtrl;
  final bool passwordVisible;
  final VoidCallback onTogglePassword;
  final VoidCallback? onSubmit;
  final bool isLoading;
  final ColorScheme cs;

  const _FormCard({
    required this.formKey,
    required this.usernameCtrl,
    required this.passwordCtrl,
    required this.passwordVisible,
    required this.onTogglePassword,
    required this.onSubmit,
    required this.isLoading,
    required this.cs,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
        side: BorderSide(color: cs.outlineVariant.withOpacity(0.5)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Form(
          key: formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                'Iniciar sesión',
                style: Theme.of(
                  context,
                ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 24),

              // ── Campo usuario ──────────────────────────────
              TextFormField(
                controller: usernameCtrl,
                textInputAction: TextInputAction.next,
                keyboardType: TextInputType.text,
                autocorrect: false,
                decoration: const InputDecoration(
                  labelText: 'Usuario',
                  prefixIcon: Icon(Icons.person_outline_rounded),
                ),
                validator: (v) {
                  if (v == null || v.trim().isEmpty) {
                    return 'Introduce tu nombre de usuario';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 16),

              // ── Campo contraseña ───────────────────────────
              TextFormField(
                controller: passwordCtrl,
                obscureText: !passwordVisible,
                textInputAction: TextInputAction.done,
                onFieldSubmitted: (_) => onSubmit?.call(),
                decoration: InputDecoration(
                  labelText: 'Contraseña',
                  prefixIcon: const Icon(Icons.lock_outline_rounded),
                  suffixIcon: IconButton(
                    icon: Icon(
                      passwordVisible
                          ? Icons.visibility_off_outlined
                          : Icons.visibility_outlined,
                    ),
                    onPressed: onTogglePassword,
                    tooltip: passwordVisible ? 'Ocultar' : 'Mostrar',
                  ),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) {
                    return 'Introduce tu contraseña';
                  }
                  if (v.length < 4) {
                    return 'Mínimo 4 caracteres';
                  }
                  return null;
                },
              ),
              const SizedBox(height: 28),

              // ── Botón login ────────────────────────────────
              FilledButton(
                onPressed: onSubmit,
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(52),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(14),
                  ),
                ),
                child: isLoading
                    ? SizedBox(
                        height: 22,
                        width: 22,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.5,
                          color: cs.onPrimary,
                        ),
                      )
                    : const Text(
                        'Entrar',
                        style: TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
              ),
              const SizedBox(height: 16),

              // ── Link a registro ────────────────────────────
              Wrap(
                alignment: WrapAlignment.center,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  Text(
                    '¿Eres jefe de cocina y no tienes cuenta?',
                    style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13),
                  ),
                  TextButton(
                    onPressed: () => Navigator.pushNamed(context, '/setup_jefe'),
                    style: TextButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 6),
                      minimumSize: Size.zero,
                      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    ),
                    child: const Text('Registrarse', style: TextStyle(fontSize: 13)),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
