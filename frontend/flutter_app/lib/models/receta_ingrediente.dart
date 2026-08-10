import 'ingrediente.dart';
import 'unidad_medida.dart';

/// Relación entre una receta y uno de sus ingredientes,
/// con la cantidad necesaria para elaborarla.
///
/// [unidadId] y [unidad] son la unidad de medida (catálogo global) en la que
/// se expresa [cantidadNecesaria] (Fase 2 "unidades-medida", PR3). Pueden
/// venir `null` si el backend todavía no los expone (compatibilidad con
/// despliegues anteriores a esta fase); en ese caso se asume la propia
/// unidad del ingrediente.
class RecetaIngrediente {
  final int        id;
  final int        recetaId;
  final Ingrediente ingrediente;
  final double     cantidadNecesaria;
  final int?       unidadId;
  final UnidadMedida? unidad;

  const RecetaIngrediente({
    required this.id,
    required this.recetaId,
    required this.ingrediente,
    required this.cantidadNecesaria,
    this.unidadId,
    this.unidad,
  });

  factory RecetaIngrediente.fromJson(Map<String, dynamic> json) =>
      RecetaIngrediente(
        id:                json['id']                as int,
        recetaId:          json['recetaId']          as int,
        ingrediente:       Ingrediente.fromJson(json['ingrediente'] as Map<String, dynamic>),
        cantidadNecesaria: (json['cantidadNecesaria'] as num).toDouble(),
        unidadId:          json['unidadId']          as int?,
        unidad:            json['unidad'] != null
            ? UnidadMedida.fromJson(json['unidad'] as Map<String, dynamic>)
            : null,
      );

  Map<String, dynamic> toJson() => {
        'id':                id,
        'recetaId':          recetaId,
        'ingrediente':       ingrediente.toJson(),
        'cantidadNecesaria': cantidadNecesaria,
        if (unidadId != null) 'unidadId': unidadId,
        if (unidad != null) 'unidad': unidad!.toJson(),
      };

  RecetaIngrediente copyWith({
    int?         id,
    int?         recetaId,
    Ingrediente? ingrediente,
    double?      cantidadNecesaria,
    int?         unidadId,
    UnidadMedida? unidad,
  }) =>
      RecetaIngrediente(
        id:                id                ?? this.id,
        recetaId:          recetaId          ?? this.recetaId,
        ingrediente:       ingrediente       ?? this.ingrediente,
        cantidadNecesaria: cantidadNecesaria ?? this.cantidadNecesaria,
        unidadId:          unidadId          ?? this.unidadId,
        unidad:            unidad            ?? this.unidad,
      );
}