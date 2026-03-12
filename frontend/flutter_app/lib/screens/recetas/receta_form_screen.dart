// lib/screens/recetas/receta_form_screen.dart

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../models/receta.dart';
import '../../models/ingrediente.dart';
import '../../providers/auth_provider.dart';
import '../../providers/recetas_provider.dart';
import '../../providers/ingredientes_provider.dart';
import '../../widgets/loading_widget.dart';

/// Formulario para crear o editar una [Receta].
///
/// Solo accesible para el jefe de cocina.
///
/// Secciones:
/// 1. Datos básicos (nombre + descripción).
/// 2. Ingredientes: lista dinámica con selector + cantidad.
/// 3. Pasos: lista ordenable con drag-and-drop.
class RecetaFormScreen extends StatefulWidget {
  const RecetaFormScreen({super.key});

  @override
  State<RecetaFormScreen> createState() => _RecetaFormScreenState();
}

class _RecetaFormScreenState extends State<RecetaFormScreen> {
  final _formKey       = GlobalKey<FormState>();
  final _nombreCtrl    = TextEditingController();
  final _descCtrl      = TextEditingController();

  // Ingredientes de la receta: {ingredienteId, nombre, unidad, cantidad}
  final List<_IngRow> _ingredientes = [];

  // Pasos de la receta (texto libre, orden = índice + 1)
  final List<TextEditingController> _pasos = [];

