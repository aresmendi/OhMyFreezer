// lib/screens/splash_screen.dart

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import '../theme/app_assets.dart';

/// Pantalla de inicio de OhMyFreezer.
///
/// Flujo:
/// 1. Muestra la mascota [gorila_chef] con animación de entrada.
/// 2. Llama a [AuthProvider.cargarSesion] para recuperar sesión persistida.
/// 3. Navega a `/home` si hay sesión activa, o a `/login` si no la hay.
///
/// Duración mínima de 1.8 s para que la animación sea visible incluso
/// cuando [cargarSesion] resuelve muy rápido.
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _ctrl;
  late final Animation<double> _scaleAnim;
  late final Animation<double> _fadeAnim;

  @override
  void initState() {
    super.initState();

    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    );

    _scaleAnim = Tween<double>(
      begin: 0.7,
      end: 1.0,
    ).animate(CurvedAnimation(parent: _ctrl, curve: Curves.elasticOut));

    _fadeAnim = CurvedAnimation(parent: _ctrl, curve: Curves.easeIn);

    _ctrl.forward();

    WidgetsBinding.instance.addPostFrameCallback((_) {
      _init();
    });
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  Future<void> _init() async {
    // Carga de sesión + duración mínima en paralelo
    await Future.wait([
      context.read<AuthProvider>().cargarSesion(),
      Future.delayed(const Duration(milliseconds: 1800)),
    ]);

    if (!mounted) return;

    final auth = context.read<AuthProvider>();

    // Decidir a qué pantalla ir
    String nextRoute;
    if (auth.isFirstLaunch) {
      nextRoute = '/onboarding';
    } else {
      nextRoute = auth.isLoggedIn ? '/home' : '/login';
    }

    Navigator.pushReplacementNamed(context, nextRoute);
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final size = MediaQuery.sizeOf(context);

    return Scaffold(
      backgroundColor: cs.surface,
      body: Stack(
        children: [
          // ── Fondo degradado ────────────────────────────────
          Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [cs.primaryContainer.withValues(alpha: 0.5), cs.surface],
              ),
            ),
          ),

          // ── Contenido centrado ─────────────────────────────
          Center(
            child: FadeTransition(
              opacity: _fadeAnim,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Mascota gorila chef con animación de escala
                  ScaleTransition(
                    scale: _scaleAnim,
                    child: Image.asset(
                      AppAssets.gorilaChef,
                      height: size.height * 0.28,
                      fit: BoxFit.contain,
                    ),
                  ),
                  const SizedBox(height: 28),

                  // Nombre de la app
                  Text(
                    'OhMyFreezer',
                    style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: cs.onSurface,
                      letterSpacing: -0.5,
                    ),
                  ),
                  const SizedBox(height: 8),

                  // Tagline
                  Text(
                    'Gestión inteligente de cocina',
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: cs.onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 48),

                  // Indicador de carga discreto
                  SizedBox(
                    width: 28,
                    height: 28,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.5,
                      color: cs.primary,
                    ),
                  ),
                ],
              ),
            ),
          ),

          // ── Versión en footer ──────────────────────────────
          Positioned(
            bottom: 24,
            left: 0,
            right: 0,
            child: FadeTransition(
              opacity: _fadeAnim,
              child: Text(
                'v1.0.0',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: cs.onSurfaceVariant.withValues(alpha: 0.5),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
