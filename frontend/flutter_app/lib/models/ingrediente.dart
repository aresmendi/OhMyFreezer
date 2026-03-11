/// Representa un ingrediente del inventario del congelador.
///
/// [stockActual] se compara con [stockMinimo] para generar alertas.
/// [unidadMedida] puede ser: `"kg"`, `"g"`, `"L"`, `"ml"`, `"ud"`.
class Ingrediente {
  final int    id;
  final String nombre;
  final double stockActual;
  final double stockMinimo;
  final String unidadMedida;
  final String fechaActualizacion; // ISO-8601

  const Ingrediente({
    required this.id,
    required this.nombre,
    required this.stockActual,
    required this.stockMinimo,
    required this.unidadMedida,
    required this.fechaActualizacion,
  });

  /// `true` si el stock actual está por debajo del mínimo.
  bool get tieneAlertaStock => stockActual < stockMinimo;

  factory Ingrediente.fromJson(Map<String, dynamic> json) => Ingrediente(
        id:                  json['id']                  as int,
        nombre:              json['nombre']              as String,
        stockActual:         (json['stockActual']        as num).toDouble(),
        stockMinimo:         (json['stockMinimo']        as num).toDouble(),
        unidadMedida:        json['unidadMedida']        as String,
        fechaActualizacion:  json['fechaActualizacion']  as String,
      );

  Map<String, dynamic> toJson() => {
        'id':                 id,
        'nombre':             nombre,
        'stockActual':        stockActual,
        'stockMinimo':        stockMinimo,
        'unidadMedida':       unidadMedida,
        'fechaActualizacion': fechaActualizacion,
      };

  Ingrediente copyWith({
    int?    id,
    String? nombre,
    double? stockActual,
    double? stockMinimo,
    String? unidadMedida,
    String? fechaActualizacion,
  }) =>
      Ingrediente(
        id:                 id                 ?? this.id,
        nombre:             nombre             ?? this.nombre,
        stockActual:        stockActual        ?? this.stockActual,
        stockMinimo:        stockMinimo        ?? this.stockMinimo,
        unidadMedida:       unidadMedida       ?? this.unidadMedida,
        fechaActualizacion: fechaActualizacion ?? this.fechaActualizacion,
      );
}