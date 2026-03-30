import 'package:flutter/material.dart';
import 'package:flutter_app/models/receta.dart';
import 'package:flutter_app/providers/favoritos_provider.dart';
import 'package:provider/provider.dart';

import '../../providers/auth_provider.dart';
import '../../providers/recetas_provider.dart';
import '../../widgets/receta_card.dart';
import '../../widgets/loading_widget.dart';
import '../home_screen.dart';

class RecetasListScreen extends StatefulWidget {
  const RecetasListScreen({super.key});

  @override
  State<RecetasListScreen> createState() => _RecetasListScreenState();
}

class _RecetasListScreenState extends State<RecetasListScreen> {
  final _searchCtrl = TextEditingController();
  String _busqueda = '';

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  List<Receta> get _recetasFiltradas {
    final provider = context.read<RecetasProvider>();
    if (_busqueda.isEmpty) return provider.recetas;
    return provider.recetas
        .where((r) => r.nombre.toLowerCase().contains(_busqueda.toLowerCase()))
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<RecetasProvider>();
    final auth     = context.read<AuthProvider>();
    final esJefe   = auth.esJefeCocina;
    final recetas  = _recetasFiltradas;

    if (provider.isLoading && provider.recetas.isEmpty) {
      return const LoadingWidget.inline(mensaje: 'Cargando recetas…');
    }

    return Scaffold(
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: SearchBar(
              controller: _searchCtrl,
              hintText: 'Buscar recetas…',
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
              onRefresh: () async {
                final favoritosProvider = context.read<FavoritosProvider>();
                await Future.wait([
                  provider.cargar(auth.token!),
                  favoritosProvider.cargar(auth.usuarioId!, auth.token!)
                ]);
                provider.sincronizarFavoritos(favoritosProvider.idsFavoritos);
              },
              child: Stack(
                children: [
                  recetas.isEmpty
                      ? SingleChildScrollView(
                          physics: const AlwaysScrollableScrollPhysics(),
                          child: SizedBox(
                            height: MediaQuery.of(context).size.height * 0.7,
                            child: EmptyState(
                              titulo: _busqueda.isEmpty ? 'Sin recetas' : 'Sin resultados',
                              subtitulo: esJefe
                                  ? 'Crea la primera receta con el botón +'
                                  : 'El jefe de cocina aún no ha creado recetas.',
                              onRecargar: () => provider.cargar(auth.token!),
                            ),
                          ),
                        )
                      : ListView.builder(
                          padding: const EdgeInsets.only(top: 8, bottom: 88),
                          itemCount: recetas.length,
                          itemBuilder: (context, i) {
                            final receta = recetas[i];
                            return RecetaCard(
                              receta: receta,
                              onTap: () => _irDetalle(context, receta.id),
                              onEliminar: esJefe
                                  ? () async {
                                      final ok = await _confirmarEliminar(context, receta.nombre);
                                      if (ok && context.mounted) {
                                        try {
                                          await provider.eliminar(receta.id, auth.usuarioId!, auth.token!);
                                        } catch (e) {
                                          if (context.mounted) {
                                            ScaffoldMessenger.of(context).showSnackBar(
                                              SnackBar(
                                                content: Text('Error al eliminar: $e'),
                                                backgroundColor: Theme.of(context).colorScheme.error,
                                              ),
                                            );
                                          }
                                        }
                                      }
                                    }
                                  : null,
                              mostrarFavorito: true,
                              onToggleFavorito: () => _toggleFavorito(context, receta),
                            );
                          },
                        ),
                  if (esJefe)
                    Positioned(
                      bottom: 16,
                      right: 16,
                      child: FloatingActionButton.extended(
                        heroTag: 'fab_receta',
                        onPressed: () => Navigator.pushNamed(context, '/recetas/nueva'),
                        icon: const Icon(Icons.add_rounded),
                        label: const Text('Receta'),
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

  void _irDetalle(BuildContext context, int id) {
    context.read<RecetasProvider>().seleccionar(id, context.read<AuthProvider>().token!);
    Navigator.pushNamed(context, '/recetas/detalle', arguments: id);
  }

  Future<bool> _confirmarEliminar(BuildContext context, String nombre) async {
    return await showDialog<bool>(
          context: context,
          builder: (_) => AlertDialog(
            title: const Text('Eliminar receta'),
            content: Text('¿Eliminar "$nombre"? Esta acción no se puede deshacer.'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: const Text('Cancelar'),
              ),
              FilledButton(
                style: FilledButton.styleFrom(backgroundColor: Theme.of(context).colorScheme.error),
                onPressed: () => Navigator.pop(context, true),
                child: const Text('Eliminar'),
              ),
            ],
          ),
        ) ??
        false;
  }

  Future<void> _toggleFavorito(BuildContext context, Receta receta) async {
    final auth = context.read<AuthProvider>();
    final favoritosProvider = context.read<FavoritosProvider>();
    final recetasProvider = context.read<RecetasProvider>();

    try {
      await favoritosProvider.toggle(
        usuarioId: auth.usuarioId!,
        recetaId: receta.id,
        token: auth.token!,
      );
      final esFavoritaNow = favoritosProvider.esFavorita(receta.id);
      recetasProvider.actualizarFavoritoLocal(receta.id, esFavoritaNow);
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(esFavoritaNow ? '${receta.nombre} añadida a favoritos' : '${receta.nombre} quitada de favoritos'),
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red),
        );
      }
    }
  }
}
