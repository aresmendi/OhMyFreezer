// lib/screens/recetas/receta_detail_screen.dart

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../models/receta.dart';
import '../../providers/auth_provider.dart';
import '../../providers/ingredientes_provider.dart';
import '../../providers/recetas_provider.dart';
import '../../widgets/loading_widget.dart';

/// Detalle completo de una receta.
///
/// Flujo de elaboración:
/// 1. Botón "Verificar stock" → llama a [RecetasProvider.verificar].
/// 2. Si [puedeElaborarse] == true → botón "Elaborar" activo.
/// 3. Botón "Elaborar" → dialog de confirmación → [RecetasProvider.elaborar].
/// 4. Tras elaborar → SnackBar de éxito + reset de verificación.
class RecetaDetailScreen extends StatelessWidget {
  const RecetaDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<RecetasProvider>();
    final receta   = provider.seleccionada;

    if (provider.isLoading || receta == null) {
      return const Scaffold(
        body: LoadingWidget.inline(mensaje: 'Cargando receta…'),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(receta.nombre),
        actions: [
          if (context.read<AuthProvider>().esJefeCocina)
            IconButton(
              icon: const Icon(Icons.edit_outlined),
              tooltip: 'Editar receta',
              onPressed: () => Navigator.pushNamed(
                context, '/recetas/nueva',
                arguments: receta,
              ),
            ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _SeccionDescripcion(receta: receta),
            const SizedBox(height: 24),
            _SeccionIngredientes(receta: receta),
            const SizedBox(height: 24),
            _SeccionPasos(receta: receta),
            const SizedBox(height: 32),
            _BotonesElaboracion(receta: receta),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}

// ─── Secciones ───────────────────────────────────────────────

class _SeccionDescripcion extends StatelessWidget {
  final Receta receta;
  const _SeccionDescripcion({required this.receta});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(receta.descripcion,
            style: tt.bodyLarge?.copyWith(color: cs.onSurfaceVariant)),
        const SizedBox(height: 8),
        Text(
          'Creada el ${_formatFecha(receta.fechaCreacion)}',
          style: tt.labelSmall?.copyWith(color: cs.onSurfaceVariant),
        ),
      ],
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

class _SeccionIngredientes extends StatelessWidget {
  final Receta receta;
  const _SeccionIngredientes({required this.receta});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Ingredientes', style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
        const SizedBox(height: 10),
        ...receta.ingredientes.map((ri) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 4),
          child: Row(
            children: [
              Icon(Icons.circle, size: 8, color: cs.primary),
              const SizedBox(width: 10),
              Expanded(child: Text(ri.ingrediente.nombre, style: tt.bodyMedium)),
              Text(
                '${ri.cantidadNecesaria} ${ri.ingrediente.unidadMedida}',
                style: tt.bodyMedium?.copyWith(
                  color: cs.onSurfaceVariant,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        )),
      ],
    );
  }
}

class _SeccionPasos extends StatelessWidget {
  final Receta receta;
  const _SeccionPasos({required this.receta});

  @override
  Widget build(BuildContext context) {
    final tt    = Theme.of(context).textTheme;
    final pasos = [...receta.pasos]..sort((a, b) => a.orden.compareTo(b.orden));
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Text('Pasos', style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
            const Spacer(),
            TextButton.icon(
              onPressed: () => Navigator.pushNamed(
                context, '/recetas/pasos',
                arguments: receta,
              ),
              icon:  const Icon(Icons.play_circle_outline_rounded, size: 18),
              label: const Text('Modo paso a paso'),
            ),
          ],
        ),
        const SizedBox(height: 10),
        ...pasos.map((p) => Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CircleAvatar(
                radius: 14,
                backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                child: Text('${p.orden}',
                    style: tt.labelSmall?.copyWith(fontWeight: FontWeight.w700)),
              ),
              const SizedBox(width: 12),
              Expanded(child: Text(p.descripcion, style: tt.bodyMedium)),
            ],
          ),
        )),
      ],
    );
  }
}

class _BotonesElaboracion extends StatelessWidget {
  final Receta receta;
  const _BotonesElaboracion({required this.receta});

  @override
  Widget build(BuildContext context) {
    final provider  = context.watch<RecetasProvider>();
    final auth      = context.read<AuthProvider>();
    final cs        = Theme.of(context).colorScheme;
    final verificado = receta.puedeElaborarse;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Chip de estado de stock
        if (verificado != null)
          Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Chip(
              avatar: Icon(
                verificado ? Icons.check_circle_outline : Icons.block_rounded,
                size: 18,
                color: verificado ? Colors.green : cs.error,
              ),
              label: Text(
                verificado
                    ? 'Stock suficiente para elaborar'
                    : 'Stock insuficiente — no se puede elaborar',
                style: TextStyle(
                  color: verificado ? Colors.green : cs.error,
                  fontWeight: FontWeight.w600,
                ),
              ),
              backgroundColor:
                  (verificado ? Colors.green : cs.error).withOpacity(0.1),
              side: BorderSide(
                  color: (verificado ? Colors.green : cs.error).withOpacity(0.4)),
            ),
          ),

        // Botón verificar
        OutlinedButton.icon(
          onPressed: provider.isLoading
              ? null
              : () => provider.verificar(receta.id,auth.usuarioId!, auth.token!),
          icon:  const Icon(Icons.search_rounded),
          label: const Text('Verificar stock'),
          style: OutlinedButton.styleFrom(
            minimumSize: const Size.fromHeight(48),
          ),
        ),
        const SizedBox(height: 12),

        // Botón elaborar
        FilledButton.icon(
          onPressed: (verificado == true && !provider.isLoading)
              ? () => _confirmarElaborar(context)
              : null,
          icon:  const Icon(Icons.restaurant_rounded),
          label: const Text('Elaborar receta'),
          style: FilledButton.styleFrom(
            minimumSize: const Size.fromHeight(52),
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14)),
          ),
        ),
      ],
    );
  }

  Future<void> _confirmarElaborar(BuildContext context) async {
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Elaborar receta'),
        content: const Text(
            'Se descontará el stock de los ingredientes necesarios. ¿Continuar?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Elaborar'),
          ),
        ],
      ),
    );

    if (confirmar != true || !context.mounted) return;

    final provider = context.read<RecetasProvider>();
    final ingProvider = context.read<IngredientesProvider>();
    final auth = context.read<AuthProvider>();
    final token = auth.token!;
    final usuarioId = auth.usuarioId!;
    try {
      await provider.elaborar(receta.id,usuarioId, token);
      //Recargamos ingredientes para reflejar descuento de stock
      ingProvider.cargar(token);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Text('✅ Receta elaborada. Stock actualizado.'),
            backgroundColor: Colors.green,
            behavior: SnackBarBehavior.floating,
            margin: const EdgeInsets.all(16),
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(10)),
          ),
        );
      }
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Error al elaborar la receta.')),
        );
      }
    }
  }
}