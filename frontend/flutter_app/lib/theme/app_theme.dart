import 'package:flutter/material.dart';
import 'app_colors.dart';
import 'app_text_styles.dart';

/// Define los temas visual claro y oscuro de OhMyFreezer.
///
/// Delega los colores a [AppColorsLight] / [AppColorsDark]
/// y la tipografía a [AppTextStyles], manteniendo este fichero
/// limpio y centrado únicamente en la composición de [ThemeData].
///
/// Uso en [MaterialApp]:
/// ```dart
/// MaterialApp(
///   theme:     AppTheme.light,
///   darkTheme: AppTheme.dark,
///   themeMode: ThemeMode.system, // respeta la preferencia del SO
/// )
/// ```
class AppTheme {
  AppTheme._();

  // ─── LIGHT ──────────────────────────────────────────────────

  /// Tema claro de OhMyFreezer.
  static ThemeData get light => ThemeData(
        useMaterial3: true,
        brightness: Brightness.light,
        colorScheme: const ColorScheme(
          brightness:            Brightness.light,
          primary:               AppColorsLight.primary,
          onPrimary:             AppColorsLight.onPrimary,
          primaryContainer:      AppColorsLight.primaryContainer,
          onPrimaryContainer:    AppColorsLight.onPrimaryContainer,
          secondary:             AppColorsLight.secondary,
          onSecondary:           AppColorsLight.onSecondary,
          secondaryContainer:    AppColorsLight.secondaryContainer,
          onSecondaryContainer:  AppColorsLight.onSecondaryContainer,
          error:                 AppColorsLight.error,
          onError:               AppColorsLight.onError,
          surface:               AppColorsLight.surface,
          onSurface:             AppColorsLight.onSurface,
          surfaceContainerHighest: AppColorsLight.surfaceVariant,
          onSurfaceVariant:      AppColorsLight.onSurfaceVariant,
          outline:               AppColorsLight.outline,
        ),
        scaffoldBackgroundColor: AppColorsLight.background,
        textTheme:               AppTextStyles.textTheme,
        appBarTheme:             _appBarLight,
        elevatedButtonTheme:     _elevatedButtonLight,
        cardTheme:               _cardTheme,
        inputDecorationTheme:    _inputDecorationLight,
        navigationBarTheme:      _navigationBarTheme,
      );

  // ─── DARK ───────────────────────────────────────────────────

  /// Tema oscuro de OhMyFreezer.
  ///
  /// Se activa automáticamente cuando el SO tiene el modo oscuro habilitado
  /// (gracias a [ThemeMode.system] en [MaterialApp]).
  static ThemeData get dark => ThemeData(
        useMaterial3: true,
        brightness: Brightness.dark,
        colorScheme: const ColorScheme(
          brightness:            Brightness.dark,
          primary:               AppColorsDark.primary,
          onPrimary:             AppColorsDark.onPrimary,
          primaryContainer:      AppColorsDark.primaryContainer,
          onPrimaryContainer:    AppColorsDark.onPrimaryContainer,
          secondary:             AppColorsDark.secondary,
          onSecondary:           AppColorsDark.onSecondary,
          secondaryContainer:    AppColorsDark.secondaryContainer,
          onSecondaryContainer:  AppColorsDark.onSecondaryContainer,
          error:                 AppColorsDark.error,
          onError:               AppColorsDark.onError,
          surface:               AppColorsDark.surface,
          onSurface:             AppColorsDark.onSurface,
          surfaceContainerHighest: AppColorsDark.surfaceVariant,
          onSurfaceVariant:      AppColorsDark.onSurfaceVariant,
          outline:               AppColorsDark.outline,
        ),
        scaffoldBackgroundColor: AppColorsDark.background,
        textTheme:               AppTextStyles.textTheme,
        appBarTheme:             _appBarDark,
        elevatedButtonTheme:     _elevatedButtonDark,
        cardTheme:               _cardTheme,
        inputDecorationTheme:    _inputDecorationDark,
        navigationBarTheme:      _navigationBarTheme,
      );

  // ─── Sub-temas LIGHT ─────────────────────────────────────────

  /// AppBar clara: fondo azul marino, texto blanco.
  static const AppBarTheme _appBarLight = AppBarTheme(
    backgroundColor: AppColorsLight.primary,
    foregroundColor: AppColorsLight.onPrimary,
    elevation: 0,
    centerTitle: true,
  );

  /// Botón elevado claro: fondo azul marino, texto blanco.
  static final ElevatedButtonThemeData _elevatedButtonLight =
      ElevatedButtonThemeData(
    style: ElevatedButton.styleFrom(
      backgroundColor: AppColorsLight.primary,
      foregroundColor: AppColorsLight.onPrimary,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 24),
    ),
  );

  /// Input claro: fondo blanco, borde gris, borde primario al enfocar.
  static final InputDecorationTheme _inputDecorationLight =
      InputDecorationTheme(
    filled: true,
    fillColor: AppColorsLight.surface,
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: AppColorsLight.outline),
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: AppColorsLight.outline),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: AppColorsLight.primary, width: 2),
    ),
  );

  // ─── Sub-temas DARK ──────────────────────────────────────────

  /// AppBar oscura: fondo superficie oscura, texto blanco suave.
  static const AppBarTheme _appBarDark = AppBarTheme(
    backgroundColor: AppColorsDark.surface,
    foregroundColor: AppColorsDark.onSurface,
    elevation: 0,
    centerTitle: true,
  );

  /// Botón elevado oscuro: fondo ámbar, texto negro.
  static final ElevatedButtonThemeData _elevatedButtonDark =
      ElevatedButtonThemeData(
    style: ElevatedButton.styleFrom(
      backgroundColor: AppColorsDark.primary,
      foregroundColor: AppColorsDark.onPrimary,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      padding: const EdgeInsets.symmetric(vertical: 14, horizontal: 24),
    ),
  );

  /// Input oscuro: fondo superficie oscura, borde gris oscuro, borde ámbar al enfocar.
  static final InputDecorationTheme _inputDecorationDark =
      InputDecorationTheme(
    filled: true,
    fillColor: AppColorsDark.surface,
    border: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: AppColorsDark.outline),
    ),
    enabledBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: AppColorsDark.outline),
    ),
    focusedBorder: OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: const BorderSide(color: AppColorsDark.primary, width: 2),
    ),
  );

  // ─── Sub-temas COMPARTIDOS ───────────────────────────────────

  /// NavigationBar: quita el letterSpacing heredado de labelSmall para evitar
  /// que etiquetas largas como "Ingredientes" partan la última letra.
  static final NavigationBarThemeData _navigationBarTheme =
      NavigationBarThemeData(
    labelTextStyle: WidgetStateProperty.all(
      const TextStyle(fontSize: 11, fontWeight: FontWeight.w500, letterSpacing: 0),
    ),
  );

  /// Tarjetas: misma forma en ambos temas, el color lo pone el [ColorScheme].
  static final CardThemeData _cardTheme = CardThemeData(
    elevation: 2,
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
  );
}