import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../models/ingrediente.dart';
import '../../providers/auth_provider.dart';
import '../../providers/recetas_provider.dart';
import '../../providers/ingredientes_provider.dart';
import '../../widgets/ingrediente_card.dart';
import '../../widgets/loading_widget.dart';
import '../home_screen.dart';

class IngredientesListScreen extends StatefulWidget {
  const IngredientesListScreen({super.key});

  @override
  State<IngredientesListScreen> createState() => _IngredientesListScreenState();
}

class _IngredientesListScreenState extends State<IngredientesListScreen> {
  final _searchCtrl = TextEditingController();
  String _busqueda = '';

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  List<Ingrediente> get _ingredientesFiltrados {
    final provider = context.watch<IngredientesProvider>();
    if (_busqueda.isEmpty) return provider.ingredientes;
    return provider.ingredientes
        .where((i) => i.nombre.toLowerCase().contains(_busqueda.toLowerCase()))
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<IngredientesProvider>();
    final auth = context.read<AuthProvider>();
    final esJefe = auth.esJefeCocina;
    final ingredientes = _ingredientesFiltrados;

    if (provider.isLoading && provider.ingredientes.isEmpty) {
      return const LoadingWidget.inline(mensaje: 'Cargando ingredientes…');
    }

    return Scaffold(
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: SearchBar(
              controller: _searchCtrl,
              hintText: 'Buscar ingredientes…',
              leading: const Icon(Icons.search),
              trailing: [
                if (_busqueda.isNotEmpty)
                  IconButton(
                    icon: const Icon(Icons.clear),
                    onPressed: () {
                      _searchCtrl.clear();
                      setState(() => _busqueda = '');
                    },
                  ),
              ],
              onChanged: (v) => setState(() => _busqueda = v),
            ),
          ),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => provider.cargar(auth.token!),
              child: Stack(
                children: [
                  ingredientes.isEmpty
                      ? SingleChildScrollView(
                          physics: const AlwaysScrollableScrollPhysics(),
                          child: SizedBox(
                            height: MediaQuery.of(context).size.height * 0.7,
                            child: EmptyState(
                              titulo: _busqueda.isEmpty
                                  ? 'Sin ingredientes'
                                  : 'Sin resultados',
                              subtitulo: esJefe
                                  ? 'Añade el primer ingrediente con el botón +'
                                  : 'El jefe de cocina aún no ha añadido ingredientes.',
                              onRecargar: () => provider.cargar(auth.token!),
                            ),
                          ),
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.only(top: 8, bottom: 88),
                          itemCount: ingredientes.length,
                          itemBuilder: (context, i) {
                            final ing = ingredientes[i];
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
                                        context,
                                        ing.nombre,
                                      );
                                      if (ok && context.mounted) {
                                        try {
                                          await provider.eliminar(
                                            ing.id,
                                            auth.usuarioId!,
                                            auth.token!,
                                          );
                                        } catch (_) {
                                          if (context.mounted) {
                                            ScaffoldMessenger.of(context).showSnackBar(
                                              const SnackBar(
                                                content: Text(
                                                  'No se puede eliminar: este ingrediente está siendo usado en una o más recetas.',
                                                ),
                                                backgroundColor: Colors.red,
                                              ),
                                            );
                                          }
                                        }
                                      }
                                    }
                                  : null,
                            );
                          },
                        ),
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
          ),
        ],
      ),
    );
  }

  Future<void> _mostrarDialogCantidad(
    BuildContext context,
    Ingrediente ing,
  ) async {
    final ctrl = TextEditingController(text: ing.stockActual.toString());
    final formKey = GlobalKey<FormState>();

    final confirmar = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text('Actualizar stock\n${ing.nombre}'),
        content: Form(
          key: formKey,
          child: TextFormField(
            controller: ctrl,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            autofocus: true,
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
    final recetasProvider = context.read<RecetasProvider>();
    final token = context.read<AuthProvider>().token!;
    final usuarioId = context.read<AuthProvider>().usuarioId!;
    try {
      await provider.actualizarCantidad(ing.id, double.parse(ctrl.text), token);
      // Revalidar recetas afectadas por este ingrediente para actualizar chips
      await recetasProvider.verificarPorIngrediente(
        ing.id,
        usuarioId,
        token,
      );
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
            content: Text(
              '¿Eliminar "$nombre"? Esta acción no se puede deshacer.',
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
                child: const Text('Eliminar'),
              ),
            ],
          ),
        ) ??
        false;
  }
}
