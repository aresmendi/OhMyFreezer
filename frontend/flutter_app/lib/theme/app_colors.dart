import 'package:flutter/material.dart';

/// Paleta de colores completa de OhMyFreezer.
///
/// Separada en dos grupos: [AppColorsLight] y [AppColorsDark].
/// Ninguna de las dos clases es instanciable; todos sus miembros son constantes.
///
/// Uso recomendado en widgets:
/// ```dart
/// // Acceder al color correcto según el tema activo:
/// Theme.of(context).colorScheme.primary
///
/// // Acceder a una constante directamente (evitar salvo en AppTheme):
/// AppColorsLight.primary
/// ```

// ─── LIGHT MODE ─────────────────────────────────────────────────────────────

/// Constantes de color para el tema claro.
class AppColorsLight {
  AppColorsLight._();

  /// Azul marino oscuro. Color principal de la app.
  static const Color primary         = Color(0xFF1B2A3B);

  /// Blanco puro. Texto/iconos sobre fondo primario.
  static const Color onPrimary       = Color(0xFFFFFFFF);

  /// Azul marino suavizado. Contenedor de elementos primarios.
  static const Color primaryContainer    = Color(0xFF2E4460);

  /// Blanco. Texto sobre [primaryContainer].
  static const Color onPrimaryContainer = Color(0xFFFFFFFF);

  /// Ámbar dorado. Color de acento y acciones destacadas.
  static const Color secondary       = Color(0xFFE8A838);

  /// Blanco. Texto/iconos sobre fondo secundario.
  static const Color onSecondary     = Color(0xFFFFFFFF);

  /// Ámbar claro. Contenedor de elementos secundarios.
  static const Color secondaryContainer    = Color(0xFFFFF0CC);

  /// Marrón oscuro. Texto sobre [secondaryContainer].
  static const Color onSecondaryContainer  = Color(0xFF3E2800);

  /// Rojo alerta. Usado en errores y alertas críticas de stock.
  static const Color error           = Color(0xFFE53935);

  /// Blanco. Texto/iconos sobre fondo de error.
  static const Color onError         = Color(0xFFFFFFFF);

  /// Gris claro. Fondo general de la app.
  static const Color background      = Color(0xFFF5F5F5);

  /// Negro suave. Texto sobre fondo claro.
  static const Color onBackground    = Color(0xFF1A1A1A);

  /// Blanco. Fondo de tarjetas y superficies elevadas.
  static const Color surface         = Color(0xFFFFFFFF);

  /// Negro suave. Texto sobre superficies blancas.
  static const Color onSurface      = Color(0xFF1A1A1A);

  /// Gris muy claro. Variante de superficie (chips, dividers).
  static const Color surfaceVariant  = Color(0xFFE8E8E8);

  /// Gris medio. Texto secundario sobre [surfaceVariant].
  static const Color onSurfaceVariant = Color(0xFF555555);

  /// Gris medio. Bordes y separadores.
  static const Color outline         = Color(0xFFDDDDDD);
}

// ─── DARK MODE ──────────────────────────────────────────────────────────────

/// Constantes de color para el tema oscuro.
///
/// Inspirado en la paleta dark del prototipo Kotlin, adaptada a la
/// identidad visual profesional de OhMyFreezer.
class AppColorsDark {
  AppColorsDark._();

  /// Ámbar dorado. En dark mode el acento pasa a ser el color principal
  /// para mantener contraste sobre fondos oscuros.
  static const Color primary         = Color(0xFFE8A838);

  /// Negro oscuro. Texto/iconos sobre fondo primario ámbar.
  static const Color onPrimary       = Color(0xFF1A1A1A);

  /// Ámbar oscuro. Contenedor de elementos primarios.
  static const Color primaryContainer    = Color(0xFF7A4F00);

  /// Ámbar claro. Texto sobre [primaryContainer].
  static const Color onPrimaryContainer = Color(0xFFFFDFA0);

  /// Azul marino suavizado. Secundario en dark mode.
  static const Color secondary       = Color(0xFF4A6580);

  /// Blanco. Texto/iconos sobre fondo secundario.
  static const Color onSecondary     = Color(0xFFFFFFFF);

  /// Azul marino oscuro. Contenedor secundario.
  static const Color secondaryContainer    = Color(0xFF1B2A3B);

  /// Azul claro. Texto sobre [secondaryContainer].
  static const Color onSecondaryContainer  = Color(0xFFB8D0E8);

  /// Rojo alerta (ligeramente más suave en dark para no saturar).
  static const Color error           = Color(0xFFFF6B6B);

  /// Negro. Texto/iconos sobre fondo de error.
  static const Color onError         = Color(0xFF1A1A1A);

  /// Negro profundo. Fondo general en dark mode.
  static const Color background      = Color(0xFF0D0D0D);

  /// Blanco suave. Texto sobre fondo oscuro.
  static const Color onBackground    = Color(0xFFE8E8E8);

  /// Gris muy oscuro. Fondo de tarjetas en dark mode.
  static const Color surface         = Color(0xFF1E1E1E);

  /// Blanco suave. Texto sobre superficies oscuras.
  static const Color onSurface      = Color(0xFFE8E8E8);

  /// Gris oscuro. Variante de superficie (chips, dividers).
  static const Color surfaceVariant  = Color(0xFF2C2C2E);

  /// Gris claro. Texto secundario sobre [surfaceVariant].
  static const Color onSurfaceVariant = Color(0xFFAAAAAA);

  /// Gris oscuro. Bordes y separadores en dark mode.
  static const Color outline         = Color(0xFF3A3A3A);
}