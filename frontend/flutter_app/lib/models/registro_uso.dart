/// Registro de una elaboración de receta por un cocinero.
///
/// [completada] indica si la elaboración llegó al final o fue cancelada.
class RegistroUso {
  final int    id;
  final int    recetaId;
  final String recetaNombre; // desnormalizado para mostrar sin join
  final int    usuarioId;
  final String fechaElaboracion; // ISO-8601
  final bool   completada;

  const RegistroUso({
    required this.id,
    required this.recetaId,
    required this.recetaNombre,
    required this.usuarioId,
    required this.fechaElaboracion,
    required this.completada,
  });

  factory RegistroUso.fromJson(Map<String, dynamic> json) => RegistroUso(
        id:               json['id']               as int,
        recetaId:         json['recetaId']         as int,
        recetaNombre:     json['recetaNombre']     as String,
        usuarioId:        json['usuarioId']        as int,
        fechaElaboracion: json['fechaElaboracion'] as String,
        completada:       json['completada']       as bool,
      );

  Map<String, dynamic> toJson() => {
        'id':               id,
        'recetaId':         recetaId,
        'recetaNombre':     recetaNombre,
        'usuarioId':        usuarioId,
        'fechaElaboracion': fechaElaboracion,
        'completada':       completada,
      };

  RegistroUso copyWith({
    int?    id,
    int?    recetaId,
    String? recetaNombre,
    int?    usuarioId,
    String? fechaElaboracion,
    bool?   completada,
  }) =>
      RegistroUso(
        id:               id               ?? this.id,
        recetaId:         recetaId         ?? this.recetaId,
        recetaNombre:     recetaNombre     ?? this.recetaNombre,
        usuarioId:        usuarioId        ?? this.usuarioId,
        fechaElaboracion: fechaElaboracion ?? this.fechaElaboracion,
        completada:       completada       ?? this.completada,
      );
}