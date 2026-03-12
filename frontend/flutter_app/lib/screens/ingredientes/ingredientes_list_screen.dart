// lib/screens/ingredientes/ingredientes_list_screen.dart

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../providers/ingredientes_provider.dart';
import '../../widgets/ingrediente_card.dart';
import '../../widgets/loading_widget.dart';
import '../home_screen.dart'; // EmptyState

/// Lista de ingredientes del inventario.
///
/// - Todos los roles: ver lista + actualizar cantidad (dialog inline).
/// - Jefe de cocina: botón FAB para crear + swipe-to-delete.
class IngredientesListScreen extends StatelessWidget {
  const IngredientesListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<IngredientesProvider>();
    final auth     = context.read<AuthProvider>();
    final esJefe   = auth.esJefeCocina;

    if (provider.isLoading && provider.ingredientes.isEmpty) {
      return const LoadingWidget.inline(mensaje: 'Cargando ingredientes…');
    }

    return Scaffold(
      body: RefreshIndicator(
        onRefresh: () => provider.cargar(auth.token!),
        child: Stack(
          children: [
            provider.ingredientes.isEmpty
                ? SingleChildScrollView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    child: SizedBox(
                      height: MediaQuery.of(context).size.height * 0.7,
                      child: EmptyState(
                        titulo: 'Sin ingredientes',
                        subtitulo: esJefe
                            ? 'Añade el primer ingrediente con el botón +'
                            : 'El jefe de cocina aún no ha añadido ingredientes.',
                        onRecargar: () => provider.cargar(auth.token!),
                      ),
                    ),
                  )
                : ListView.builder(
                    padding: const EdgeInsets.only(top: 8, bottom: 88),
                    itemCount: provider.ingredientes.length,
                    itemBuilder: (context, i) {
                      final ing = provider.ingredientes[i];
                      return IngredienteCard(
                        ingrediente: ing,
                        onTap: esJefe
                            ? () => Navigator.pushNamed(
                                  context,
                                  '/ingredientes/editar',
                                  arguments: ing,
                                )
                            : null,
                        onActualizarCantidad: () =>
                            _mostrarDialogCantidad(context, ing),
                        onEliminar: esJefe
                            ? () async {
                                final ok = await _confirmarEliminar(
                                    context, ing.nombre);
                                if (ok && context.mounted) {
                                  provider.eliminar(ing.id, auth.usuarioId!, auth.token!);
                                }
                              }
                            : null,
                      );
                    },
                  ),

            // FAB solo para jefe de cocina
            if (esJefe)
              Positioned(
                bottom: 16,
                right: 16,
                child: FloatingActionButton.extended(
                  heroTag: 'fab_ingrediente',
                  onPressed: () =>
                      Navigator.pushNamed(context, '/ingredientes/nuevo'),
                  icon: const Icon(Icons.add_rounded),
                  label: const Text('Ingrediente'),
                ),
              ),
          ],
        ),
      ),
    );
  }

  // ─── Dialogs ────────────────────────────────────────────────

  Future<void> _mostrarDialogCantidad(
      BuildContext context, ing) async {
    final ctrl = TextEditingController(
        text: ing.stockActual.toString());
    final formKey = GlobalKey<FormState>();

    final confirmar = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text('Actualizar stock\n${ing.nombre}'),
        content: Form(
          key: formKey,
          child: TextFormField(
            controller:   ctrl,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            autofocus:    true,
            decoration: InputDecoration(
              labelText: 'Cantidad (${ing.unidadMedida})',
              suffixText: ing.unidadMedida,
            ),
            validator: (v) {
              if (v == null || v.isEmpty) return 'Campo obligatorio';
              if (double.tryParse(v) == null) return 'Número no válido';
              if (double.parse(v) < 0) return 'No puede ser negativo';
              return null;
            },
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () {
              if (formKey.currentState!.validate()) {
                Navigator.pop(context, true);
              }
            },
            child: const Text('Guardar'),
          ),
        ],
      ),
    );

    if (confirmar != true || !context.mounted) return;

    final provider = context.read<IngredientesProvider>();
    final token    = context.read<AuthProvider>().token!;
    try {
      await provider.actualizarCantidad(
          ing.id, double.parse(ctrl.text), token);
    } catch (_) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Error al actualizar el stock.')),
        );
      }
    }
  }

  Future<bool> _confirmarEliminar(BuildContext context, String nombre) async {
    return await showDialog<bool>(
          context: context,
          builder: (_) => AlertDialog(
            title: const Text('Eliminar ingrediente'),
            content: Text('¿Eliminar "$nombre"? Esta acción no se puede deshacer.'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('Cancelar'),
              ),
              FilledButton(
                style: FilledButton.styleFrom(
                    backgroundColor: Theme.of(context).colorScheme.error),
                onPressed: () => Navigator.pop(context, true),
                child: const Text('Eliminar'),
              ),
            ],
          ),
        ) ??
        false;
  }
}

class _FondoEliminar extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Container(
      alignment: Alignment.centerRight,
      padding: const EdgeInsets.only(right: 24),
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.errorContainer,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Icon(Icons.delete_outline_rounded,
          color: Theme.of(context).colorScheme.onErrorContainer),
    );
  }
}