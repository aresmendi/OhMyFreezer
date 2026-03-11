/// Estadística de uso de una receta, devuelta por el endpoint
/// `GET /estadisticas/recetas`.
class EstadisticaReceta {
  final int    recetaId;
  final String recetaNombre;
  final int    totalElaboraciones;
  final int    elaboracionesCompletadas;
  final String ultimaElaboracion; // ISO-8601, puede ser vacío

  const EstadisticaReceta({
    required this.recetaId,
    required this.recetaNombre,
    required this.totalElaboraciones,
    required this.elaboracionesCompletadas,
    required this.ultimaElaboracion,
  });

  /// Porcentaje de elaboraciones completadas (0.0 – 1.0).
  double get tasaCompletado => totalElaboraciones == 0
      ? 0.0
      : elaboracionesCompletadas / totalElaboraciones;

  factory EstadisticaReceta.fromJson(Map<String, dynamic> json) =>
      EstadisticaReceta(
        recetaId:                json['recetaId']                as int,
        recetaNombre:            json['recetaNombre']            as String,
        totalElaboraciones:      json['totalElaboraciones']      as int,
        elaboracionesCompletadas: json['elaboracionesCompletadas'] as int,
        ultimaElaboracion:       json['ultimaElaboracion']       as String,
      );

  Map<String, dynamic> toJson() => {
        'recetaId':                recetaId,
        'recetaNombre':            recetaNombre,
        'totalElaboraciones':      totalElaboraciones,
        'elaboracionesCompletadas': elaboracionesCompletadas,
        'ultimaElaboracion':       ultimaElaboracion,
      };
}