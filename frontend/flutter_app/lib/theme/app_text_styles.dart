import 'package:flutter/material.dart';

/// Estilos tipográficos globales de OhMyFreezer.
///
/// Extiende el sistema de [TextTheme] de Material 3.
/// Los colores de texto NO se definen aquí; los hereda del [ColorScheme]
/// activo (light/dark) para que el cambio de tema sea automático.
///
/// Uso en widgets:
/// ```dart
/// Text('Título', style: Theme.of(context).textTheme.titleLarge)
/// ```
class AppTextStyles {
  AppTextStyles._();

  /// [TextTheme] completo listo para inyectar en [ThemeData].
  static const TextTheme textTheme = TextTheme(
    // ─── Display ──────────────────────────────────────────────
    displayLarge  : TextStyle(fontSize: 57, fontWeight: FontWeight.w300, letterSpacing: -0.25),
    displayMedium : TextStyle(fontSize: 45, fontWeight: FontWeight.w300),
    displaySmall  : TextStyle(fontSize: 36, fontWeight: FontWeight.w400),

    // ─── Headline ─────────────────────────────────────────────
    headlineLarge  : TextStyle(fontSize: 32, fontWeight: FontWeight.w700),
    headlineMedium : TextStyle(fontSize: 28, fontWeight: FontWeight.w600),
    headlineSmall  : TextStyle(fontSize: 24, fontWeight: FontWeight.w600),

    // ─── Title ────────────────────────────────────────────────
    /// Usado en AppBar y títulos de pantalla.
    titleLarge  : TextStyle(fontSize: 22, fontWeight: FontWeight.w600),
    /// Usado en cabeceras de sección y tarjetas.
    titleMedium : TextStyle(fontSize: 16, fontWeight: FontWeight.w600, letterSpacing: 0.15),
    /// Usado en subtítulos y etiquetas de campo.
    titleSmall  : TextStyle(fontSize: 14, fontWeight: FontWeight.w500, letterSpacing: 0.1),

    // ─── Body ─────────────────────────────────────────────────
    /// Texto principal de contenido (descripciones, pasos de receta).
    bodyLarge   : TextStyle(fontSize: 16, fontWeight: FontWeight.w400, letterSpacing: 0.5),
    /// Texto secundario (metadatos, fechas, cantidades).
    bodyMedium  : TextStyle(fontSize: 14, fontWeight: FontWeight.w400, letterSpacing: 0.25),
    /// Texto auxiliar (hints, captions).
    bodySmall   : TextStyle(fontSize: 12, fontWeight: FontWeight.w400, letterSpacing: 0.4),

    // ─── Label ────────────────────────────────────────────────
    /// Etiquetas de botones.
    labelLarge  : TextStyle(fontSize: 14, fontWeight: FontWeight.w600, letterSpacing: 1.25),
    /// Etiquetas de chips y badges.
    labelMedium : TextStyle(fontSize: 12, fontWeight: FontWeight.w500, letterSpacing: 0.5),
    /// Etiquetas muy pequeñas (unidades de medida, indicadores).
    labelSmall  : TextStyle(fontSize: 11, fontWeight: FontWeight.w500, letterSpacing: 0.5),
  );
}