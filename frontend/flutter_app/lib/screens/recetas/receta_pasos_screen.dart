
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../models/receta.dart';
import '../../providers/auth_provider.dart';
import '../../providers/ingredientes_provider.dart';
import '../../providers/recetas_provider.dart';

/// Modo paso a paso para elaborar una receta.
///
/// Muestra un [PageView] con un paso por página.
/// El usuario avanza/retrocede con botones o swipe.
/// Al llegar al último paso muestra un botón "Finalizar".
class RecetaPasosScreen extends StatefulWidget {
  const RecetaPasosScreen({super.key});

  @override
  State<RecetaPasosScreen> createState() => _RecetaPasosScreenState();
}

class _RecetaPasosScreenState extends State<RecetaPasosScreen> {
  late final PageController _pageCtrl;
  int _paginaActual = 0;
  bool _isProcessing = false;

  @override
  void initState() {
    super.initState();
    _pageCtrl = PageController();
  }

  @override
  void dispose() {
    _pageCtrl.dispose();
    super.dispose();
  }

  Future<void> _onFinalizar() async {
    final receta = ModalRoute.of(context)!.settings.arguments as Receta;
    final auth = context.read<AuthProvider>();

    final resultado = await showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (context) => AlertDialog(
        title: const Text('¿Cómo fue la elaboración?'),
        content: const Text(
          'Selecciona el resultado de haber seguido los pasos de la receta.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, 'cancelar'),
            child: const Text('Cancelar'),
          ),
          OutlinedButton(
            onPressed: () => Navigator.pop(context, 'fallida'),
            child: const Text('Elaboración fallida'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, 'exitosa'),
            child: const Text('Elaboración exitosa'),
          ),
        ],
      ),
    );

    if (resultado == null || resultado == 'cancelar') {
      if (mounted) Navigator.pop(context);
      return;
    }

    setState(() => _isProcessing = true);

    try {
      final recetasProvider = context.read<RecetasProvider>();
      final ingredientesProvider = context.read<IngredientesProvider>();

      if (resultado == 'exitosa') {
        await recetasProvider.elaborar(
          receta.id,
          auth.usuarioId!,
          auth.token!,
        );
        await ingredientesProvider.cargar(auth.token!);
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Receta elaborada con éxito'),
              backgroundColor: Colors.green,
            ),
          );
        }
      } else {
        await recetasProvider.elaborar(
          receta.id,
          auth.usuarioId!,
          auth.token!,
          completada: false,
        );
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text('Registro guardado: elaboración fallida'),
              backgroundColor: Colors.orange,
            ),
          );
        }
      }

      if (mounted) Navigator.pop(context);
    } catch (e) {
      setState(() => _isProcessing = false);
      await context.read<IngredientesProvider>().cargar(auth.token!);
      if (mounted) {
        showDialog(
          context: context,
          builder: (context) => AlertDialog(
            title: const Text('Error'),
            content: Text('No se pudo completar la elaboración:\n$e'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Aceptar'),
              ),
            ],
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final receta = ModalRoute.of(context)!.settings.arguments as Receta;
    final pasos  = [...receta.pasos]..sort((a, b) => a.orden.compareTo(b.orden));
    final total  = pasos.length;
    final cs     = Theme.of(context).colorScheme;

    return Scaffold(
      appBar: AppBar(
        title: Text(receta.nombre),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(4),
          child: LinearProgressIndicator(
            value: total > 0 ? (_paginaActual + 1) / total : 0,
            backgroundColor: cs.surfaceContainerHighest,
            valueColor: AlwaysStoppedAnimation<Color>(cs.primary),
          ),
        ),
      ),
      body: Column(
        children: [
          // Contador de pasos
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 12),
            child: Text(
              'Paso ${_paginaActual + 1} de $total',
              style: Theme.of(context).textTheme.labelLarge?.copyWith(
                    color: cs.onSurfaceVariant,
                  ),
            ),
          ),

          // PageView de pasos
          Expanded(
            child: PageView.builder(
              controller: _pageCtrl,
              itemCount: total,
              onPageChanged: (i) => setState(() => _paginaActual = i),
              itemBuilder: (context, i) => _PasoPagina(
                paso:  pasos[i].descripcion,
                orden: pasos[i].orden,
              ),
            ),
          ),

          // Botones de navegación
          Padding(
            padding: const EdgeInsets.fromLTRB(24, 8, 24, 32),
            child: Row(
              children: [
                // Anterior
                if (_paginaActual > 0)
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () => _pageCtrl.previousPage(
                        duration: const Duration(milliseconds: 300),
                        curve: Curves.easeInOut,
                      ),
                      icon:  const Icon(Icons.arrow_back_rounded),
                      label: const Text('Anterior'),
                      style: OutlinedButton.styleFrom(
                          minimumSize: const Size.fromHeight(50)),
                    ),
                  ),
                if (_paginaActual > 0) const SizedBox(width: 12),

                // Siguiente / Finalizar
                 Expanded(
                  child: FilledButton.icon(
                    onPressed: _isProcessing
                        ? null
                        : _paginaActual < total - 1
                            ? () => _pageCtrl.nextPage(
                                  duration: const Duration(milliseconds: 300),
                                  curve: Curves.easeInOut,
                                )
                            : _onFinalizar,
                    icon: _isProcessing
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : Icon(_paginaActual < total - 1
                            ? Icons.arrow_forward_rounded
                            : Icons.check_rounded),
                    label: Text(_paginaActual < total - 1
                        ? 'Siguiente'
                        : 'Finalizar'),
                    style: FilledButton.styleFrom(
                      minimumSize: const Size.fromHeight(50),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14)),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PasoPagina extends StatelessWidget {
  final String paso;
  final int    orden;
  const _PasoPagina({required this.paso, required this.orden});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 32),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          CircleAvatar(
            radius: 36,
            backgroundColor: cs.primaryContainer,
            child: Text(
              '$orden',
              style: tt.headlineSmall?.copyWith(
                fontWeight: FontWeight.w800,
                color: cs.onPrimaryContainer,
              ),
            ),
          ),
          const SizedBox(height: 32),
          Text(
            paso,
            style: tt.bodyLarge?.copyWith(height: 1.6),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }
}