/// Mapping entre un SKU del TPV y una receta del sistema.
class TpvSkuMapping {
  final int     id;
  final String  skuTpv;
  final String? nombreTpv;
  final int?    recetaId;
  final String? recetaNombre;
  final bool    mapeado;
  final String  fechaCreacion;
  final String  fechaActualizacion;

  const TpvSkuMapping({
    required this.id,
    required this.skuTpv,
    this.nombreTpv,
    this.recetaId,
    this.recetaNombre,
    required this.mapeado,
    required this.fechaCreacion,
    required this.fechaActualizacion,
  });

  factory TpvSkuMapping.fromJson(Map<String, dynamic> json) => TpvSkuMapping(
        id:                   json['id']                 as int,
        skuTpv:               json['skuTpv']             ?? '',
        nombreTpv:            json['nombreTpv']          as String?,
        recetaId:             json['recetaId']           as int?,
        recetaNombre:         json['recetaNombre']       as String?,
        mapeado:              json['mapeado']            as bool? ?? false,
        fechaCreacion:        json['fechaCreacion']      ?? '',
        fechaActualizacion:   json['fechaActualizacion'] ?? '',
      );

  Map<String, dynamic> toJson() => {
        'id':                 id,
        'skuTpv':             skuTpv,
        'nombreTpv':          nombreTpv,
        'recetaId':           recetaId,
        'recetaNombre':       recetaNombre,
        'mapeado':            mapeado,
        'fechaCreacion':      fechaCreacion,
        'fechaActualizacion': fechaActualizacion,
      };

  TpvSkuMapping copyWith({
    int?    id,
    String? skuTpv,
    String? nombreTpv,
    int?    recetaId,
    String? recetaNombre,
    bool?   mapeado,
    String? fechaCreacion,
    String? fechaActualizacion,
  }) =>
      TpvSkuMapping(
        id:                   id                   ?? this.id,
        skuTpv:               skuTpv               ?? this.skuTpv,
        nombreTpv:            nombreTpv            ?? this.nombreTpv,
        recetaId:             recetaId             ?? this.recetaId,
        recetaNombre:         recetaNombre         ?? this.recetaNombre,
        mapeado:              mapeado              ?? this.mapeado,
        fechaCreacion:        fechaCreacion        ?? this.fechaCreacion,
        fechaActualizacion:   fechaActualizacion   ?? this.fechaActualizacion,
      );
}
