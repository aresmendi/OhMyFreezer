class MovimientoStock {
  final int id;
  final int ingredienteId;
  final String ingredienteNombre;
  final String ingredienteUnidad;
  final double cantidadAnterior;
  final double cantidadNueva;
  final double cantidadCambio;
  final String tipo;
  final DateTime fecha;
  final String? motivo;

  MovimientoStock({
    required this.id,
    required this.ingredienteId,
    required this.ingredienteNombre,
    required this.ingredienteUnidad,
    required this.cantidadAnterior,
    required this.cantidadNueva,
    required this.cantidadCambio,
    required this.tipo,
    required this.fecha,
    this.motivo,
  });

  factory MovimientoStock.fromJson(Map<String, dynamic> json) {
    DateTime parseFecha(dynamic value) {
      if (value is String) {
        return DateTime.parse(value);
      } else if (value is List) {
        // Spring serializa LocalDateTime como array [year, month, day, hour, minute, second, nano]
        return DateTime(
          value[0] as int,
          value[1] as int,
          value[2] as int,
          value.length > 3 ? value[3] as int : 0,
          value.length > 4 ? value[4] as int : 0,
          value.length > 5 ? value[5] as int : 0,
        );
      }
      throw FormatException('Formato de fecha no soportado: $value');
    }

    return MovimientoStock(
      id: (json['id'] as num).toInt(),
      ingredienteId: (json['ingredienteId'] as num).toInt(),
      ingredienteNombre: json['ingredienteNombre'] as String,
      ingredienteUnidad: json['ingredienteUnidad'] as String,
      cantidadAnterior: (json['cantidadAnterior'] as num).toDouble(),
      cantidadNueva: (json['cantidadNueva'] as num).toDouble(),
      cantidadCambio: (json['cantidadCambio'] as num).toDouble(),
      tipo: json['tipo'] as String,
      fecha: parseFecha(json['fecha']),
      motivo: json['motivo'] as String?,
    );
  }
}
