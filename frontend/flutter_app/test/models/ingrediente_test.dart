import 'package:flutter_test/flutter_test.dart';
import 'package:oh_my_freezer/models/ingrediente.dart';

void main() {
  group('Ingrediente.fromJson', () {
    test('mapea el campo "cantidad" del backend a stockActual', () {
      final json = {
        'id': 1,
        'nombre': 'Tomate',
        'cantidad': 10.5,
        'stockMinimo': 5.0,
        'unidadMedida': 'kg',
        'fechaActualizacion': '2026-06-16T10:00:00',
      };

      final ing = Ingrediente.fromJson(json);

      expect(ing.id, 1);
      expect(ing.nombre, 'Tomate');
      expect(ing.stockActual, 10.5);
      expect(ing.stockMinimo, 5.0);
      expect(ing.unidadMedida, 'kg');
    });

    test('acepta enteros del backend y los convierte a double', () {
      final json = {
        'id': 2,
        'nombre': 'Queso',
        'cantidad': 8, // int, no double
        'stockMinimo': 3,
        'unidadMedida': 'kg',
        'fechaActualizacion': '2026-06-16T10:00:00',
      };

      final ing = Ingrediente.fromJson(json);

      expect(ing.stockActual, 8.0);
      expect(ing.stockMinimo, 3.0);
    });

    test('mapea unidadBaseId y unidad cuando el backend los incluye (Fase 2 "unidades-medida")', () {
      final json = {
        'id': 3,
        'nombre': 'Harina',
        'cantidad': 2.0,
        'stockMinimo': 1.0,
        'unidadMedida': 'kg',
        'unidadBaseId': 7,
        'unidad': {'id': 7, 'codigo': 'kg', 'nombre': 'Kilogramo', 'tipo': 'MASA', 'factorABase': 1000.0},
        'fechaActualizacion': '2026-06-16T10:00:00',
      };

      final ing = Ingrediente.fromJson(json);

      expect(ing.unidadBaseId, 7);
      expect(ing.unidad, isNotNull);
      expect(ing.unidad!.codigo, 'kg');
      expect(ing.unidad!.tipo, 'MASA');
    });

    test('tolera unidadBaseId y unidad ausentes (cliente/backend pre-migración)', () {
      final json = {
        'id': 4,
        'nombre': 'Queso',
        'cantidad': 1.0,
        'stockMinimo': 1.0,
        'unidadMedida': 'ud',
        'fechaActualizacion': '2026-06-16T10:00:00',
      };

      final ing = Ingrediente.fromJson(json);

      expect(ing.unidadBaseId, isNull);
      expect(ing.unidad, isNull);
      expect(ing.unidadMedida, 'ud');
    });
  });

  group('tieneAlertaStock', () {
    test('es true cuando el stock actual está por debajo del mínimo', () {
      const ing = Ingrediente(
        id: 1,
        nombre: 'Tomate',
        stockActual: 2.0,
        stockMinimo: 5.0,
        unidadMedida: 'kg',
        fechaActualizacion: '2026-06-16T10:00:00',
      );

      expect(ing.tieneAlertaStock, isTrue);
    });

    test('es false cuando el stock actual iguala o supera el mínimo', () {
      const igual = Ingrediente(
        id: 1,
        nombre: 'Tomate',
        stockActual: 5.0,
        stockMinimo: 5.0,
        unidadMedida: 'kg',
        fechaActualizacion: '2026-06-16T10:00:00',
      );
      const superior = Ingrediente(
        id: 2,
        nombre: 'Queso',
        stockActual: 9.0,
        stockMinimo: 5.0,
        unidadMedida: 'kg',
        fechaActualizacion: '2026-06-16T10:00:00',
      );

      expect(igual.tieneAlertaStock, isFalse);
      expect(superior.tieneAlertaStock, isFalse);
    });
  });

  group('toJson / round-trip', () {
    test('toJson usa la clave "cantidad" esperada por el backend', () {
      const ing = Ingrediente(
        id: 1,
        nombre: 'Tomate',
        stockActual: 10.0,
        stockMinimo: 5.0,
        unidadMedida: 'kg',
        fechaActualizacion: '2026-06-16T10:00:00',
      );

      final json = ing.toJson();

      expect(json['cantidad'], 10.0);
      expect(json.containsKey('stockActual'), isFalse);
    });

    test('fromJson(toJson(x)) preserva los datos', () {
      const original = Ingrediente(
        id: 7,
        nombre: 'Harina',
        stockActual: 3.2,
        stockMinimo: 1.0,
        unidadMedida: 'kg',
        fechaActualizacion: '2026-06-16T10:00:00',
      );

      final copia = Ingrediente.fromJson(original.toJson());

      expect(copia.id, original.id);
      expect(copia.nombre, original.nombre);
      expect(copia.stockActual, original.stockActual);
      expect(copia.unidadMedida, original.unidadMedida);
    });
  });

  group('copyWith', () {
    test('solo cambia los campos indicados', () {
      const ing = Ingrediente(
        id: 1,
        nombre: 'Tomate',
        stockActual: 10.0,
        stockMinimo: 5.0,
        unidadMedida: 'kg',
        fechaActualizacion: '2026-06-16T10:00:00',
      );

      final actualizado = ing.copyWith(stockActual: 2.0);

      expect(actualizado.stockActual, 2.0);
      expect(actualizado.nombre, 'Tomate');
      expect(actualizado.tieneAlertaStock, isTrue);
    });
  });
}
