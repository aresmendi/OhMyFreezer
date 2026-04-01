import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/alertas_provider.dart';

/// Icono de campana con badge numérico para la AppBar.
///
/// El badge desaparece automáticamente cuando [contadorNoLeidas] == 0.
///
/// Uso en AppBar:
/// ```dart
/// actions: const [AlertaBadge()],
/// ```
class AlertaBadge extends StatelessWidget {
  final VoidCallback? onTap;

  const AlertaBadge({super.key, this.onTap});

  @override
  Widget build(BuildContext context) {
    final contador = context.watch<AlertasProvider>().contadorNoLeidas;
    final cs       = Theme.of(context).colorScheme;

    return Stack(
      alignment: Alignment.center,
      children: [
        IconButton(
          icon: const Icon(Icons.notifications_outlined),
          tooltip: 'Alertas de stock',
          onPressed: onTap,
        ),
        if (contador > 0)
          Positioned(
            top: 8,
            right: 8,
            child: IgnorePointer(
              child: Container(
                padding: const EdgeInsets.all(3),
                decoration: BoxDecoration(
                  color: cs.error,
                  shape: BoxShape.circle,
                ),
                constraints: const BoxConstraints(minWidth: 18, minHeight: 18),
                child: Text(
                  contador > 99 ? '99+' : '$contador',
                  style: TextStyle(
                    color: cs.onError,
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                  ),
                  textAlign: TextAlign.center,
                ),
              ),
            ),
          ),
      ],
    );
  }
}