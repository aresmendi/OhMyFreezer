import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:oh_my_freezer/providers/unidades_provider.dart';

/// Cubre `UnidadesProvider`, que mantiene en memoria el catálogo global e
/// inmutable de unidades de medida (Fase 2 "unidades-medida") y expone
/// `porTipo` para alimentar los selectores del formulario de ingrediente.
void main() {
  group('UnidadesProvider.cargar', () {
    test('carga el catálogo y porTipo filtra solo las unidades de ese tipo', () async {
      final mockClient = MockClient((request) async {
        return http.Response(
          jsonEncode([
            {'id': 1, 'codigo': 'kg', 'nombre': 'Kilogramo', 'tipo': 'MASA', 'factorABase': 1000.0},
            {'id': 2, 'codigo': 'g', 'nombre': 'Gramo', 'tipo': 'MASA', 'factorABase': 1.0},
            {'id': 3, 'codigo': 'ml', 'nombre': 'Mililitro', 'tipo': 'VOLUMEN', 'factorABase': 1.0},
            {'id': 5, 'codigo': 'ud', 'nombre': 'Unidad', 'tipo': 'UNIDAD', 'factorABase': 1.0},
          ]),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      });

      final provider = UnidadesProvider();

      await http.runWithClient(() async {
        await provider.cargar('fake-token');
      }, () => mockClient);

      expect(provider.todas, hasLength(4));
      expect(provider.porTipo('MASA').map((u) => u.codigo), containsAll(['kg', 'g']));
      expect(provider.porTipo('MASA'), hasLength(2));
      expect(provider.porTipo('VOLUMEN'), hasLength(1));
      expect(provider.porTipo('VOLUMEN').single.codigo, 'ml');
    });

    test('una segunda llamada a cargar() no repite la petición HTTP (catálogo cacheado)', () async {
      var callCount = 0;
      final mockClient = MockClient((request) async {
        callCount++;
        return http.Response(
          jsonEncode([
            {'id': 1, 'codigo': 'kg', 'nombre': 'Kilogramo', 'tipo': 'MASA', 'factorABase': 1000.0},
          ]),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      });

      final provider = UnidadesProvider();

      await http.runWithClient(() async {
        await provider.cargar('fake-token');
        await provider.cargar('fake-token');
      }, () => mockClient);

      expect(callCount, 1);
      expect(provider.todas, hasLength(1));
    });
  });
}
