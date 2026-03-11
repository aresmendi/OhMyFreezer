/// Alerta generada por el backend cuando el stock de un ingrediente
/// cae por debajo del mínimo tras elaborar una receta.
///
/// Solo visible para usuarios con [esJefeCocina] = `true`.
/// [leida] se actualiza en local y se sincroniza con el backend.
enum TipoAlerta { stockBajo, stockAgotado }

class Alerta {
  final int        id;
  final TipoAlerta tipo;
  final String     mensaje;
  final int?       recetaId;       // receta que provocó la alerta (nullable)
  final int        ingredienteId;
  final String     ingredienteNombre; // desnormalizado
  final String     fechaCreacion;  // ISO-8601
  final bool       leida;

  const Alerta({
    required this.id,
    required this.tipo,
    required this.mensaje,
    this.recetaId,
    required this.ingredienteId,
    required this.ingredienteNombre,
    required this.fechaCreacion,
    required this.leida,
  });

  factory Alerta.fromJson(Map<String, dynamic> json) => Alerta(
        id:                 json['id']                 as int,
        tipo:               TipoAlerta.values.byName(json['tipo'] as String),
        mensaje:            json['mensaje']            as String,
        recetaId:           json['recetaId']           as int?,
        ingredienteId:      json['ingredienteId']      as int,
        ingredienteNombre:  json['ingredienteNombre']  as String,
        fechaCreacion:      json['fechaCreacion']      as String,
        leida:              json['leida']              as bool,
      );

  Map<String, dynamic> toJson() => {
        'id':                id,
        'tipo':              tipo.name,
        'mensaje':           mensaje,
        'recetaId':          recetaId,
        'ingredienteId':     ingredienteId,
        'ingredienteNombre': ingredienteNombre,
        'fechaCreacion':     fechaCreacion,
        'leida':             leida,
      };

  Alerta copyWith({
    int?        id,
    TipoAlerta? tipo,
    String?     mensaje,
    int?        recetaId,
    int?        ingredienteId,
    String?     ingredienteNombre,
    String?     fechaCreacion,
    bool?       leida,
  }) =>
      Alerta(
        id:                id                ?? this.id,
        tipo:              tipo              ?? this.tipo,
        mensaje:           mensaje           ?? this.mensaje,
        recetaId:          recetaId          ?? this.recetaId,
        ingredienteId:     ingredienteId     ?? this.ingredienteId,
        ingredienteNombre: ingredienteNombre ?? this.ingredienteNombre,
        fechaCreacion:     fechaCreacion     ?? this.fechaCreacion,
        leida:             leida             ?? this.leida,
      );
}