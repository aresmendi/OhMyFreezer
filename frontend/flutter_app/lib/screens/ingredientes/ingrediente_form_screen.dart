// lib/screens/ingredientes/ingrediente_form_screen.dart

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../models/ingrediente.dart';
import '../../models/unidad_medida.dart';
import '../../providers/auth_provider.dart';
import '../../providers/ingredientes_provider.dart';
import '../../providers/unidades_provider.dart';
import '../../widgets/loading_widget.dart';

/// Formulario para crear o editar un [Ingrediente].
///
/// Si se recibe un [Ingrediente] como argumento de ruta → modo edición.
/// Si no → modo creación.
///
/// La unidad de medida se elige de [UnidadesProvider] (catálogo global,
/// Fase 2 "unidades-medida") en vez de texto libre; el body enviado al
/// backend usa `unidadBaseId`, no el campo legado `unidadMedida`.
///
/// Solo accesible para el jefe de cocina.
class IngredienteFormScreen extends StatefulWidget {
  const IngredienteFormScreen({super.key});

  @override
  State<IngredienteFormScreen> createState() => _IngredienteFormScreenState();
}

class _IngredienteFormScreenState extends State<IngredienteFormScreen> {
  final _formKey      = GlobalKey<FormState>();
  final _nombreCtrl   = TextEditingController();
  final _stockCtrl    = TextEditingController();
  final _minimoCtrl   = TextEditingController();
  UnidadMedida? _unidad;
  bool   _guardando   = false;

  Ingrediente? _ingrediente; // null = modo creación
  bool get _esEdicion => _ingrediente != null;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final arg = ModalRoute.of(context)?.settings.arguments;
    if (arg is Ingrediente && _ingrediente == null) {
      _ingrediente = arg;
      _nombreCtrl.text = arg.nombre;
      _stockCtrl.text  = arg.stockActual.toString();
      _minimoCtrl.text = arg.stockMinimo.toString();
    }

