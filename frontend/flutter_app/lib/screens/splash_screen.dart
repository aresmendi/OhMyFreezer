import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import 'login_screen.dart';

/// Pantalla de inicio que se muestra mientras la app se inicializa.
///
/// Se encarga de:
/// 1. Recuperar la sesión guardada llamando a [AuthProvider.cargarSesion]
/// 2. Redirigir al [LoginScreen] (stub temporal — en Capa 5 se añade
///    la lógica de onboarding y redirección a home si hay sesión activa)
///
/// El diseño final con la mascota gorila chef se implementa en la Capa 5.
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    _init();
  }

  /// Inicializa la app: carga sesión y navega a la pantalla correspondiente.
  Future<void> _init() async {
    await context.read<AuthProvider>().cargarSesion();
    if (!mounted) return;
    Navigator.of(context).pushReplacement(
      MaterialPageRoute(builder: (_) => const LoginScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      backgroundColor: Color(0xFF1B2A3B),
      body: Center(
        /// Indicador de carga con el color acento mientras se inicializa.
        child: CircularProgressIndicator(color: Color(0xFFE8A838)),
      ),
    );
  }
}