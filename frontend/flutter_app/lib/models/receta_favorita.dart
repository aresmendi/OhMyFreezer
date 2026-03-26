import 'receta.dart';

/// Representa una receta marcada como favorita por un usuario.
/// Incluye la información completa de la receta y la fecha de marcado.
class RecetaFavorita {
  final int id;
  final int usuarioId;
  final Receta receta;
  final String fechaMarcado; // ISO-8601

  const RecetaFavorita({
    required this.id,
    required this.usuarioId,
    required this.receta,
    required this.fechaMarcado,
  });

  factory RecetaFavorita.fromJson(Map<String, dynamic> json) => RecetaFavorita(
        id: json['id'] as int,
        usuarioId: json['usuarioId'] as int,
        receta: Receta.fromJson(json['receta'] as Map<String, dynamic>),
        fechaMarcado: json['fechaMarcado'] ?? '',
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'usuarioId': usuarioId,
        'receta': receta.toJson(),
        'fechaMarcado': fechaMarcado,
      };
}