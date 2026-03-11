import 'ingrediente.dart';

/// Relación entre una receta y uno de sus ingredientes,
/// con la cantidad necesaria para elaborarla.
class RecetaIngrediente {
  final int        id;
  final int        recetaId;
  final Ingrediente ingrediente;
  final double     cantidadNecesaria;

  const RecetaIngrediente({
    required this.id,
    required this.recetaId,
    required this.ingrediente,
    required this.cantidadNecesaria,
  });

  factory RecetaIngrediente.fromJson(Map<String, dynamic> json) =>
      RecetaIngrediente(
        id:                json['id']                as int,
        recetaId:          json['recetaId']          as int,
        ingrediente:       Ingrediente.fromJson(json['ingrediente'] as Map<String, dynamic>),
        cantidadNecesaria: (json['cantidadNecesaria'] as num).toDouble(),
      );

  Map<String, dynamic> toJson() => {
        'id':                id,
        'recetaId':          recetaId,
        'ingrediente':       ingrediente.toJson(),
        'cantidadNecesaria': cantidadNecesaria,
      };

  RecetaIngrediente copyWith({
    int?         id,
    int?         recetaId,
    Ingrediente? ingrediente,
    double?      cantidadNecesaria,
  }) =>
      RecetaIngrediente(
        id:                id                ?? this.id,
        recetaId:          recetaId          ?? this.recetaId,
        ingrediente:       ingrediente       ?? this.ingrediente,
        cantidadNecesaria: cantidadNecesaria ?? this.cantidadNecesaria,
      );
}