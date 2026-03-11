// lib/screens/alertas/alertas_screen.dart

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../models/alerta.dart';
import '../../providers/auth_provider.dart';
import '../../providers/alertas_provider.dart';
import '../../widgets/loading_widget.dart';
import '../home_screen.dart'; // EmptyState

/// Pantalla de alertas de stock. Solo visible para el jefe de cocina.
///
/// - Lista de alertas ordenadas por fecha (más recientes primero).
/// - Chip de filtro: Todas / No leídas.
/// - Swipe-to-dismiss para marcar como leída.
/// - Botón "Marcar todas como leídas" en AppBar.
/// - Pull-to-refresh.
class AlertasScreen extends StatefulWidget {
  const AlertasScreen({super.key});

  @override
  State<AlertasScreen> createState() => _AlertasScreenState();
}

class _AlertasScreenState extends State<AlertasScreen> {
  bool _soloNoLeidas = false;

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<AlertasProvider>();
    final auth     = context.read<AuthProvider>();

    final alertas = (_soloNoLeidas ? provider.noLeidas : provider.alertas)
      ..sort((a, b) => b.fechaCreacion.compareTo(a.fechaCreacion));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Alertas'),
        actions: [
          if (provider.contadorNoLeidas > 0)
            TextButton.icon(
              onPressed: () =>
                  provider.marcarTodasLeidas(auth.token!),
              icon:  const Icon(Icons.done_all_rounded, size: 18),
              label: const Text('Todas leídas'),
            ),
        ],
      ),
      body: Column(
        children: [
          // Filtro chips
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 4),
            child: Row(
              children: [
                FilterChip(
                  label:    const Text('Todas'),
                  selected: !_soloNoLeidas,
                  onSelected: (_) => setState(() => _soloNoLeidas = false),
                ),
                const SizedBox(width: 8),
                FilterChip(
                  label: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Text('No leídas'),
                      if (provider.contadorNoLeidas > 0) ...[
                        const SizedBox(width: 6),
                        Badge(
                          label: Text('${provider.contadorNoLeidas}'),
                        ),
                      ],
                    ],
                  ),
                  selected: _soloNoLeidas,
                  onSelected: (_) => setState(() => _soloNoLeidas = true),
                ),
              ],
            ),
          ),

          // Lista
          Expanded(
            child: provider.isLoading && alertas.isEmpty
                ? const LoadingWidget.inline(mensaje: 'Cargando alertas…')
                : alertas.isEmpty
                    ? EmptyState(
                        titulo:    _soloNoLeidas
                            ? '¡Todo al día!'
                            : 'Sin alertas',
                        subtitulo: _soloNoLeidas
                            ? 'No tienes alertas pendientes de leer.'
                            : 'No se han generado alertas de stock.',
                        onRecargar: () => provider.recargar(auth.token!),
                      )
                    : RefreshIndicator(
                        onRefresh: () => provider.recargar(auth.token!),
                        child: ListView.builder(
                          padding: const EdgeInsets.only(
                              top: 8, bottom: 24, left: 12, right: 12),
                          itemCount: alertas.length,
                          itemBuilder: (context, i) {
                            final alerta = alertas[i];
                            return _AlertaTile(
                              alerta:       alerta,
                              onMarcarLeida: alerta.leida
                                  ? null
                                  : () => provider.marcarLeida(
                                        alerta.id, auth.token!),
                            );
                          },
                        ),
                      ),
          ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────

class _AlertaTile extends StatelessWidget {
  final Alerta alerta;
  final VoidCallback? onMarcarLeida;

  const _AlertaTile({required this.alerta, this.onMarcarLeida});

  @override
  Widget build(BuildContext context) {
    final cs     = Theme.of(context).colorScheme;
    final tt     = Theme.of(context).textTheme;
    final esGrave = alerta.tipo == TipoAlerta.stockAgotado;

    final colorFondo = alerta.leida
        ? cs.surfaceContainerLow
        : (esGrave
            ? cs.errorContainer.withOpacity(0.5)
            : cs.tertiaryContainer.withOpacity(0.5));

    final colorIcono = esGrave ? cs.error : cs.tertiary;

    return Dismissible(
      key: ValueKey(alerta.id),
      direction: alerta.leida
          ? DismissDirection.none
          : DismissDirection.endToStart,
      background: Container(
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.only(right: 24),
        margin: const EdgeInsets.only(bottom: 8),
        decoration: BoxDecoration(
          color: cs.primaryContainer,
          borderRadius: BorderRadius.circular(14),
        ),
        child: Icon(Icons.done_rounded, color: cs.onPrimaryContainer),
      ),
      onDismissed: (_) => onMarcarLeida?.call(),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        margin: const EdgeInsets.only(bottom: 8),
        decoration: BoxDecoration(
          color: colorFondo,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(
            color: alerta.leida ? cs.outlineVariant : colorIcono.withOpacity(0.4),
          ),
        ),
        child: ListTile(
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          leading: CircleAvatar(
            backgroundColor: colorIcono.withOpacity(0.15),
            child: Icon(
              esGrave
                  ? Icons.block_rounded
                  : Icons.warning_amber_rounded,
              color: colorIcono,
            ),
          ),
          title: Text(
            alerta.ingredienteNombre,
            style: tt.titleSmall?.copyWith(
              fontWeight: alerta.leida ? FontWeight.w400 : FontWeight.w700,
            ),
          ),
          subtitle: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 2),
              Text(alerta.mensaje, style: tt.bodySmall),
              const SizedBox(height: 4),
              Text(
                _formatFecha(alerta.fechaCreacion),
                style: tt.labelSmall
                    ?.copyWith(color: cs.onSurfaceVariant),
              ),
            ],
          ),
          trailing: alerta.leida
              ? Icon(Icons.check_circle_outline_rounded,
                  color: cs.onSurfaceVariant, size: 18)
              : IconButton(
                  icon: const Icon(Icons.mark_email_read_outlined),
                  tooltip: 'Marcar como leída',
                  onPressed: onMarcarLeida,
                ),
        ),
      ),
    );
  }

  String _formatFecha(String iso) {
    try {
      final dt = DateTime.parse(iso).toLocal();
      final ahora = DateTime.now();
      final diff  = ahora.difference(dt);
      if (diff.inMinutes < 1)  return 'Ahora mismo';
      if (diff.inMinutes < 60) return 'Hace ${diff.inMinutes} min';
      if (diff.inHours   < 24) return 'Hace ${diff.inHours} h';
      return '${dt.day.toString().padLeft(2,'0')}/'
             '${dt.month.toString().padLeft(2,'0')}/'
             '${dt.year}';
    } catch (_) { return iso; }
  }
}