import 'package:flutter_test/flutter_test.dart';
import 'package:oh_my_freezer/models/ingrediente.dart';
import 'package:oh_my_freezer/models/receta_ingrediente.dart';
import 'package:oh_my_freezer/models/unidad_medida.dart';

void main() {
  const ingredienteBase = Ingrediente(
    id: 1,
    nombre: 'Harina',
    stockActual: 2.0,
    stockMinimo: 0.5,
    unidadMedida: 'kg',
    fechaActualizacion: '2026-06-16T10:00:00',
  );

  group('RecetaIngrediente.fromJson', () {
    test('mapea unidadId y unidad cuando el backend los incluye (Fase 2 "unidades-medida", PR3)', () {
      final json = {
        'id': 10,
        'recetaId': 5,
        'ingrediente': {
          'id': 1,
          'nombre': 'Harina',
          'cantidad': 2.0,
          'stockMinimo': 0.5,
          'unidadMedida': 'kg',
          'fechaActualizacion': '2026-06-16T10:00:00',
        },
        'cantidadNecesaria': 500.0,
        'unidadId': 2,
        'unidad': {'id': 2, 'codigo': 'g', 'nombre': 'Gramo', 'tipo': 'MASA', 'factorABase': 1.0},
      };

      final ri = RecetaIngrediente.fromJson(json);

      expect(ri.id, 10);
      expect(ri.recetaId, 5);
      expect(ri.cantidadNecesaria, 500.0);
      expect(ri.unidadId, 2);
      expect(ri.unidad, isNotNull);
      expect(ri.unidad!.codigo, 'g');
      expect(ri.unidad!.tipo, 'MASA');
    });

    test('tolera unidadId y unidad ausentes (paso de receta pre Fase 2/PR3)', () {
      final json = {
        'id': 11,
        'recetaId': 5,
        'ingrediente': {
          'id': 1,
          'nombre': 'Harina',
          'cantidad': 2.0,
          'stockMinimo': 0.5,
          'unidadMedida': 'kg',
          'fechaActualizacion': '2026-06-16T10:00:00',
        },
        'cantidadNecesaria': 1.0,
      };

      final ri = RecetaIngrediente.fromJson(json);

      expect(ri.unidadId, isNull);
      expect(ri.unidad, isNull);
    });
  });

  group('toJson / round-trip', () {
    test('incluye unidadId y unidad cuando están informados', () {
      const unidad = UnidadMedida(id: 2, codigo: 'g', nombre: 'Gramo', tipo: 'MASA', factorABase: 1.0);
      const ri = RecetaIngrediente(
        id: 10,
        recetaId: 5,
        ingrediente: ingredienteBase,
        cantidadNecesaria: 500.0,
        unidadId: 2,
        unidad: unidad,
      );

      final json = ri.toJson();

      expect(json['unidadId'], 2);
      expect(json['unidad'], isNotNull);
    });

    test('omite unidadId y unidad cuando son null', () {
      const ri = RecetaIngrediente(
        id: 10,
        recetaId: 5,
        ingrediente: ingredienteBase,
        cantidadNecesaria: 500.0,
      );

      final json = ri.toJson();

      expect(json.containsKey('unidadId'), isFalse);
      expect(json.containsKey('unidad'), isFalse);
    });
  });

  group('copyWith', () {
    test('solo cambia los campos indicados', () {
      const ri = RecetaIngrediente(
        id: 10,
        recetaId: 5,
        ingrediente: ingredienteBase,
        cantidadNecesaria: 500.0,
      );

      final actualizado = ri.copyWith(cantidadNecesaria: 750.0);

      expect(actualizado.cantidadNecesaria, 750.0);
      expect(actualizado.id, 10);
      expect(actualizado.ingrediente, ingredienteBase);
    });
  });
}