  Receta? _receta; // null = modo creación
  bool get _esEdicion => _receta != null;
  bool _guardando = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final arg = ModalRoute.of(context)?.settings.arguments;
    if (arg is Receta && _receta == null) {
      _receta = arg;
      _nombreCtrl.text = arg.nombre;
      _descCtrl.text   = arg.descripcion;

      for (final ri in arg.ingredientes) {
        _ingredientes.add(_IngRow(
          ingredienteId: ri.ingrediente.id,
          nombre:        ri.ingrediente.nombre,
          unidad:        ri.ingrediente.unidadMedida,
          cantidadCtrl:  TextEditingController(
              text: ri.cantidadNecesaria.toString()),
        ));
      }

      final pasos = [...arg.pasos]
        ..sort((a, b) => a.orden.compareTo(b.orden));
      for (final p in pasos) {
        _pasos.add(TextEditingController(text: p.descripcion));
      }
    }
  }

  @override
  void dispose() {
    _nombreCtrl.dispose();
    _descCtrl.dispose();
    for (final r in _ingredientes) r.cantidadCtrl.dispose();
    for (final c in _pasos) c.dispose();
    super.dispose();
  }

  // ─── Guardar ────────────────────────────────────────────────

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;
    if (_pasos.isEmpty) {
      _mostrarError('Añade al menos un paso a la receta.');
      return;
    }
    if (_ingredientes.isEmpty) {
      _mostrarError('Añade al menos un ingrediente a la receta.');
      return;
    }

    setState(() => _guardando = true);

    final provider = context.read<RecetasProvider>();
    final token    = context.read<AuthProvider>().token!;

    final body = {
      'nombre':       _nombreCtrl.text.trim(),
      'descripcion':  _descCtrl.text.trim(),
      'creadaPorId':  context.read<AuthProvider>().usuarioId,
      'ingredientes': _ingredientes.map((r) => {
        'ingredienteId':    r.ingredienteId,
        'cantidadNecesaria': double.parse(r.cantidadCtrl.text),
      }).toList(),
      'pasos': _pasos.asMap().entries.map((e) => {
        'orden':       e.key + 1,
        'descripcion': e.value.text.trim(),
      }).toList(),
    };

    try {
      if (_esEdicion) {
        await provider.actualizar(_receta!.id, body, token);
      } else {
        await provider.crear(body, token);
      }
      if (mounted) Navigator.pop(context);
    } catch (_) {
      if (mounted) _mostrarError('Error al guardar la receta.');
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  void _mostrarError(String msg) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(msg),
        backgroundColor: Theme.of(context).colorScheme.error,
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(16),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      ),
    );
  }

  // ─── Ingredientes ────────────────────────────────────────────

  void _agregarIngrediente(Ingrediente ing) {
    if (_ingredientes.any((r) => r.ingredienteId == ing.id)) return;
    setState(() {
      _ingredientes.add(_IngRow(
        ingredienteId: ing.id,
        nombre:        ing.nombre,
        unidad:        ing.unidadMedida,
        cantidadCtrl:  TextEditingController(text: '1'),
      ));
    });
  }

  void _eliminarIngrediente(int index) {
    _ingredientes[index].cantidadCtrl.dispose();
    setState(() => _ingredientes.removeAt(index));
  }

  Future<void> _mostrarSelectorIngrediente() async {
    final todos = context.read<IngredientesProvider>().ingredientes;
    final disponibles = todos
        .where((i) => !_ingredientes.any((r) => r.ingredienteId == i.id))
        .toList();

    if (disponibles.isEmpty) {
      _mostrarError('No hay más ingredientes disponibles.');
      return;
    }

    await showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(20))),
      builder: (_) => _SelectorIngredienteSheet(
        ingredientes: disponibles,
        onSeleccionar: _agregarIngrediente,
      ),
    );
  }

  // ─── Pasos ───────────────────────────────────────────────────

  void _agregarPaso() {
    setState(() => _pasos.add(TextEditingController()));
  }

  void _eliminarPaso(int index) {
    _pasos[index].dispose();
    setState(() => _pasos.removeAt(index));
  }

  // ─── UI ──────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_esEdicion ? 'Editar receta' : 'Nueva receta'),
        actions: [
          IconButton(
            onPressed: _guardando ? null : _guardar,
            icon: const Icon(Icons.check_rounded),
            tooltip: 'Guardar',
          ),
        ],
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: FilledButton.icon(
            onPressed: _guardando ? null : _guardar,
            style: FilledButton.styleFrom(
              minimumSize: const Size.fromHeight(56),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            ),
            icon: _guardando
                ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Icon(Icons.save_rounded),
            label: Text(_esEdicion ? 'Guardar Cambios' : 'Crear Receta'),
          ),
        ),
      ),
      body: Stack(
        children: [
          Form(
            key: _formKey,
            child: ListView(
              padding: const EdgeInsets.all(20),
              children: [
                _SeccionTitulo('Datos básicos'),
                const SizedBox(height: 12),
                _campoDatos(),
                const SizedBox(height: 28),

                _SeccionTitulo(
                  'Ingredientes',
                  accion: TextButton.icon(
                    onPressed: _mostrarSelectorIngrediente,
                    icon:  const Icon(Icons.add_rounded, size: 18),
                    label: const Text('Añadir'),
                  ),
                ),
                const SizedBox(height: 8),
                _listaIngredientes(),
                const SizedBox(height: 28),

                _SeccionTitulo(
                  'Pasos',
                  accion: TextButton.icon(
                    onPressed: _agregarPaso,
                    icon:  const Icon(Icons.add_rounded, size: 18),
                    label: const Text('Añadir paso'),
                  ),
                ),
                const SizedBox(height: 8),
                _listaPasos(),
                const SizedBox(height: 40),
              ],
            ),
          ),
          if (_guardando) const Positioned.fill(child: LoadingWidget.overlay()),
        ],
      ),
    );
  }

  Widget _campoDatos() => Column(
    children: [
      TextFormField(
        controller:         _nombreCtrl,
        textCapitalization: TextCapitalization.sentences,
        textInputAction:    TextInputAction.next,
        decoration: const InputDecoration(
          labelText:  'Nombre de la receta',
          prefixIcon: Icon(Icons.menu_book_outlined),
        ),
        validator: (v) =>
            v == null || v.trim().isEmpty ? 'Campo obligatorio' : null,
      ),
      const SizedBox(height: 14),
      TextFormField(
        controller:         _descCtrl,
        textCapitalization: TextCapitalization.sentences,
        maxLines:           3,
        decoration: const InputDecoration(
          labelText:   'Descripción',
          prefixIcon:  Icon(Icons.notes_rounded),
          alignLabelWithHint: true,
        ),
        validator: (v) =>
            v == null || v.trim().isEmpty ? 'Campo obligatorio' : null,
      ),
    ],
  );

  Widget _listaIngredientes() {
    if (_ingredientes.isEmpty) {
      return _PlaceholderVacio(
        icono: Icons.kitchen_outlined,
        texto: 'Sin ingredientes. Pulsa "Añadir".',
      );
    }
    return Column(
      children: _ingredientes.asMap().entries.map((entry) {
        final i   = entry.key;
        final row = entry.value;
        return _IngredienteRow(
          row:       row,
          onEliminar: () => _eliminarIngrediente(i),
        );
      }).toList(),
    );
  }

  Widget _listaPasos() {
    if (_pasos.isEmpty) {
      return _PlaceholderVacio(
        icono: Icons.format_list_numbered_rounded,
        texto: 'Sin pasos. Pulsa "Añadir paso".',
      );
    }
    return ReorderableListView.builder(
      shrinkWrap:  true,
      physics:     const NeverScrollableScrollPhysics(),
      itemCount:   _pasos.length,
      onReorder:   (oldIndex, newIndex) {
        if (newIndex > oldIndex) newIndex--;
        setState(() {
          final item = _pasos.removeAt(oldIndex);
          _pasos.insert(newIndex, item);
        });
      },
      itemBuilder: (context, i) => _PasoRow(
        key:       ValueKey(_pasos[i]),
        numero:    i + 1,
        ctrl:      _pasos[i],
        onEliminar: () => _eliminarPaso(i),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Subwidgets
// ─────────────────────────────────────────────────────────────────────────────

class _SeccionTitulo extends StatelessWidget {
  final String titulo;
  final Widget? accion;
  const _SeccionTitulo(this.titulo, {this.accion});

  @override
  Widget build(BuildContext context) {
    final tt = Theme.of(context).textTheme;
    return Row(
      children: [
        Text(titulo,
            style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
        const Spacer(),
        if (accion != null) accion!,
      ],
    );
  }
}

class _PlaceholderVacio extends StatelessWidget {
  final IconData icono;
  final String   texto;
  const _PlaceholderVacio({required this.icono, required this.texto});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: cs.surfaceContainerHighest.withOpacity(0.4),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: cs.outlineVariant),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icono, color: cs.onSurfaceVariant, size: 20),
          const SizedBox(width: 10),
          Text(texto,
              style: TextStyle(color: cs.onSurfaceVariant, fontSize: 13)),
        ],
      ),
    );
  }
}

