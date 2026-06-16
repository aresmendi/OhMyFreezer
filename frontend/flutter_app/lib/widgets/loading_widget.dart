import 'package:flutter/material.dart';

/// Widget de carga genérico reutilizable en cualquier pantalla.
///
/// Modos de uso:
/// - [LoadingWidget.fullScreen()] → ocupa toda la pantalla (Scaffold body).
/// - [LoadingWidget.inline()]     → ocupa solo el espacio disponible (Center).
/// - [LoadingWidget.overlay()]    → superpuesto sobre otro widget con fondo semitransparente.
///
/// Ejemplo:
/// ```dart
/// if (isLoading) return const LoadingWidget.fullScreen();
/// ```
class LoadingWidget extends StatelessWidget {
  final String? mensaje;
  final _Modo _modo;

  const LoadingWidget.fullScreen({super.key, this.mensaje})
      : _modo = _Modo.fullScreen;

  const LoadingWidget.inline({super.key, this.mensaje})
      : _modo = _Modo.inline;

  const LoadingWidget.overlay({super.key, this.mensaje})
      : _modo = _Modo.overlay;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    final contenido = Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        CircularProgressIndicator(color: cs.primary),
        if (mensaje != null) ...[
          const SizedBox(height: 16),
          Text(
            mensaje!,
            style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
            textAlign: TextAlign.center,
          ),
        ],
      ],
    );

    switch (_modo) {
      case _Modo.fullScreen:
        return Scaffold(
          body: Center(child: contenido),
        );

      case _Modo.inline:
        return Center(child: contenido);

      case _Modo.overlay:
        return ColoredBox(
          color: Colors.black.withValues(alpha: 0.35),
          child: Center(child: contenido),
        );
    }
  }
}

enum _Modo { fullScreen, inline, overlay }