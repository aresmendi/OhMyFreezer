import 'package:flutter_test/flutter_test.dart';
import 'package:oh_my_freezer/models/receta.dart';

void main() {
  group('Receta.fromJson', () {
    test('extrae creadaPorId del objeto anidado creadaPor', () {
      final json = {
        'id': 1,
        'nombre': 'Pizza',
        'descripcion': 'Pizza margarita',
        'creadaPor': {'id': 42, 'username': 'jefe'},
        'fechaCreacion': '2026-06-16T10:00:00',
        'disponible': true,
      };

      final receta = Receta.fromJson(json);

      expect(receta.id, 1);
      expect(receta.nombre, 'Pizza');
      expect(receta.creadaPorId, 42);
      expect(receta.puedeElaborarse, isTrue);
    });

    test('usa 0 como creadaPorId si no viene creadaPor', () {
      final json = {
        'id': 2,
        'nombre': 'Pasta',
        'descripcion': '',
        'fechaCreacion': '2026-06-16T10:00:00',
      };

      final receta = Receta.fromJson(json);

      expect(receta.creadaPorId, 0);
      expect(receta.puedeElaborarse, isNull); // no verificado
    });

    test('devuelve listas vacías si pasos e ingredientes son null', () {
      final json = {
        'id': 3,
        'nombre': 'Ensalada',
        'descripcion': 'desc',
        'creadaPor': {'id': 1},
        'fechaCreacion': '2026-06-16T10:00:00',
      };

      final receta = Receta.fromJson(json);

      expect(receta.pasos, isEmpty);
      expect(receta.ingredientes, isEmpty);
    });

    test('tolera nombre y descripcion ausentes con cadena vacía', () {
      final json = {
        'id': 4,
        'creadaPor': {'id': 1},
        'fechaCreacion': '2026-06-16T10:00:00',
      };

      final receta = Receta.fromJson(json);

      expect(receta.nombre, '');
      expect(receta.descripcion, '');
    });
  });

  group('copyWith', () {
    test('actualiza puedeElaborarse sin perder el resto', () {
      const receta = Receta(
        id: 1,
        nombre: 'Pizza',
        descripcion: 'desc',
        pasos: [],
        ingredientes: [],
        creadaPorId: 42,
        fechaCreacion: '2026-06-16T10:00:00',
      );

      final verificada = receta.copyWith(puedeElaborarse: true);

      expect(verificada.puedeElaborarse, isTrue);
      expect(verificada.nombre, 'Pizza');
      expect(verificada.creadaPorId, 42);
    });
  });
}
