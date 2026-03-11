import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'providers/auth_provider.dart';
import 'screens/splash_screen.dart';
import 'theme/app_theme.dart';

/// Punto de entrada de la aplicación OhMyFreezer.
///
/// Inicializa los bindings de Flutter, registra los providers globales
/// y lanza el widget raíz [OhMyFreezerApp].
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const OhMyFreezerApp());
}

/// Widget raíz de la aplicación.
///
/// Configura el árbol de providers globales con [MultiProvider]
/// y aplica el tema visual definido en [AppTheme].
class OhMyFreezerApp extends StatelessWidget {
  const OhMyFreezerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        /// Proveedor de autenticación — disponible en toda la app.
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        // Los demás providers se registran en capas posteriores
      ],
      child: MaterialApp(
        title: 'OhMyFreezer',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light,
        darkTheme: AppTheme.dark,
        themeMode: ThemeMode.system, // respeta la preferencia del SO
        home: const SplashScreen(),
      ),
    );
  }
}