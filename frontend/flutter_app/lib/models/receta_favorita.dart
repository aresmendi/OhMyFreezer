import 'receta.dart';

/// Representa una receta marcada como favorita por un usuario.
/// Incluye la información completa de la receta y la fecha de marcado.
class RecetaFavorita {
  final int id;
  final Receta receta;
  final String fechaMarcado; // ISO-8601

  const RecetaFavorita({
    required this.id,
    required this.receta,
    required this.fechaMarcado,
  });

  factory RecetaFavorita.fromJson(Map<String, dynamic> json) {
    final fechaMarcadoRaw = json['fechaMarcado'];
    String fechaMarcado;
    if (fechaMarcadoRaw is String) {
      fechaMarcado = fechaMarcadoRaw;
    } else if (fechaMarcadoRaw is List) {
      // Spring serializa LocalDateTime como array [year, month, day, hour, minute, second]
      fechaMarcado = DateTime(
        fechaMarcadoRaw[0] as int,
        fechaMarcadoRaw[1] as int,
        fechaMarcadoRaw[2] as int,
        fechaMarcadoRaw.length > 3 ? fechaMarcadoRaw[3] as int : 0,
        fechaMarcadoRaw.length > 4 ? fechaMarcadoRaw[4] as int : 0,
        fechaMarcadoRaw.length > 5 ? fechaMarcadoRaw[5] as int : 0,
      ).toIso8601String();
    } else {
      fechaMarcado = '';
    }
    return RecetaFavorita(
      id: (json['id'] as num).toInt(),
      receta: Receta.fromJson(json['receta'] as Map<String, dynamic>),
      fechaMarcado: fechaMarcado,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'receta': receta.toJson(),
    'fechaMarcado': fechaMarcado,
  };
}
