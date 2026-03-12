/// Representa un usuario del sistema OhMyFreezer.
///
/// [esJefeCocina] determina qué funcionalidades están disponibles:
/// - `true`  → puede crear/editar recetas y recibe alertas de stock.
/// - `false` → solo puede consultar recetas y elaborarlas.
class Usuario {
  final int    id;
  final String username;
  final bool   esJefeCocina;
  final String fechaRegistro; // ISO-8601

  const Usuario({
    required this.id,
    required this.username,
    required this.esJefeCocina,
    required this.fechaRegistro,
  });

  factory Usuario.fromJson(Map<String, dynamic> json) => Usuario(
        id:            json['id']            as int,
        username:      json['username']      as String,
        esJefeCocina:  json['esJefeCocina']  as bool,
        fechaRegistro: json['fechaRegistro'] as String,
      );

  Map<String, dynamic> toJson() => {
        'id':            id,
        'username':      username,
        'esJefeCocina':  esJefeCocina,
        'fechaRegistro': fechaRegistro,
      };

  Usuario copyWith({
    int?    id,
    String? username,
    bool?   esJefeCocina,
    String? fechaRegistro,
  }) =>
      Usuario(
        id:            id            ?? this.id,
        username:      username      ?? this.username,
        esJefeCocina:  esJefeCocina  ?? this.esJefeCocina,
        fechaRegistro: fechaRegistro ?? this.fechaRegistro,
      );
}