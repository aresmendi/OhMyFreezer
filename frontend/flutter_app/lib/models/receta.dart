import 'paso_receta.dart';
import 'receta_ingrediente.dart';

/// Receta completa con sus pasos e ingredientes embebidos.
///
/// [creadaPorId] referencia al [Usuario] jefe de cocina que la creó.
/// [puedElaborarse] es calculado por el backend al verificar stock;
/// en el modelo Flutter se almacena como campo opcional para cachear
/// el resultado de la última verificación.
class Receta {
  final int                   id;
  final String                nombre;
  final String                descripcion;
  final List<PasoReceta>      pasos;
  final List<RecetaIngrediente> ingredientes;
  final int                   creadaPorId;
  final String                fechaCreacion; // ISO-8601
  final bool?                 puedeElaborarse; // null = no verificado aún

  const Receta({
    required this.id,
    required this.nombre,
    required this.descripcion,
    required this.pasos,
    required this.ingredientes,
    required this.creadaPorId,
    required this.fechaCreacion,
    this.puedeElaborarse,
  });

  factory Receta.fromJson(Map<String, dynamic> json) => Receta(
        id:              json['id']          as int,
        nombre:          json['nombre']      ?? '',
        descripcion:     json['descripcion'] ?? '',
        pasos:           json['pasos'] != null 
            ? (json['pasos'] as List<dynamic>)
                .map((e) => PasoReceta.fromJson(e as Map<String, dynamic>))
                .toList()
            : [],
        ingredientes:    json['ingredientes'] != null
            ? (json['ingredientes'] as List<dynamic>)
                .map((e) => RecetaIngrediente.fromJson(e as Map<String, dynamic>))
                .toList()
            : [],
        creadaPorId:     json['creadaPor'] != null ? (json['creadaPor']['id'] as int) : 0,
        fechaCreacion:   json['fechaCreacion']   ?? '',
        puedeElaborarse: json['disponible'] as bool?,
      );

  Map<String, dynamic> toJson() => {
        'id':              id,
        'nombre':          nombre,
        'descripcion':     descripcion,
        'pasos':           pasos.map((p) => p.toJson()).toList(),
        'ingredientes':    ingredientes.map((i) => i.toJson()).toList(),
        'creadaPorId':     creadaPorId,
        'fechaCreacion':   fechaCreacion,
        'disponible':      puedeElaborarse,
      };

  Receta copyWith({
    int?                    id,
    String?                 nombre,
    String?                 descripcion,
    List<PasoReceta>?       pasos,
    List<RecetaIngrediente>? ingredientes,
    int?                    creadaPorId,
    String?                 fechaCreacion,
    bool?                   puedeElaborarse,
  }) =>
      Receta(
        id:              id              ?? this.id,
        nombre:          nombre          ?? this.nombre,
        descripcion:     descripcion     ?? this.descripcion,
        pasos:           pasos           ?? this.pasos,
        ingredientes:    ingredientes    ?? this.ingredientes,
        creadaPorId:     creadaPorId     ?? this.creadaPorId,
        fechaCreacion:   fechaCreacion   ?? this.fechaCreacion,
        puedeElaborarse: puedeElaborarse ?? this.puedeElaborarse,
      );
}