// lib/screens/estadisticas/estadisticas_screen.dart

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../models/estadistica.dart';
import '../../providers/auth_provider.dart';
import '../../providers/estadisticas_provider.dart';
import '../../widgets/loading_widget.dart';
import '../home_screen.dart'; // EmptyState

/// Pantalla de estadísticas de elaboración. Solo jefe de cocina.
///
/// Muestra:
/// - Tarjeta resumen: total elaboraciones + receta más elaborada.
/// - Lista de recetas con barra de progreso de tasa de completado.
/// - Pull-to-refresh.
class EstadisticasScreen extends StatelessWidget {
  const EstadisticasScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<EstadisticasProvider>();
    final auth     = context.read<AuthProvider>();

    if (provider.isLoading && provider.estadisticas.isEmpty) {
      return const LoadingWidget.inline(mensaje: 'Cargando estadísticas…');
    }

    if (provider.estadisticas.isEmpty) {
      return EmptyState(
        titulo:    'Sin estadísticas',
        subtitulo: 'Aún no se ha elaborado ninguna receta.',
        onRecargar: () => provider.cargar(auth.token!),
      );
    }

    final stats  = provider.estadisticas;
    final total  = stats.fold<int>(0, (s, e) => s + e.totalElaboraciones);
    final top    = stats.reduce(
        (a, b) => a.totalElaboraciones >= b.totalElaboraciones ? a : b);

    return RefreshIndicator(
      onRefresh: () => provider.cargar(auth.token!),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // ── Tarjeta resumen ──────────────────────────────────
          _TarjetaResumen(
            totalElaboraciones: total,
            recetaTop:          top,
          ),
          const SizedBox(height: 20),

          // ── Título sección ───────────────────────────────────
          Text(
            'Por receta',
            style: Theme.of(context)
                .textTheme
                .titleMedium
                ?.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 12),

          // ── Lista de recetas ─────────────────────────────────
          ...stats
              .sorted()
              .map((e) => _EstadisticaCard(estadistica: e)),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────

class _TarjetaResumen extends StatelessWidget {
  final int totalElaboraciones;
  final EstadisticaReceta recetaTop;

  const _TarjetaResumen({
    required this.totalElaboraciones,
    required this.recetaTop,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return Card(
      color: cs.primaryContainer,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Row(
          children: [
            // Total elaboraciones
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '$totalElaboraciones',
                    style: tt.displaySmall?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: cs.onPrimaryContainer,
                    ),
                  ),
                  Text(
                    'elaboraciones\nen total',
                    style: tt.bodySmall?.copyWith(
                        color: cs.onPrimaryContainer.withOpacity(0.8)),
                  ),
                ],
              ),
            ),
            Container(
              width: 1, height: 60,
              color: cs.onPrimaryContainer.withOpacity(0.2),
            ),
            const SizedBox(width: 16),
            // Receta top
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '🏆 Más elaborada',
                    style: tt.labelSmall?.copyWith(
                        color: cs.onPrimaryContainer.withOpacity(0.7)),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    recetaTop.recetaNombre,
                    style: tt.titleSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: cs.onPrimaryContainer,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  Text(
                    '${recetaTop.totalElaboraciones} veces',
                    style: tt.bodySmall?.copyWith(
                        color: cs.onPrimaryContainer.withOpacity(0.8)),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _EstadisticaCard extends StatelessWidget {
  final EstadisticaReceta estadistica;
  const _EstadisticaCard({required this.estadistica});

  @override
  Widget build(BuildContext context) {
    final cs   = Theme.of(context).colorScheme;
    final tt   = Theme.of(context).textTheme;
    final tasa = estadistica.tasaCompletado;

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Nombre + total
            Row(
              children: [
                Expanded(
                  child: Text(
                    estadistica.recetaNombre,
                    style: tt.titleSmall
                        ?.copyWith(fontWeight: FontWeight.w600),
                  ),
                ),
                Text(
                  '${estadistica.totalElaboraciones} elabor.',
                  style: tt.labelSmall
                      ?.copyWith(color: cs.onSurfaceVariant),
                ),
              ],
            ),
            const SizedBox(height: 10),

            // Barra de progreso
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value:           tasa,
                minHeight:       8,
                backgroundColor: cs.surfaceContainerHighest,
                valueColor:      AlwaysStoppedAnimation<Color>(
                  tasa >= 0.8
                      ? Colors.green
                      : tasa >= 0.5
                          ? cs.primary
                          : cs.error,
                ),
              ),
            ),
            const SizedBox(height: 6),

            // Completadas / total + última fecha
            Row(
              children: [
                Text(
                  '${estadistica.elaboracionesCompletadas}/'
                  '${estadistica.totalElaboraciones} completadas '
                  '(${(tasa * 100).toStringAsFixed(0)}%)',
                  style: tt.labelSmall
                      ?.copyWith(color: cs.onSurfaceVariant),
                ),
                const Spacer(),
                if (estadistica.ultimaElaboracion.isNotEmpty)
                  Text(
                    'Última: ${_formatFecha(estadistica.ultimaElaboracion)}',
                    style: tt.labelSmall
                        ?.copyWith(color: cs.onSurfaceVariant),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _formatFecha(String iso) {
    try {
      final dt = DateTime.parse(iso).toLocal();
      return '${dt.day.toString().padLeft(2,'0')}/'
             '${dt.month.toString().padLeft(2,'0')}/'
             '${dt.year}';
    } catch (_) { return iso; }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Extension helper para ordenar estadísticas
// ─────────────────────────────────────────────────────────────────────────────

extension _SortStats on List<EstadisticaReceta> {
  List<EstadisticaReceta> sorted() =>
      [...this]..sort((a, b) =>
          b.totalElaboraciones.compareTo(a.totalElaboraciones));
}