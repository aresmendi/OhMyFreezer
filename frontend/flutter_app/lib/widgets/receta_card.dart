// lib/widgets/receta_card.dart

import 'package:flutter/material.dart';
import '../models/receta.dart';

/// Tarjeta que muestra una [Receta] en la lista de recetas.
///
/// - Muestra nombre, descripción truncada y número de pasos.
/// - Si [puedeElaborarse] == true  → chip verde "Listo".
/// - Si [puedeElaborarse] == false → chip rojo "Sin stock".
/// - Si [puedeElaborarse] == null  → sin chip (no verificado).
/// - [onTap] → navega al detalle de la receta.
/// - [onEliminar] → visible solo para jefe de cocina.
/// [onToggleFavorito] → marca como favorita la receta o desmarcala
/// [mostrarFavorito] → devuelve true si está marcado o false si no (true por defecto)
class RecetaCard extends StatelessWidget {
  final Receta receta;
  final VoidCallback? onTap;
  final VoidCallback? onEliminar;
  final VoidCallback? onToggleFavorito;
  final bool mostrarFavorito;

  const RecetaCard({
    super.key,
    required this.receta,
    this.onTap,
    this.onEliminar,
    this.onToggleFavorito,
    this.mostrarFavorito = true,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // ── Cabecera: nombre + menú ──────────────────────
              Row(
                children: [
                  CircleAvatar(
                    radius: 20,
                    backgroundColor: cs.secondaryContainer,
                    child: Icon(
                      Icons.menu_book_rounded,
                      color: cs.onSecondaryContainer,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      receta.nombre,
                      style: tt.titleMedium,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  // ───── BOTÓN DE FAVORITO (ESTRELLA) ─────
                  if (mostrarFavorito && onToggleFavorito != null)
                    IconButton(
                      icon: Icon(
                        receta.esFavorita == true
                            ? Icons.star_rounded
                            : Icons.star_border_rounded,
                        color: receta.esFavorita == true
                            ? Colors.amber
                            : cs.onSurfaceVariant,
                        size: 24,
                      ),
                      tooltip: receta.esFavorita == true
                          ? 'Quitar de favoritos'
                          : 'Añadir a favoritos',
                      onPressed: onToggleFavorito,
                    ),
                  // ───── BOTÓN DE ELIMINAR (BASURA) ─────
                  if (onEliminar != null)
                    IconButton(
                      icon: Icon(Icons.delete_outline_rounded,
                          color: cs.error, size: 20),
                      tooltip: 'Eliminar receta',
                      onPressed: onEliminar,
                    ),
                ],
              ),
              const SizedBox(height: 8),

              // ── Descripción ──────────────────────────────────
              Text(
                receta.descripcion,
                style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 10),

              // ── Footer: pasos + chip stock ───────────────────
              Row(
                children: [
                  Icon(Icons.format_list_numbered_rounded,
                      size: 16, color: cs.onSurfaceVariant),
                  const SizedBox(width: 4),
                  Text(
                    '${receta.pasos.length} pasos',
                    style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
                  ),
                  const Spacer(),
                  if (receta.puedeElaborarse != null)
                    _ChipStock(puedeElaborarse: receta.puedeElaborarse!),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ChipStock extends StatelessWidget {
  final bool puedeElaborarse;
  const _ChipStock({required this.puedeElaborarse});

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final color  = puedeElaborarse ? Colors.green : cs.error;
    final label  = puedeElaborarse ? 'Listo' : 'Sin stock';
    final icon   = puedeElaborarse ? Icons.check_circle_outline : Icons.block_rounded;

    return Chip(
      avatar: Icon(icon, size: 14, color: color),
      label: Text(label,
          style: TextStyle(fontSize: 11, color: color, fontWeight: FontWeight.w600)),
      backgroundColor: color.withOpacity(0.1),
      side: BorderSide(color: color.withOpacity(0.4)),
      padding: const EdgeInsets.symmetric(horizontal: 4),
      visualDensity: VisualDensity.compact,
    );
  }
}