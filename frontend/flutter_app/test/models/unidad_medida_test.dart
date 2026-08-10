import 'package:flutter_test/flutter_test.dart';
import 'package:oh_my_freezer/models/unidad_medida.dart';

void main() {
  group('UnidadMedida.fromJson', () {
    test('mapea todos los campos del catálogo global', () {
      final json = {
        'id': 1,
        'codigo': 'kg',
        'nombre': 'Kilogramo',
        'tipo': 'MASA',
        'factorABase': 1000.0,
      };

      final unidad = UnidadMedida.fromJson(json);

      expect(unidad.id, 1);
      expect(unidad.codigo, 'kg');
      expect(unidad.nombre, 'Kilogramo');
      expect(unidad.tipo, 'MASA');
      expect(unidad.factorABase, 1000.0);
    });

    test('acepta factorABase entero del backend y lo convierte a double', () {
      final json = {
        'id': 5,
        'codigo': 'ud',
        'nombre': 'Unidad',
        'tipo': 'UNIDAD',
        'factorABase': 1, // int, no double
      };

      final unidad = UnidadMedida.fromJson(json);

      expect(unidad.factorABase, 1.0);
    });
  });

  group('toJson / round-trip', () {
    test('toJson serializa todos los campos con las claves del backend', () {
      const unidad = UnidadMedida(
        id: 2,
        codigo: 'L',
        nombre: 'Litro',
        tipo: 'VOLUMEN',
        factorABase: 1000.0,
      );

      final json = unidad.toJson();

      expect(json['id'], 2);
      expect(json['codigo'], 'L');
      expect(json['nombre'], 'Litro');
      expect(json['tipo'], 'VOLUMEN');
      expect(json['factorABase'], 1000.0);
    });

    test('fromJson(toJson(x)) preserva los datos', () {
      const original = UnidadMedida(
        id: 3,
        codigo: 'ml',
        nombre: 'Mililitro',
        tipo: 'VOLUMEN',
        factorABase: 1.0,
      );

      final copia = UnidadMedida.fromJson(original.toJson());

      expect(copia.id, original.id);
      expect(copia.codigo, original.codigo);
      expect(copia.nombre, original.nombre);
      expect(copia.tipo, original.tipo);
      expect(copia.factorABase, original.factorABase);
    });
  });
}
