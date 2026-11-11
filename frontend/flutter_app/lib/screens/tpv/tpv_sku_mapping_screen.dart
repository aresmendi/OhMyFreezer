import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../models/tpv_sku_mapping.dart';
import '../../providers/auth_provider.dart';
import '../../providers/tpv_sku_mapping_provider.dart';
import '../../providers/recetas_provider.dart';
import '../../widgets/loading_widget.dart';
import '../home_screen.dart';

/// Pantalla de mapeo entre SKUs del TPV y recetas del sistema.
///
/// Muestra dos secciones:
/// - **Pendientes**: SKUs sin receta asignada, con botón para mapear.
/// - **Mapeados**: SKUs con receta asignada, con opción para desmapear.
class TpvSkuMappingScreen extends StatelessWidget {
  const TpvSkuMappingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<TpvSkuMappingProvider>();
    final auth = context.read<AuthProvider>();

    if (provider.isLoading && provider.mappings.isEmpty) {
      return const LoadingWidget.inline(mensaje: 'Cargando mapeos TPV…');
    }

    final pendientes = provider.mappings.where((m) => !m.mapeado).toList();
    final mapeados   = provider.mappings.where((m) => m.mapeado).toList();

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: () => provider.cargar(auth.token!),
        child: provider.mappings.isEmpty
            ? SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                child: SizedBox(
                  height: MediaQuery.of(context).size.height * 0.7,
                  child: EmptyState(
                    titulo: 'Sin mapeos TPV',
                    subtitulo: 'No hay SKUs registrados desde el TPV todavía.',
                    onRecargar: () => provider.cargar(auth.token!),
                  ),
                ),
              )
            : ListView(
                padding: const EdgeInsets.only(bottom: 24),
                children: [
                  if (pendientes.isNotEmpty) ...[
                    _SectionHeader(
                      'Pendientes (${pendientes.length})',
                      Icons.link_off_rounded,
                    ),
                    ...pendientes.map(
                      (m) => _PendienteCard(
                        mapping: m,
                        onAsignar: () => _mostrarDialogoAsignar(context, m),
                      ),
                    ),
                  ],
                  if (mapeados.isNotEmpty) ...[
                    _SectionHeader(
                      'Mapeados (${mapeados.length})',
                      Icons.link_rounded,
                    ),
                    ...mapeados.map(
                      (m) => _MapeadoCard(
                        mapping: m,
                        onDesmapear: () => _confirmarDesmapear(context, m),
                      ),
                    ),
                  ],
                ],
              ),
      ),
    );
  }

  Future<void> _mostrarDialogoAsignar(
    BuildContext context,
    TpvSkuMapping mapping,
  ) async {
    final provider      = context.read<TpvSkuMappingProvider>();
    final auth          = context.read<AuthProvider>();
    final recetas       = context.read<RecetasProvider>().recetas;

    if (recetas.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('No hay recetas disponibles para asignar')),
      );
      return;
    }

    int? recetaIdSeleccionada;

    await showDialog<void>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          title: Text('Asignar receta a ${mapping.skuTpv}'),
          content: DropdownButton<int>(
            value: recetaIdSeleccionada,
            hint: const Text('Seleccionar receta'),
            isExpanded: true,
            items: recetas
                .map(
                  (r) => DropdownMenuItem<int>(
                    value: r.id,
                    child: Text(r.nombre, overflow: TextOverflow.ellipsis),
                  ),
                )
                .toList(),
            onChanged: (v) => setDialogState(() => recetaIdSeleccionada = v),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancelar'),
            ),
            FilledButton(
              onPressed: recetaIdSeleccionada == null
                  ? null
                  : () async {
                      Navigator.pop(ctx);
                      try {
                        await provider.asignarReceta(
                          mapping.id,
                          recetaIdSeleccionada!,
                          auth.token!,
                        );
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('Receta asignada correctamente'),
                            ),
                          );
                        }
                      } catch (e) {
                        if (context.mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(
                              content: Text('Error al asignar: $e'),
                              backgroundColor:
                                  Theme.of(context).colorScheme.error,
                            ),
                          );
                        }
                      }
                    },
              child: const Text('Asignar'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _confirmarDesmapear(
    BuildContext context,
    TpvSkuMapping mapping,
  ) async {
    final ok = await showDialog<bool>(
          context: context,
          builder: (_) => AlertDialog(
            title: const Text('Desmapear SKU'),
            content: Text(
              '¿Quitar la receta "${mapping.recetaNombre}" del SKU ${mapping.skuTpv}?',
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('Cancelar'),
              ),
              FilledButton(
                style: FilledButton.styleFrom(
                  backgroundColor: Theme.of(context).colorScheme.error,
                ),
                onPressed: () => Navigator.pop(context, true),
                child: const Text('Desmapear'),
              ),
            ],
          ),
        ) ??
        false;

    if (!ok || !context.mounted) return;

    try {
      await context
          .read<TpvSkuMappingProvider>()
          .desmapear(mapping.id, context.read<AuthProvider>().token!);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Mapeo eliminado')),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error al desmapear: $e'),
            backgroundColor: Theme.of(context).colorScheme.error,
          ),
        );
      }
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Subwidgets privados
// ─────────────────────────────────────────────────────────────────────────────

class _SectionHeader extends StatelessWidget {
  final String   label;
  final IconData icon;
  const _SectionHeader(this.label, this.icon);

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 4),
      child: Row(
        children: [
          Icon(icon, size: 18, color: cs.primary),
          const SizedBox(width: 8),
          Text(
            label,
            style: tt.titleSmall?.copyWith(
              color: cs.primary,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

/// Tarjeta para un SKU pendiente de mapear.
class _PendienteCard extends StatelessWidget {
  final TpvSkuMapping mapping;
  final VoidCallback  onAsignar;
  const _PendienteCard({required this.mapping, required this.onAsignar});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: cs.errorContainer,
          child: Icon(Icons.link_off_rounded, color: cs.onErrorContainer, size: 20),
        ),
        title: Text(mapping.skuTpv, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: mapping.nombreTpv != null
            ? Text(mapping.nombreTpv!, maxLines: 1, overflow: TextOverflow.ellipsis)
            : const Text('Sin nombre en TPV', style: TextStyle(fontStyle: FontStyle.italic)),
        trailing: FilledButton.tonal(
          onPressed: onAsignar,
          child: const Text('Asignar'),
        ),
      ),
    );
  }
}

/// Tarjeta para un SKU ya mapeado a una receta.
class _MapeadoCard extends StatelessWidget {
  final TpvSkuMapping mapping;
  final VoidCallback  onDesmapear;
  const _MapeadoCard({required this.mapping, required this.onDesmapear});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: cs.primaryContainer,
          child: Icon(Icons.link_rounded, color: cs.onPrimaryContainer, size: 20),
        ),
        title: Text(mapping.skuTpv, style: const TextStyle(fontWeight: FontWeight.w600)),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (mapping.nombreTpv != null)
              Text(mapping.nombreTpv!, maxLines: 1, overflow: TextOverflow.ellipsis),
            Text(
              '→ ${mapping.recetaNombre ?? ''}',
              style: TextStyle(color: cs.primary, fontWeight: FontWeight.w500),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ],
        ),
        isThreeLine: mapping.nombreTpv != null,
        trailing: IconButton(
          tooltip: 'Desmapear',
          icon: Icon(Icons.link_off_rounded, color: cs.error),
          onPressed: onDesmapear,
        ),
      ),
    );
  }
}
