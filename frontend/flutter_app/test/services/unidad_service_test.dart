import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:oh_my_freezer/models/unidad_medida.dart';
import 'package:oh_my_freezer/services/unidad_service.dart';

/// Cubre `UnidadService.getAll`, que consulta el catálogo global de
/// unidades de medida (`GET /api/unidades`, Fase 2 "unidades-medida").
///
/// `ApiClient` llama a las funciones de nivel superior `http.get` (no
/// inyecta un `http.Client`), así que interceptamos esa llamada con
/// `http.runWithClient`, igual que en `usuario_service_test.dart`.
void main() {
  group('UnidadService.getAll', () {
    test(
      'envía GET a /unidades con el token y parsea el catálogo completo',
      () async {
        http.Request? capturedRequest;

        final mockClient = MockClient((request) async {
          capturedRequest = request;
          return http.Response(
            jsonEncode([
              {'id': 1, 'codigo': 'kg', 'nombre': 'Kilogramo', 'tipo': 'MASA', 'factorABase': 1000.0},
              {'id': 2, 'codigo': 'g', 'nombre': 'Gramo', 'tipo': 'MASA', 'factorABase': 1.0},
              {'id': 5, 'codigo': 'ud', 'nombre': 'Unidad', 'tipo': 'UNIDAD', 'factorABase': 1.0},
            ]),
            200,
            headers: {'content-type': 'application/json; charset=utf-8'},
          );
        });

        late List<UnidadMedida> result;
        await http.runWithClient(() async {
          result = await UnidadService.getAll('fake-token');
        }, () => mockClient);

        expect(capturedRequest!.method, 'GET');
        expect(capturedRequest!.url.path, endsWith('/unidades'));
        expect(capturedRequest!.headers['Authorization'], 'Bearer fake-token');

        expect(result, hasLength(3));
        expect(result[0].codigo, 'kg');
        expect(result[0].tipo, 'MASA');
        expect(result[2].codigo, 'ud');
        expect(result[2].tipo, 'UNIDAD');
      },
    );

    test(
      'propaga un error cuando el backend rechaza la petición sin token (401)',
      () async {
        final mockClient = MockClient((request) async {
          return http.Response(
            jsonEncode({'error': 'No autorizado'}),
            401,
            headers: {'content-type': 'application/json; charset=utf-8'},
          );
        });

        await http.runWithClient(() async {
          await expectLater(
            UnidadService.getAll('token-invalido'),
            throwsA(
              isA<Exception>().having(
                (e) => e.toString(),
                'message',
                contains('No autorizado'),
              ),
            ),
          );
        }, () => mockClient);
      },
    );
  });
}
