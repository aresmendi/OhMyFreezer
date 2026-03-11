// lib/screens/ingredientes/ingrediente_form_screen.dart

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../models/ingrediente.dart';
import '../../providers/auth_provider.dart';
import '../../providers/ingredientes_provider.dart';
import '../../widgets/loading_widget.dart';

/// Formulario para crear o editar un [Ingrediente].
///
/// Si se recibe un [Ingrediente] como argumento de ruta → modo edición.
/// Si no → modo creación.
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
  String _unidad      = 'kg';
  bool   _guardando   = false;

  Ingrediente? _ingrediente; // null = modo creación
  bool get _esEdicion => _ingrediente != null;

  static const _unidades = ['kg', 'g', 'L', 'ml', 'ud'];

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final arg = ModalRoute.of(context)?.settings.arguments;
    if (arg is Ingrediente && _ingrediente == null) {
      _ingrediente = arg;
      _nombreCtrl.text = arg.nombre;
      _stockCtrl.text  = arg.stockActual.toString();
      _minimoCtrl.text = arg.stockMinimo.toString();
      _unidad          = arg.unidadMedida;
    }
  }

  @override
  void dispose() {
    _nombreCtrl.dispose();
    _stockCtrl.dispose();
    _minimoCtrl.dispose();
    super.dispose();
  }

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _guardando = true);

    final provider = context.read<IngredientesProvider>();
    final token    = context.read<AuthProvider>().token!;
    final body = {
      'nombre':       _nombreCtrl.text.trim(),
      'stockActual':  double.parse(_stockCtrl.text),
      'stockMinimo':  double.parse(_minimoCtrl.text),
      'unidadMedida': _unidad,
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
                      _SelectorUnidad(
                        valor:    _unidad,
                        opciones: _unidades,
                        onChange: (v) => setState(() => _unidad = v),
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
                    onPressed: _guardando ? null : _guardar,
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
  final String valor;
  final List<String> opciones;
  final ValueChanged<String> onChange;

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
        child: DropdownButton<String>(
          value: valor,
          items: opciones
              .map((u) => DropdownMenuItem(value: u, child: Text(u)))
              .toList(),
          onChanged: (v) { if (v != null) onChange(v); },
        ),
      ),
    );
  }
}