class _IngredienteRow extends StatelessWidget {
  final _IngRow row;
  final VoidCallback onEliminar;
  const _IngredienteRow({required this.row, required this.onEliminar});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          children: [
            Icon(Icons.circle, size: 8, color: cs.primary),
            const SizedBox(width: 10),
            Expanded(
              child: Text(row.nombre,
                  style: const TextStyle(fontWeight: FontWeight.w500)),
            ),
            SizedBox(
              width: 80,
              child: TextFormField(
                controller:   row.cantidadCtrl,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                textAlign:    TextAlign.center,
                decoration: InputDecoration(
                  isDense:    true,
                  suffixText: row.unidad,
                  contentPadding: const EdgeInsets.symmetric(
                      horizontal: 8, vertical: 8),
                ),
                validator: (v) {
                  if (v == null || v.isEmpty) return '';
                  if (double.tryParse(v) == null) return '';
                  if (double.parse(v) <= 0) return '';
                  return null;
                },
              ),
            ),
            IconButton(
              icon: Icon(Icons.close_rounded, color: cs.error, size: 18),
              onPressed: onEliminar,
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(),
            ),
          ],
        ),
      ),
    );
  }
}

class _PasoRow extends StatelessWidget {
  final int numero;
  final TextEditingController ctrl;
  final VoidCallback onEliminar;
  const _PasoRow({
    super.key,
    required this.numero,
    required this.ctrl,
    required this.onEliminar,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Handle drag
            Padding(
              padding: const EdgeInsets.only(top: 12, right: 8),
              child: Icon(Icons.drag_handle_rounded,
                  color: cs.onSurfaceVariant, size: 20),
            ),
            // Número
            Padding(
              padding: const EdgeInsets.only(top: 10, right: 10),
              child: CircleAvatar(
                radius: 12,
                backgroundColor: cs.primaryContainer,
                child: Text('$numero',
                    style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: cs.onPrimaryContainer)),
              ),
            ),
            // Campo texto
            Expanded(
              child: TextFormField(
                controller:         ctrl,
                textCapitalization: TextCapitalization.sentences,
                maxLines:           null,
                decoration: const InputDecoration(
                  hintText:       'Describe este paso…',
                  border:         InputBorder.none,
                  isDense:        true,
                  contentPadding: EdgeInsets.symmetric(vertical: 10),
                ),
                validator: (v) =>
                    v == null || v.trim().isEmpty ? 'Campo obligatorio' : null,
              ),
            ),
            // Eliminar
            IconButton(
              icon: Icon(Icons.close_rounded, color: cs.error, size: 18),
              onPressed: onEliminar,
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(),
            ),
          ],
        ),
      ),
    );
  }
}

