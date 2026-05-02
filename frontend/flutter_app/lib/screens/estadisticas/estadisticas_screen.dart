import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:intl/intl.dart';

import '../../models/estadistica.dart';
import '../../models/ingrediente.dart';
import '../../models/movimiento_stock.dart';
import '../../providers/auth_provider.dart';
import '../../providers/estadisticas_provider.dart';
import '../../providers/ingredientes_provider.dart';
import '../../services/movimiento_stock_service.dart';
import '../../widgets/loading_widget.dart';
import '../home_screen.dart';

enum TipoGrafico { lineas, barras }

class EstadisticasScreen extends StatefulWidget {
  const EstadisticasScreen({super.key});

  @override
  State<EstadisticasScreen> createState() => _EstadisticasScreenState();
}

class _EstadisticasScreenState extends State<EstadisticasScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Estadísticas'),
        bottom: TabBar(
          controller: _tabController,
          labelColor: Theme.of(context).colorScheme.secondary,
          unselectedLabelColor: Theme.of(context).colorScheme.onSurfaceVariant,
          indicatorColor: Theme.of(context).colorScheme.secondary,
          tabs: const [
            Tab(text: 'Recetas'),
            Tab(text: 'Ingredientes'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: const [_RecetasTab(), _IngredientesTab()],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB DE RECETAS
// ─────────────────────────────────────────────────────────────────────────────

class _RecetasTab extends StatelessWidget {
  const _RecetasTab();

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<EstadisticasProvider>();
    final auth = context.read<AuthProvider>();

    if (provider.isLoading && provider.estadisticas.isEmpty) {
      return const LoadingWidget.inline(mensaje: 'Cargando estadísticas…');
    }

    if (provider.estadisticas.isEmpty) {
      return EmptyState(
        titulo: 'Sin estadísticas',
        subtitulo: 'Aún no se ha elaborado ninguna receta.',
        onRecargar: () => provider.cargar(auth.token!),
      );
    }

    final stats = provider.estadisticas;
    final total = stats.fold<int>(0, (s, e) => s + e.totalElaboraciones);
    final top = stats.reduce(
      (a, b) => a.totalElaboraciones >= b.totalElaboraciones ? a : b,
    );

    return RefreshIndicator(
      onRefresh: () => provider.cargar(auth.token!),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _TarjetaResumen(totalElaboraciones: total, recetaTop: top),
          const SizedBox(height: 20),
          Text(
            'Por receta',
            style: Theme.of(
              context,
            ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700),
          ),
          const SizedBox(height: 12),
          ...stats.sorted().map((e) => _EstadisticaCard(estadistica: e)),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// TAB DE INGREDIENTES
// ─────────────────────────────────────────────────────────────────────────────

class _IngredientesTab extends StatefulWidget {
  const _IngredientesTab();

  @override
  State<_IngredientesTab> createState() => _IngredientesTabState();
}

class _IngredientesTabState extends State<_IngredientesTab> {
  List<MovimientoStock> _movimientos = [];
  bool _isLoading = false;
  String? _error;
  Set<int> _ingredientesSeleccionados = {};
  TipoGrafico _tipoGrafico = TipoGrafico.lineas;
  late DateTime _fechaDesde;
  late DateTime _fechaHasta;

  _IngredientesTabState() {
    final now = DateTime.now();
    _fechaDesde = DateTime(now.year, now.month, now.day - 30);
    _fechaHasta = DateTime(now.year, now.month, now.day, 23, 59, 59);
  }

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _inicializar();
    });
  }

  void _inicializar() {
    final ingredientes = context.read<IngredientesProvider>().ingredientes;
    if (ingredientes.isNotEmpty) {
      setState(() {
        _ingredientesSeleccionados = ingredientes
            .take(3)
            .map((i) => i.id)
            .toSet();
      });
      _cargarMovimientos();
    }
  }

  Future<void> _cargarMovimientos() async {
    if (_ingredientesSeleccionados.isEmpty) {
      setState(() {
        _movimientos = [];
        _error = null;
      });
      return;
    }

    setState(() => _isLoading = true);

    try {
      final auth = context.read<AuthProvider>();
      final movimientos = await MovimientoStockService.obtenerMovimientos(
        ingredienteIds: _ingredientesSeleccionados.toList(),
        fechaDesde: _fechaDesde,
        fechaHasta: _fechaHasta,
        token: auth.token!,
      );
      setState(() {
        _movimientos = movimientos;
        _isLoading = false;
        _error = null;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        _error = e.toString();
      });
    }
  }

  Future<void> _seleccionarIngredientes() async {
    final ingredientes = context.read<IngredientesProvider>().ingredientes;
    final seleccionados = await showDialog<Set<int>>(
      context: context,
      builder: (context) => _SelectorIngredientesDialog(
        ingredientes: ingredientes,
        seleccionados: _ingredientesSeleccionados,
        maxSeleccionar: 6,
      ),
    );

    if (seleccionados != null) {
      setState(() => _ingredientesSeleccionados = seleccionados);
      _cargarMovimientos();
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Controles
        Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              Expanded(
                child: SegmentedButton<TipoGrafico>(
                  segments: const [
                    ButtonSegment(
                      value: TipoGrafico.lineas,
                      label: Text('Líneas'),
                    ),
                    ButtonSegment(
                      value: TipoGrafico.barras,
                      label: Text('Barras'),
                    ),
                  ],
                  selected: {_tipoGrafico},
                  onSelectionChanged: (v) =>
                      setState(() => _tipoGrafico = v.first),
                ),
              ),
              const SizedBox(width: 12),
              IconButton.filled(
                onPressed: _seleccionarIngredientes,
                icon: const Icon(Icons.filter_list),
                tooltip: 'Seleccionar ingredientes',
              ),
              IconButton.filled(
                onPressed: _cargarMovimientos,
                icon: const Icon(Icons.refresh),
                tooltip: 'Recargar',
              ),
            ],
          ),
        ),

        // Selector de fechas
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => _seleccionarFecha(true),
                  icon: const Icon(Icons.calendar_today, size: 16),
                  label: Text(DateFormat('dd/MM/yy').format(_fechaDesde)),
                ),
              ),
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 8),
                child: Text('hasta'),
              ),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => _seleccionarFecha(false),
                  icon: const Icon(Icons.calendar_today, size: 16),
                  label: Text(DateFormat('dd/MM/yy').format(_fechaHasta)),
                ),
              ),
            ],
          ),
        ),

        const SizedBox(height: 12),

        // Gráfico
        Expanded(
          child: _isLoading
              ? const LoadingWidget.inline(mensaje: 'Cargando movimientos…')
              : _error != null
              ? Center(child: Text('Error: $_error'))
              : _movimientos.isEmpty
              ? const EmptyState(
                  titulo: 'Sin movimientos',
                  subtitulo: 'No hay registros en el período seleccionado',
                )
              : Padding(
                  padding: const EdgeInsets.all(16),
                  child: _buildGrafico(),
                ),
        ),
      ],
    );
  }

  Widget _buildGrafico() {
    if (_movimientos.isEmpty) return const SizedBox();

    final ingredientes = _ingredientesSeleccionados.toList();
    final colores = [
      Colors.blue,
      Colors.red,
      Colors.green,
      Colors.orange,
      Colors.purple,
      Colors.teal,
    ];

    if (_tipoGrafico == TipoGrafico.lineas) {
      return _buildGraficoLineas(ingredientes, colores);
    } else {
      return _buildGraficoBarras(ingredientes, colores);
    }
  }

  Widget _buildGraficoLineas(List<int> ingredientes, List<Color> colores) {
    final spotsPorIngrediente = <int, List<FlSpot>>{};
    final datosPorIngrediente = <int, Map<DateTime, double>>{};
    final todasLasFechas = <DateTime>[];

    for (final m in _movimientos) {
      datosPorIngrediente.putIfAbsent(m.ingredienteId, () => {});
      final mapa = datosPorIngrediente[m.ingredienteId]!;
      mapa[m.fecha] = m.cantidadNueva;
      if (!todasLasFechas.contains(m.fecha)) {
        todasLasFechas.add(m.fecha);
      }
    }

    todasLasFechas.sort();
    final fechaToIndex = <DateTime, int>{};
    for (int i = 0; i < todasLasFechas.length; i++) {
      fechaToIndex[todasLasFechas[i]] = i;
    }

    for (final id in ingredientes) {
      if (datosPorIngrediente.containsKey(id)) {
        final mapa = datosPorIngrediente[id]!;
        final sortedEntries = mapa.entries.toList()
          ..sort((a, b) => a.key.compareTo(b.key));
        spotsPorIngrediente[id] = sortedEntries.map((e) {
          final fechaIndex = fechaToIndex[e.key]!.toDouble();
          return FlSpot(fechaIndex, e.value);
        }).toList();
      }
    }

    final lineas = <LineChartBarData>[];
    int idx = 0;
    for (final id in ingredientes) {
      final spots = spotsPorIngrediente[id];
      if (spots != null && spots.isNotEmpty) {
        lineas.add(
          LineChartBarData(
            spots: spots,
            isCurved: true,
            color: colores[idx % colores.length],
            barWidth: 3,
            dotData: const FlDotData(show: false),
          ),
        );
      }
      idx++;
    }

    return Column(
      children: [
        // Leyenda de ingredientes
        _buildLeyendaLineas(ingredientes, colores),
        const SizedBox(height: 8),
        Expanded(
          child: LineChart(
            LineChartData(
              minY: 0,
              lineBarsData: lineas,
              titlesData: _buildLineasTitlesData(todasLasFechas),
              borderData: FlBorderData(show: true),
              gridData: const FlGridData(show: true),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildLeyendaLineas(List<int> ingredientes, List<Color> colores) {
    final ingredientesProvider = context.read<IngredientesProvider>();
    return Wrap(
      spacing: 12,
      runSpacing: 4,
      children: List.generate(ingredientes.length, (idx) {
        final id = ingredientes[idx];
        final ing = ingredientesProvider.ingredientes
            .where((i) => i.id == id)
            .firstOrNull;
        return Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 12,
              height: 12,
              decoration: BoxDecoration(
                color: colores[idx % colores.length],
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(width: 4),
            Text(ing?.nombre ?? '', style: const TextStyle(fontSize: 11)),
          ],
        );
      }),
    );
  }

  FlTitlesData _buildLineasTitlesData(List<DateTime> fechas) {
    return FlTitlesData(
      bottomTitles: AxisTitles(
        sideTitles: SideTitles(
          showTitles: true,
          reservedSize: 30,
          interval: (fechas.length / 5).ceilToDouble().clamp(
            1,
            double.infinity,
          ),
          getTitlesWidget: (value, meta) {
            final idx = value.toInt();
            if (idx >= 0 && idx < fechas.length) {
              final fecha = fechas[idx];
              return Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  '${fecha.day}/${fecha.month}',
                  style: const TextStyle(fontSize: 9),
                ),
              );
            }
            return const Text('');
          },
        ),
      ),
      leftTitles: AxisTitles(
        sideTitles: SideTitles(
          showTitles: true,
          interval: 1,
          reservedSize: 40,
          getTitlesWidget: (value, meta) => Text(
            value.toStringAsFixed(0),
            style: const TextStyle(fontSize: 10),
          ),
        ),
      ),
      topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
      rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
    );
  }

  Widget _buildGraficoBarras(List<int> ingredientes, List<Color> colores) {
    final grupos = <int, Map<String, double>>{};

    for (final m in _movimientos) {
      grupos.putIfAbsent(m.ingredienteId, () => {'entrada': 0, 'salida': 0});
      if (m.tipo == 'ENTRADA') {
        grupos[m.ingredienteId]!['entrada'] =
            (grupos[m.ingredienteId]!['entrada'] ?? 0) + m.cantidadCambio.abs();
      } else {
        grupos[m.ingredienteId]!['salida'] =
            (grupos[m.ingredienteId]!['salida'] ?? 0) + m.cantidadCambio.abs();
      }
    }

    final barGroups = <BarChartGroupData>[];
    int idx = 0;
    for (final id in ingredientes) {
      if (grupos.containsKey(id)) {
        final datos = grupos[id]!;
        barGroups.add(
          BarChartGroupData(
            x: idx,
            barRods: [
              BarChartRodData(
                toY: datos['entrada'] ?? 0,
                color: Colors.green,
                width: 12,
                borderRadius: const BorderRadius.vertical(
                  top: Radius.circular(4),
                ),
              ),
              BarChartRodData(
                toY: datos['salida'] ?? 0,
                color: Colors.red,
                width: 12,
                borderRadius: const BorderRadius.vertical(
                  top: Radius.circular(4),
                ),
              ),
            ],
          ),
        );
      }
      idx++;
    }

    return Column(
      children: [
        // Leyenda de Entrada/Salida
        _buildLeyendaBarras(),
        const SizedBox(height: 8),
        Expanded(
          child: BarChart(
            BarChartData(
              minY: 0,
              barGroups: barGroups,
              titlesData: _buildBarTitlesData(ingredientes, colores),
              borderData: FlBorderData(show: true),
              gridData: const FlGridData(show: true),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildLeyendaBarras() {
    return const Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(
              width: 12,
              height: 12,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: Colors.green,
                  borderRadius: BorderRadius.all(Radius.circular(2)),
                ),
              ),
            ),
            SizedBox(width: 4),
            Text('Entradas', style: TextStyle(fontSize: 11)),
          ],
        ),
        SizedBox(width: 24),
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            SizedBox(
              width: 12,
              height: 12,
              child: DecoratedBox(
                decoration: BoxDecoration(
                  color: Colors.red,
                  borderRadius: BorderRadius.all(Radius.circular(2)),
                ),
              ),
            ),
            SizedBox(width: 4),
            Text('Salidas', style: TextStyle(fontSize: 11)),
          ],
        ),
      ],
    );
  }

  FlTitlesData _buildBarTitlesData(
    List<int> ingredientes,
    List<Color> colores,
  ) {
    final ingredientesProvider = context.read<IngredientesProvider>();
    return FlTitlesData(
      bottomTitles: AxisTitles(
        sideTitles: SideTitles(
          showTitles: true,
          reservedSize: 40,
          getTitlesWidget: (value, meta) {
            final idx = value.toInt();
            if (idx >= 0 && idx < ingredientes.length) {
              final ing = ingredientesProvider.ingredientes
                  .where((i) => i.id == ingredientes[idx])
                  .firstOrNull;
              return Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  ing?.nombre ?? '',
                  style: const TextStyle(fontSize: 9),
                  overflow: TextOverflow.ellipsis,
                ),
              );
            }
            return const Text('');
          },
        ),
      ),
      leftTitles: AxisTitles(
        sideTitles: SideTitles(
          showTitles: true,
          interval: 1,
          reservedSize: 40,
          getTitlesWidget: (value, meta) => Text(
            value.toStringAsFixed(0),
            style: const TextStyle(fontSize: 10),
          ),
        ),
      ),
      topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
      rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
    );
  }

  Future<void> _seleccionarFecha(bool esDesde) async {
    final fecha = await showDatePicker(
      context: context,
      initialDate: esDesde ? _fechaDesde : _fechaHasta,
      firstDate: DateTime(2020),
      lastDate: DateTime.now(),
    );
    if (fecha != null) {
      setState(() {
        if (esDesde) {
          _fechaDesde = fecha;
        } else {
          _fechaHasta = fecha;
        }
      });
      _cargarMovimientos();
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────

class _SelectorIngredientesDialog extends StatefulWidget {
  final List<Ingrediente> ingredientes;
  final Set<int> seleccionados;
  final int maxSeleccionar;

  const _SelectorIngredientesDialog({
    required this.ingredientes,
    required this.seleccionados,
    this.maxSeleccionar = 6,
  });

  @override
  State<_SelectorIngredientesDialog> createState() =>
      _SelectorIngredientesDialogState();
}

class _SelectorIngredientesDialogState
    extends State<_SelectorIngredientesDialog> {
  late Set<int> _seleccionados;

  @override
  void initState() {
    super.initState();
    _seleccionados = {...widget.seleccionados};
  }

  @override
  Widget build(BuildContext context) {
    final puedeSeleccionarMas = _seleccionados.length < widget.maxSeleccionar;

    return AlertDialog(
      title: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text('Seleccionar ingredientes'),
          Text(
            'Máximo ${widget.maxSeleccionar} ingredientes',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
      content: SizedBox(
        width: double.maxFinite,
        height: 400,
        child: ListView.builder(
          itemCount: widget.ingredientes.length,
          itemBuilder: (context, i) {
            final ing = widget.ingredientes[i];
            final isSelected = _seleccionados.contains(ing.id);
            return CheckboxListTile(
              title: Text(ing.nombre),
              subtitle: Text(ing.unidadMedida),
              value: isSelected,
              onChanged: (v) {
                setState(() {
                  if (v == true && !puedeSeleccionarMas) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text(
                          'Máximo ${widget.maxSeleccionar} ingredientes',
                        ),
                        duration: const Duration(seconds: 2),
                      ),
                    );
                    return;
                  }
                  if (v == true) {
                    _seleccionados.add(ing.id);
                  } else {
                    _seleccionados.remove(ing.id);
                  }
                });
              },
            );
          },
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(context, _seleccionados),
          child: Text('Aceptar (${_seleccionados.length})'),
        ),
      ],
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

class _TarjetaResumen extends StatelessWidget {
  final int totalElaboraciones;
  final EstadisticaReceta recetaTop;

  const _TarjetaResumen({
    required this.totalElaboraciones,
    required this.recetaTop,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return Card(
      color: cs.primaryContainer,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '$totalElaboraciones',
                    style: tt.displaySmall?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: cs.onPrimaryContainer,
                    ),
                  ),
                  Text(
                    'elaboraciones\nen total',
                    style: tt.bodySmall?.copyWith(
                      color: cs.onPrimaryContainer.withOpacity(0.8),
                    ),
                  ),
                ],
              ),
            ),
            Container(
              width: 1,
              height: 60,
              color: cs.onPrimaryContainer.withOpacity(0.2),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '🏆 Más elaborada',
                    style: tt.labelSmall?.copyWith(
                      color: cs.onPrimaryContainer.withOpacity(0.7),
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    recetaTop.recetaNombre,
                    style: tt.titleSmall?.copyWith(
                      fontWeight: FontWeight.w700,
                      color: cs.onPrimaryContainer,
                    ),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                  ),
                  Text(
                    '${recetaTop.totalElaboraciones} veces',
                    style: tt.bodySmall?.copyWith(
                      color: cs.onPrimaryContainer.withOpacity(0.8),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _EstadisticaCard extends StatelessWidget {
  final EstadisticaReceta estadistica;
  const _EstadisticaCard({required this.estadistica});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;
    final tasa = estadistica.tasaCompletado;

    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Expanded(
                  child: Text(
                    estadistica.recetaNombre,
                    style: tt.titleSmall?.copyWith(fontWeight: FontWeight.w600),
                  ),
                ),
                Text(
                  '${estadistica.totalElaboraciones} elabor.',
                  style: tt.labelSmall?.copyWith(color: cs.onSurfaceVariant),
                ),
              ],
            ),
            const SizedBox(height: 10),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                value: tasa,
                minHeight: 8,
                backgroundColor: cs.surfaceContainerHighest,
                valueColor: AlwaysStoppedAnimation<Color>(
                  tasa >= 0.8
                      ? Colors.green
                      : tasa >= 0.5
                      ? cs.primary
                      : cs.error,
                ),
              ),
            ),
            const SizedBox(height: 6),
            Row(
              children: [
                Text(
                  '${estadistica.elaboracionesCompletadas}/'
                  '${estadistica.totalElaboraciones} completadas '
                  '(${(tasa * 100).toStringAsFixed(0)}%)',
                  style: tt.labelSmall?.copyWith(color: cs.onSurfaceVariant),
                ),
                const Spacer(),
                if (estadistica.ultimaElaboracion.isNotEmpty)
                  Text(
                    'Última: ${_formatFecha(estadistica.ultimaElaboracion)}',
                    style: tt.labelSmall?.copyWith(color: cs.onSurfaceVariant),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String _formatFecha(String iso) {
    try {
      final dt = DateTime.parse(iso).toLocal();
      return '${dt.day.toString().padLeft(2, '0')}/'
          '${dt.month.toString().padLeft(2, '0')}/'
          '${dt.year}';
    } catch (_) {
      return iso;
    }
  }
}

extension _SortStats on List<EstadisticaReceta> {
  List<EstadisticaReceta> sorted() =>
      [...this]
        ..sort((a, b) => b.totalElaboraciones.compareTo(a.totalElaboraciones));
}
