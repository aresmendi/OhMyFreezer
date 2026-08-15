import 'unidad_medida.dart';

/// Representa un ingrediente del inventario del congelador.
///
/// [stockActual] se compara con [stockMinimo] para generar alertas.
/// [unidadMedida] puede ser: `"kg"`, `"g"`, `"L"`, `"ml"`, `"ud"`. Se
/// mantiene como campo de compatibilidad de lectura (Fase 2
/// "unidades-medida"): el backend la deriva del código de [unidad] en cada
/// escritura, así que sigue reflejando la unidad real aunque el cliente
/// solo lea este campo. [unidadBaseId] y [unidad] son la referencia tipada
/// al catálogo global y pueden venir `null` si el backend todavía no los
/// expone (compatibilidad con despliegues anteriores a esta fase).
class Ingrediente {
  final int    id;
  final String nombre;
  final double stockActual;
  final double stockMinimo;
  final String unidadMedida;
  final int?   unidadBaseId;
  final UnidadMedida? unidad;
  final String fechaActualizacion; // ISO-8601

  const Ingrediente({
    required this.id,
    required this.nombre,
    required this.stockActual,
    required this.stockMinimo,
    required this.unidadMedida,
    this.unidadBaseId,
    this.unidad,
    required this.fechaActualizacion,
  });

  /// `true` si el stock actual está por debajo del mínimo.
  bool get tieneAlertaStock => stockActual < stockMinimo;

  factory Ingrediente.fromJson(Map<String, dynamic> json) => Ingrediente(
        id:                  json['id']                  as int,
        nombre:              json['nombre']              as String,
        stockActual:         (json['cantidad']           as num).toDouble(),
        stockMinimo:         (json['stockMinimo']        as num).toDouble(),
        unidadMedida:        json['unidadMedida']        as String,
        unidadBaseId:        json['unidadBaseId']        as int?,
        unidad:              json['unidad'] != null
            ? UnidadMedida.fromJson(json['unidad'] as Map<String, dynamic>)
            : null,
        fechaActualizacion:  json['fechaActualizacion']  as String,
      );

  Map<String, dynamic> toJson() => {
        'id':                 id,
        'nombre':             nombre,
        'cantidad':           stockActual,
        'stockMinimo':        stockMinimo,
        'unidadMedida':       unidadMedida,
        if (unidadBaseId != null) 'unidadBaseId': unidadBaseId,
        if (unidad != null) 'unidad': unidad!.toJson(),
        'fechaActualizacion': fechaActualizacion,
      };

  Ingrediente copyWith({
    int?    id,
    String? nombre,
    double? stockActual,
    double? stockMinimo,
    String? unidadMedida,
    int?    unidadBaseId,
    UnidadMedida? unidad,
    String? fechaActualizacion,
  }) =>
      Ingrediente(
        id:                 id                 ?? this.id,
        nombre:             nombre             ?? this.nombre,
        stockActual:        stockActual        ?? this.stockActual,
        stockMinimo:        stockMinimo        ?? this.stockMinimo,
        unidadMedida:       unidadMedida       ?? this.unidadMedida,
        unidadBaseId:       unidadBaseId       ?? this.unidadBaseId,
        unidad:             unidad             ?? this.unidad,
        fechaActualizacion: fechaActualizacion ?? this.fechaActualizacion,
      );
}