/// BottomSheet para seleccionar un ingrediente del inventario.
class _SelectorIngredienteSheet extends StatefulWidget {
  final List<Ingrediente> ingredientes;
  final ValueChanged<Ingrediente> onSeleccionar;
  const _SelectorIngredienteSheet({
    required this.ingredientes,
    required this.onSeleccionar,
  });

  @override
  State<_SelectorIngredienteSheet> createState() =>
      _SelectorIngredienteSheetState();
}

class _SelectorIngredienteSheetState
    extends State<_SelectorIngredienteSheet> {
  String _filtro = '';

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final filtrados = widget.ingredientes
        .where((i) =>
            i.nombre.toLowerCase().contains(_filtro.toLowerCase()))
        .toList();

    return DraggableScrollableSheet(
      expand: false,
      initialChildSize: 0.6,
      maxChildSize: 0.9,
      builder: (_, ctrl) => Column(
        children: [
          // Handle
          Container(
            margin: const EdgeInsets.symmetric(vertical: 10),
            width: 40, height: 4,
            decoration: BoxDecoration(
              color: cs.outlineVariant,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: TextField(
              autofocus: true,
              decoration: const InputDecoration(
                hintText:  'Buscar ingrediente…',
                prefixIcon: Icon(Icons.search_rounded),
              ),
              onChanged: (v) => setState(() => _filtro = v),
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: ListView.builder(
              controller: ctrl,
              itemCount:  filtrados.length,
              itemBuilder: (_, i) {
                final ing = filtrados[i];
                return ListTile(
                  leading: const Icon(Icons.kitchen_outlined),
                  title:   Text(ing.nombre),
                  subtitle: Text(
                      '${ing.stockActual} ${ing.unidadMedida} disponibles'),
                  onTap: () {
                    widget.onSeleccionar(ing);
                    Navigator.pop(context);
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

/// Modelo interno de fila de ingrediente en el formulario.
class _IngRow {
  final int    ingredienteId;
  final String nombre;
  final String unidad;
  final TextEditingController cantidadCtrl;

  _IngRow({
    required this.ingredienteId,
    required this.nombre,
    required this.unidad,
    required this.cantidadCtrl,
  });
}