    final token = context.read<AuthProvider>().token;
    if (token != null) {
      context.read<UnidadesProvider>().cargar(token);
    }
  }

  @override
  void dispose() {
    _nombreCtrl.dispose();
    _stockCtrl.dispose();
    _minimoCtrl.dispose();
    super.dispose();
  }

  /// Elige la unidad inicial una vez el catálogo terminó de cargar: en modo
  /// edición, la del ingrediente (por [Ingrediente.unidadBaseId] si viene
  /// informado, si no por el código legado [Ingrediente.unidadMedida]); en
  /// modo creación, "kg" si existe en el catálogo, si no la primera unidad.
  UnidadMedida _resolverUnidadInicial(List<UnidadMedida> unidades) {
    if (_esEdicion) {
      final ing = _ingrediente!;
      if (ing.unidadBaseId != null) {
        final porId = unidades.where((u) => u.id == ing.unidadBaseId);
        if (porId.isNotEmpty) return porId.first;
      }
      final porCodigo = unidades.where((u) => u.codigo == ing.unidadMedida);
      if (porCodigo.isNotEmpty) return porCodigo.first;
      return unidades.first;
    }
    final kg = unidades.where((u) => u.codigo == 'kg');
    return kg.isNotEmpty ? kg.first : unidades.first;
  }

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate() || _unidad == null) return;
    setState(() => _guardando = true);

    final provider = context.read<IngredientesProvider>();
    final token    = context.read<AuthProvider>().token!;
    final body = {
      'nombre':       _nombreCtrl.text.trim(),
      'cantidad':     double.parse(_stockCtrl.text),
      'stockMinimo':  double.parse(_minimoCtrl.text),
      'unidadBaseId': _unidad!.id,
    };

    try {
      if (_esEdicion) {
        await provider.actualizar(_ingrediente!.id, body, token);
      } else {
        await provider.crear(body, token);
      }
      if (mounted) Navigator.pop(context);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Error al guardar el ingrediente.')),
        );
      }
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final unidadesProvider = context.watch<UnidadesProvider>();
    final unidades = unidadesProvider.todas;

    if (_unidad == null && unidades.isNotEmpty) {
      _unidad = _resolverUnidadInicial(unidades);
    }

    final catalogoListo = unidades.isNotEmpty;

    return Scaffold(
      appBar: AppBar(
        title: Text(_esEdicion ? 'Editar ingrediente' : 'Nuevo ingrediente'),
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Nombre
                  TextFormField(
                    controller:     _nombreCtrl,
                    textCapitalization: TextCapitalization.sentences,
                    textInputAction: TextInputAction.next,
                    decoration: const InputDecoration(
                      labelText:  'Nombre',
                      prefixIcon: Icon(Icons.label_outline_rounded),
                    ),
                    validator: (v) =>
                        v == null || v.trim().isEmpty ? 'Campo obligatorio' : null,
                  ),
                  const SizedBox(height: 16),

                  // Stock actual + unidad
                  Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Expanded(
                        child: TextFormField(
                          controller:   _stockCtrl,
                          keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          textInputAction: TextInputAction.next,
                          decoration: const InputDecoration(
                            labelText:  'Stock actual',
                            prefixIcon: Icon(Icons.inventory_2_outlined),
                          ),
                          validator: _validarNumero,
                        ),
                      ),
                      const SizedBox(width: 12),
                      if (catalogoListo)
                        _SelectorUnidad(
                          valor:    _unidad,
                          opciones: unidades,
                          onChange: (v) => setState(() => _unidad = v),
                        )
                      else
                        const SizedBox(
                          height: 56, width: 56,
                          child: Center(
                            child: SizedBox(
                              height: 22, width: 22,
                              child: CircularProgressIndicator(strokeWidth: 2.5),
                            ),
                          ),
                        ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  // Stock mínimo
                  TextFormField(
                    controller:   _minimoCtrl,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    textInputAction: TextInputAction.done,
                    onFieldSubmitted: (_) => _guardar(),
                    decoration: const InputDecoration(
                      labelText:   'Stock mínimo',
                      prefixIcon:  Icon(Icons.warning_amber_outlined),
                      helperText:  'Se generará una alerta si el stock baja de este valor.',
                    ),
                    validator: _validarNumero,
                  ),
                  const SizedBox(height: 32),

                  FilledButton(
                    onPressed: (_guardando || _unidad == null) ? null : _guardar,
                    style: FilledButton.styleFrom(
                      minimumSize: const Size.fromHeight(52),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(14)),
                    ),
                    child: _guardando
                        ? SizedBox(
                            height: 22, width: 22,
                            child: CircularProgressIndicator(
                                strokeWidth: 2.5, color: cs.onPrimary),
                          )
                        : Text(_esEdicion ? 'Guardar cambios' : 'Crear ingrediente'),
                  ),
                ],
              ),
            ),
          ),
          if (_guardando) const Positioned.fill(child: LoadingWidget.overlay()),
        ],
      ),
    );
  }

  String? _validarNumero(String? v) {
    if (v == null || v.isEmpty) return 'Campo obligatorio';
    if (double.tryParse(v) == null) return 'Número no válido';
    if (double.parse(v) < 0) return 'No puede ser negativo';
    return null;
  }
}

class _SelectorUnidad extends StatelessWidget {
  final UnidadMedida? valor;
  final List<UnidadMedida> opciones;
  final ValueChanged<UnidadMedida> onChange;

  const _SelectorUnidad({
    required this.valor,
    required this.opciones,
    required this.onChange,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        border: Border.all(color: cs.outline),
        borderRadius: BorderRadius.circular(12),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<UnidadMedida>(
          value: valor,
          items: opciones
              .map((u) => DropdownMenuItem(value: u, child: Text(u.codigo)))
              .toList(),
          onChanged: (v) { if (v != null) onChange(v); },
        ),
      ),
    );
  }
}
