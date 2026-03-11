/// Un paso individual dentro de una receta.
///
/// [orden] determina la secuencia de visualización (1-based).
class PasoReceta {
  final int    id;
  final int    orden;
  final String descripcion;
  final int    recetaId;

  const PasoReceta({
    required this.id,
    required this.orden,
    required this.descripcion,
    required this.recetaId,
  });

  factory PasoReceta.fromJson(Map<String, dynamic> json) => PasoReceta(
        id:          json['id']          as int,
        orden:       json['orden']       as int,
        descripcion: json['descripcion'] as String,
        recetaId:    json['recetaId']    as int,
      );

  Map<String, dynamic> toJson() => {
        'id':          id,
        'orden':       orden,
        'descripcion': descripcion,
        'recetaId':    recetaId,
      };

  PasoReceta copyWith({
    int?    id,
    int?    orden,
    String? descripcion,
    int?    recetaId,
  }) =>
      PasoReceta(
        id:          id          ?? this.id,
        orden:       orden       ?? this.orden,
        descripcion: descripcion ?? this.descripcion,
        recetaId:    recetaId    ?? this.recetaId,
      );
}