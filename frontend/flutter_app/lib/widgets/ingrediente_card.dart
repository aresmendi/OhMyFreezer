import 'package:flutter/material.dart';
import '../models/ingrediente.dart';

/// Tarjeta que muestra un [Ingrediente] en la lista de inventario.
///
/// - Muestra nombre, stock actual vs mínimo y unidad de medida.
/// - Si [tieneAlertaStock] → borde rojo + icono de advertencia.
/// - [onTap] → navega al detalle / formulario de edición.
/// - [onActualizarCantidad] → acción rápida para actualizar stock (todos los roles).
class IngredienteCard extends StatelessWidget {
  final Ingrediente ingrediente;
  final VoidCallback? onTap;
  final VoidCallback? onActualizarCantidad;
  final VoidCallback? onEliminar;

  const IngredienteCard({
    super.key,
    required this.ingrediente,
    this.onTap,
    this.onActualizarCantidad,
    this.onEliminar,
  });

  @override
  Widget build(BuildContext context) {
    final cs     = Theme.of(context).colorScheme;
    final tt     = Theme.of(context).textTheme;
    final alerta = ingrediente.tieneAlertaStock;

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: alerta
            ? BorderSide(color: cs.error, width: 1.5)
            : BorderSide.none,
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Row(
            children: [
              // ── Icono de estado ──────────────────────────────
              CircleAvatar(
                radius: 22,
                backgroundColor: alerta
                    ? cs.error.withValues(alpha: 0.12)
                    : cs.primaryContainer,
                child: Icon(
                  alerta ? Icons.warning_amber_rounded : Icons.kitchen_rounded,
                  color: alerta ? cs.error : cs.onPrimaryContainer,
                  size: 22,
                ),
              ),
              const SizedBox(width: 14),

              // ── Nombre + stock ───────────────────────────────
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      ingrediente.nombre,
                      style: tt.titleMedium,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 4),
                    _StockIndicator(ingrediente: ingrediente),
                  ],
                ),
              ),

              // ── Botón actualizar cantidad ────────────────────
              if (onActualizarCantidad != null)
                IconButton(
                  icon: Icon(Icons.edit_rounded, color: cs.primary, size: 20),
                  tooltip: 'Actualizar stock',
                  onPressed: onActualizarCantidad,
                ),

              // ── Botón eliminar ───────────────────────────────
              if (onEliminar != null)
                IconButton(
                  icon: Icon(Icons.delete_outline_rounded, color: cs.error, size: 20),
                  tooltip: 'Eliminar ingrediente',
                  onPressed: onEliminar,
                ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Barra de progreso visual del stock actual vs mínimo.
class _StockIndicator extends StatelessWidget {
  final Ingrediente ingrediente;
  const _StockIndicator({required this.ingrediente});

  @override
  Widget build(BuildContext context) {
    final cs    = Theme.of(context).colorScheme;
    final tt    = Theme.of(context).textTheme;
    final ratio = ingrediente.stockMinimo > 0
        ? (ingrediente.stockActual / ingrediente.stockMinimo).clamp(0.0, 2.0)
        : 1.0;
    final color = ingrediente.tieneAlertaStock ? cs.error : cs.primary;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: (ratio / 2).clamp(0.0, 1.0), // normalizado a 0-1
            minHeight: 6,
            backgroundColor: cs.surfaceContainerHighest,
            valueColor: AlwaysStoppedAnimation<Color>(color),
          ),
        ),
        const SizedBox(height: 4),
        Text(
          '${ingrediente.stockActual} / ${ingrediente.stockMinimo} ${ingrediente.unidadMedida}',
          style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
        ),
      ],
    );
  }
}