import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:oh_my_freezer/providers/tpv_sku_mapping_provider.dart';

const _mapping1 = {
  'id': 1,
  'skuTpv': 'SKU-001',
  'nombreTpv': 'Producto A',
  'recetaId': null,
  'recetaNombre': null,
  'mapeado': false,
  'fechaCreacion': '2024-01-01T00:00:00',
  'fechaActualizacion': '2024-01-01T00:00:00',
};

const _mapping2 = {
  'id': 2,
  'skuTpv': 'SKU-002',
  'nombreTpv': 'Producto B',
  'recetaId': 10,
  'recetaNombre': 'Receta X',
  'mapeado': true,
  'fechaCreacion': '2024-01-01T00:00:00',
  'fechaActualizacion': '2024-01-02T00:00:00',
};

void main() {
  group('TpvSkuMappingProvider.cargar', () {
    test('carga la lista y separa mapeados de pendientes correctamente', () async {
      final mockClient = MockClient((request) async {
        return http.Response(
          jsonEncode([_mapping1, _mapping2]),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      });

      final provider = TpvSkuMappingProvider();

      await http.runWithClient(() async {
        await provider.cargar('fake-token');
      }, () => mockClient);

      expect(provider.mappings, hasLength(2));
      expect(provider.mappings.where((m) => !m.mapeado), hasLength(1));
      expect(provider.mappings.where((m) => m.mapeado), hasLength(1));
      expect(provider.error, isNull);
    });

    test('almacena el error si el servidor responde con 500', () async {
      final mockClient = MockClient((request) async {
        return http.Response('Error interno', 500);
      });

      final provider = TpvSkuMappingProvider();

      await http.runWithClient(() async {
        await provider.cargar('fake-token');
      }, () => mockClient);

      expect(provider.mappings, isEmpty);
      expect(provider.error, isNotNull);
    });
  });

  group('TpvSkuMappingProvider.asignarReceta', () {
    test('actualiza el mapping en la lista tras asignar receta', () async {
      final mappingActualizado = {
        ..._mapping1,
        'recetaId': 5,
        'recetaNombre': 'Receta Nueva',
        'mapeado': true,
      };

      var callCount = 0;
      final mockClient = MockClient((request) async {
        callCount++;
        if (request.method == 'GET') {
          return http.Response(
            jsonEncode([_mapping1, _mapping2]),
            200,
            headers: {'content-type': 'application/json; charset=utf-8'},
          );
        }
        // PUT
        return http.Response(
          jsonEncode(mappingActualizado),
          200,
          headers: {'content-type': 'application/json; charset=utf-8'},
        );
      });

      final provider = TpvSkuMappingProvider();

      await http.runWithClient(() async {
        await provider.cargar('fake-token');
        await provider.asignarReceta(1, 5, 'fake-token');
      }, () => mockClient);

      expect(provider.mappings.firstWhere((m) => m.id == 1).mapeado, isTrue);
      expect(provider.mappings.firstWhere((m) => m.id == 1).recetaNombre, 'Receta Nueva');
      expect(provider.error, isNull);
    });
  });

  group('TpvSkuMappingProvider.desmapear', () {
    test('recarga la lista tras desmapear (respuesta 204 sin body)', () async {
      final listaRecargada = [
        {..._mapping1},
        {..._mapping2, 'recetaId': null, 'recetaNombre': null, 'mapeado': false},
      ];

      var getCallCount = 0;
      final mockClient = MockClient((request) async {
        if (request.method == 'GET') {
          getCallCount++;
          return http.Response(
            jsonEncode(getCallCount == 1 ? [_mapping1, _mapping2] : listaRecargada),
            200,
            headers: {'content-type': 'application/json; charset=utf-8'},
          );
        }
        // DELETE
        return http.Response('', 204);
      });

      final provider = TpvSkuMappingProvider();

      await http.runWithClient(() async {
        await provider.cargar('fake-token');
        await provider.desmapear(2, 'fake-token');
      }, () => mockClient);

      expect(provider.mappings.where((m) => m.mapeado), isEmpty);
      expect(provider.error, isNull);
    });
  });
}
