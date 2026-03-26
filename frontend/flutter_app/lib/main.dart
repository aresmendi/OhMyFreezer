// lib/main.dart

import 'package:flutter/material.dart';
import 'package:flutter_app/screens/favoritos/favoritos_screen.dart';

import 'package:hive_flutter/hive_flutter.dart';
import 'package:provider/provider.dart';

import 'providers/auth_provider.dart';
import 'providers/alertas_provider.dart';
import 'providers/favoritos_provider.dart';
import 'providers/ingredientes_provider.dart';
import 'providers/recetas_provider.dart';
import 'providers/estadisticas_provider.dart';

import 'providers/usuarios_provider.dart';

import 'screens/splash_screen.dart';
import 'screens/login_screen.dart';
import 'screens/onboarding_screen.dart';
import 'screens/setup_jefe_screen.dart';
import 'screens/home_screen.dart';
import 'screens/alertas/alertas_screen.dart';
import 'screens/ingredientes/ingredientes_list_screen.dart';
import 'screens/ingredientes/ingrediente_form_screen.dart';
import 'screens/recetas/recetas_list_screen.dart';
import 'screens/recetas/receta_detail_screen.dart';
import 'screens/recetas/receta_form_screen.dart';
import 'screens/recetas/receta_pasos_screen.dart';
import 'screens/estadisticas/estadisticas_screen.dart';
import 'screens/usuarios/usuarios_list_screen.dart';
import 'screens/usuarios/usuario_form_screen.dart';

import 'theme/app_theme.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Hive.initFlutter();
  await Hive.openBox('authBox');
  runApp(const OhMyFreezerApp());
}

class OhMyFreezerApp extends StatelessWidget {
  const OhMyFreezerApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(create: (_) => AuthProvider()),
        ChangeNotifierProvider(create: (_) => UsuariosProvider()),
        ChangeNotifierProvider(create: (_) => IngredientesProvider()),
        ChangeNotifierProvider(create: (_) => RecetasProvider()),
        ChangeNotifierProvider(create: (_) => AlertasProvider()),
        ChangeNotifierProvider(create: (_) => EstadisticasProvider()),
        ChangeNotifierProvider(create: (_) => FavoritosProvider()),
      ],
      child: MaterialApp(
        title: 'OhMyFreezer',
        debugShowCheckedModeBanner: false,
        theme:     AppTheme.light,
        darkTheme: AppTheme.dark,
        themeMode: ThemeMode.system,
        initialRoute: '/',
        routes: {
          '/':             (_) => const SplashScreen(),
          '/login':        (_) => const LoginScreen(),
          '/onboarding':   (_) => const OnboardingScreen(),
          '/setup_jefe':   (_) => const SetupJefeScreen(),
          '/home':         (_) => const HomeScreen(),
          '/alertas':      (_) => const AlertasScreen(),
          '/ingredientes': (_) => const IngredientesListScreen(),
          '/ingredientes/nuevo': (_) => const IngredienteFormScreen(),
          '/recetas':      (_) => const RecetasListScreen(),
          '/recetas/nueva': (_) => const RecetaFormScreen(),
          '/recetas/detalle': (_) => const RecetaDetailScreen(),
          '/recetas/pasos':   (_) => const RecetaPasosScreen(),
          '/estadisticas': (_) => const EstadisticasScreen(),
          '/usuarios':      (_) => const UsuariosListScreen(),
          '/usuarios/nuevo':(_) => const UsuarioFormScreen(),
          '/favoritos':(_) => const FavoritosScreen(),
        },
      ),
    );
  }
}