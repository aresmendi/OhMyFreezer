// lib/screens/recetas/receta_pasos_screen.dart

import 'package:flutter/material.dart';
import '../../models/receta.dart';

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
                    onPressed: _paginaActual < total - 1
                        ? () => _pageCtrl.nextPage(
                              duration: const Duration(milliseconds: 300),
                              curve: Curves.easeInOut,
                            )
                        : () => Navigator.pop(context),
                    icon: Icon(_paginaActual < total - 1
                        ? Icons.arrow_forward_rounded
                        : Icons.check_rounded),
                    label: Text(
                        _paginaActual < total - 1 ? 'Siguiente' : 'Finalizar'),
                    style: FilledButton.styleFrom(
                        minimumSize: const Size.fromHeight(50),
                        shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14))),
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