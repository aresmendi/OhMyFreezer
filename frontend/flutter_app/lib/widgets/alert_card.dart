import 'package:flutter/material.dart';
import '../models/alerta.dart';

/// Tarjeta que muestra una [Alerta] de stock en el panel del jefe de cocina.
///
/// - Alertas no leídas → fondo ligeramente coloreado + borde de acento.
/// - [onMarcarLeida] → acción para marcar como leída.
/// - [onTap] → opcional, para navegar al ingrediente afectado.
class AlertaCard extends StatelessWidget {
  final Alerta alerta;
  final VoidCallback? onTap;
  final VoidCallback? onMarcarLeida;

  const AlertaCard({
    super.key,
    required this.alerta,
    this.onTap,
    this.onMarcarLeida,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final esGrave =
        alerta.tipo == TipoAlerta.stockAgotado ||
        alerta.tipo == TipoAlerta.escaldaio;
    final color = esGrave ? cs.error : cs.secondary;
    final noLeida = !alerta.leida;

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: noLeida
            ? BorderSide(color: color.withValues(alpha: 0.6), width: 1.5)
            : BorderSide.none,
      ),
      color: noLeida
          ? color.withValues(alpha: 0.06)
          : Theme.of(context).cardTheme.color,
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // ── Icono tipo alerta ────────────────────────────
              Padding(
                padding: const EdgeInsets.only(top: 2),
                child: Icon(
                  esGrave
                      ? Icons.error_outline_rounded
                      : Icons.warning_amber_rounded,
                  color: color,
                  size: 26,
                ),
              ),
              const SizedBox(width: 12),

              // ── Contenido ────────────────────────────────────
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      alerta.ingredienteNombre,
                      style: tt.titleSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: noLeida ? color : null,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      alerta.mensaje,
                      style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      _formatFecha(alerta.fechaCreacion),
                      style: tt.labelSmall?.copyWith(
                        color: cs.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),

              // ── Acción marcar leída ──────────────────────────
              if (noLeida && onMarcarLeida != null)
                IconButton(
                  icon: Icon(Icons.done_rounded, color: cs.primary, size: 20),
                  tooltip: 'Marcar como leída',
                  onPressed: onMarcarLeida,
                ),
            ],
          ),
        ),
      ),
    );
  }

  /// Formatea ISO-8601 a "dd/MM/yyyy HH:mm".
  String _formatFecha(String iso) {
    try {
      final dt = DateTime.parse(iso).toLocal();
      final d = dt.day.toString().padLeft(2, '0');
      final mo = dt.month.toString().padLeft(2, '0');
      final h = dt.hour.toString().padLeft(2, '0');
      final mi = dt.minute.toString().padLeft(2, '0');
      return '$d/$mo/${dt.year}  $h:$mi';
    } catch (_) {
      return iso;
    }
  }
}
