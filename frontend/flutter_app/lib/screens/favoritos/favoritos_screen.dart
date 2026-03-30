import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/receta_favorita.dart';
import '../../providers/favoritos_provider.dart';
import '../../providers/auth_provider.dart';
import '../../providers/recetas_provider.dart';
import '../../widgets/receta_card.dart';
import '../home_screen.dart';

class FavoritosScreen extends StatefulWidget {
  const FavoritosScreen({super.key});

  @override
  State<FavoritosScreen> createState() => _FavoritosScreenState();
}

class _FavoritosScreenState extends State<FavoritosScreen> {
  bool _iniciado = false;
  final _searchCtrl = TextEditingController();
  String _busqueda = '';

  @override
  void initState() {
    super.initState();
    _searchCtrl.addListener(() => setState(() => _busqueda = _searchCtrl.text));
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_iniciado) {
      _iniciado = true;
      _cargarFavoritos();
    }
  }

  Future<void> _cargarFavoritos() async {
    final auth = context.read<AuthProvider>();
    final favoritosProvider = context.read<FavoritosProvider>();

    if (auth.token != null) {
      await favoritosProvider.cargar(auth.token!);
    }
  }

  List<RecetaFavorita> get _favoritosFiltrados {
    final provider = context.watch<FavoritosProvider>();
    if (_busqueda.isEmpty) return provider.favoritos;
    return provider.favoritos
        .where(
          (f) =>
              f.receta.nombre.toLowerCase().contains(_busqueda.toLowerCase()),
        )
        .toList();
  }

  @override
  Widget build(BuildContext context) {
    final favoritosProvider = context.watch<FavoritosProvider>();
    final auth = context.watch<AuthProvider>();
    final favoritos = _favoritosFiltrados;

    if (favoritosProvider.error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 64, color: Colors.red),
            const SizedBox(height: 16),
            Text(
              'Error al cargar favoritos',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(
              favoritosProvider.error!,
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: _cargarFavoritos,
              icon: const Icon(Icons.refresh),
              label: const Text('Reintentar'),
            ),
          ],
        ),
      );
    }

    if (favoritos.isEmpty) {
      return Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(12),
            child: SearchBar(
              controller: _searchCtrl,
              hintText: 'Buscar favoritos…',
              leading: const Icon(Icons.search),
              trailing: [
                if (_busqueda.isNotEmpty)
                  IconButton(
                    icon: const Icon(Icons.clear),
                    onPressed: () => _searchCtrl.clear(),
                  ),
              ],
            ),
          ),
          Expanded(
            child: EmptyState(
              titulo: _busqueda.isEmpty ? 'Sin favoritos' : 'Sin resultados',
              subtitulo: 'Marca recetas con la estrella para verlas aquí',
              onRecargar: _cargarFavoritos,
            ),
          ),
        ],
      );
    }

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: SearchBar(
            controller: _searchCtrl,
            hintText: 'Buscar favoritos…',
            leading: const Icon(Icons.search),
            trailing: [
              if (_busqueda.isNotEmpty)
                IconButton(
                  icon: const Icon(Icons.clear),
                  onPressed: () => _searchCtrl.clear(),
                ),
            ],
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: _cargarFavoritos,
            child: ListView.builder(
              physics: const AlwaysScrollableScrollPhysics(),
              itemCount: favoritos.length,
              itemBuilder: (context, index) {
                final favoritoItem = favoritos[index];
                final receta = favoritoItem.receta;

                return RecetaCard(
                  receta: receta,
                  onTap: () => _irDetalle(context, receta.id),
                  onEliminar: auth.esJefeCocina
                      ? () => _confirmarEliminarReceta(receta.id, receta.nombre)
                      : null,
                );
              },
            ),
          ),
        ),
      ],
    );
  }

  void _irDetalle(BuildContext context, int id) {
    context.read<RecetasProvider>().seleccionar(
      id,
      context.read<AuthProvider>().token!,
    );
    Navigator.pushNamed(context, '/recetas/detalle', arguments: id);
  }

  Future<void> _confirmarEliminarReceta(int id, String nombre) async {
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Eliminar receta'),
        content: Text('¿Eliminar "$nombre" de favoritos?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancelar'),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Eliminar', style: TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );

    if (confirmar == true && mounted) {
      final auth = context.read<AuthProvider>();
      final favoritosProvider = context.read<FavoritosProvider>();

      try {
        await favoritosProvider.desmarcar(recetaId: id, token: auth.token!);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('$nombre eliminada de favoritos')),
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }
}
