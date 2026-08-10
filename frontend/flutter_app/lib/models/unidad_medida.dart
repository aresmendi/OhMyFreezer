/// Representa una unidad de medida del catálogo global (`unidades_medida`).
///
/// El catálogo es idéntico para todos los negocios (no está scoped por
/// tenant) y prácticamente inmutable en tiempo de ejecución. [tipo] es la
/// dimensión física de la unidad (`"MASA"`, `"VOLUMEN"` o `"UNIDAD"`) y
/// [factorABase] su factor de conversión respecto a la unidad base de ese
/// tipo (gramo para MASA, mililitro para VOLUMEN).
class UnidadMedida {
  final int    id;
  final String codigo;
  final String nombre;
  final String tipo;
  final double factorABase;

  const UnidadMedida({
    required this.id,
    required this.codigo,
    required this.nombre,
    required this.tipo,
    required this.factorABase,
  });

  factory UnidadMedida.fromJson(Map<String, dynamic> json) => UnidadMedida(
        id:          json['id']          as int,
        codigo:      json['codigo']      as String,
        nombre:      json['nombre']      as String,
        tipo:        json['tipo']        as String,
        factorABase: (json['factorABase'] as num).toDouble(),
      );

  Map<String, dynamic> toJson() => {
        'id':          id,
        'codigo':      codigo,
        'nombre':      nombre,
        'tipo':        tipo,
        'factorABase': factorABase,
      };
}
