import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:oh_my_freezer/models/usuario.dart';
import 'package:oh_my_freezer/services/usuario_service.dart';

/// Cubre `UsuarioService.login`, que fue reescrito en la PR9 para autenticar
/// por email en lugar de username. Antes de estos tests, la corrección solo
/// se había verificado leyendo el código fuente — sin ninguna prueba en
/// tiempo de ejecución.
///
/// `ApiClient` llama a las funciones de nivel superior `http.get`/`http.post`
/// (no inyecta un `http.Client`), así que interceptamos esas llamadas con
/// `http.runWithClient`, que sustituye el cliente HTTP dentro de la `Zone`
/// de ejecución por un `MockClient` — sin necesidad de añadir mockito ni
/// otra dependencia de testing al proyecto.
void main() {
  group('UsuarioService.login', () {
    test(
      'envía POST a /usuarios/login con el body {email, password} '
      'y parsea un login correcto en {token, usuario}',
      () async {
        http.Request? capturedRequest;
        Map<String, dynamic>? capturedBody;

        final mockClient = MockClient((request) async {
          capturedRequest = request;
          capturedBody = jsonDecode(request.body) as Map<String, dynamic>;

          return http.Response(
            jsonEncode({
              'token': 'jwt-fake-token',
              'id': 42,
              'username': 'ares',
              'esJefeCocina': true,
              'email': 'ares@ohmyfreezer.com',
            }),
            200,
            headers: {'content-type': 'application/json; charset=utf-8'},
          );
        });

        late Map<String, dynamic> result;
        await http.runWithClient(() async {
          result = await UsuarioService.login(
            email: 'ares@ohmyfreezer.com',
            password: 'supersecreta',
          );
        }, () => mockClient);

        // ── Request shape ──────────────────────────────────────
        expect(capturedRequest!.method, 'POST');
        expect(capturedRequest!.url.path, endsWith('/usuarios/login'));
        expect(capturedBody, {
          'email': 'ares@ohmyfreezer.com',
          'password': 'supersecreta',
        });
        expect(capturedBody!.containsKey('username'), isFalse);

        // ── Response parsing ───────────────────────────────────
        expect(result['token'], 'jwt-fake-token');
        final usuario = result['usuario'] as Usuario;
        expect(usuario.id, 42);
        expect(usuario.username, 'ares');
        expect(usuario.esJefeCocina, isTrue);
        expect(usuario.email, 'ares@ohmyfreezer.com');
      },
    );

    test(
      'propaga un error cuando el backend rechaza las credenciales (401)',
      () async {
        final mockClient = MockClient((request) async {
          return http.Response(
            jsonEncode({'error': 'Credenciales inválidas'}),
            401,
            headers: {'content-type': 'application/json; charset=utf-8'},
          );
        });

        await http.runWithClient(() async {
          await expectLater(
            UsuarioService.login(
              email: 'ares@ohmyfreezer.com',
              password: 'incorrecta',
            ),
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

    test(
      'propaga un error cuando el backend responde con un fallo de servidor (500)',
      () async {
        final mockClient = MockClient((request) async {
          return http.Response('Internal Server Error', 500);
        });

        await http.runWithClient(() async {
          await expectLater(
            UsuarioService.login(
              email: 'ares@ohmyfreezer.com',
              password: 'cualquiera',
            ),
            throwsA(
              isA<Exception>().having(
                (e) => e.toString(),
                'message',
                contains('Error interno del servidor'),
              ),
            ),
          );
        }, () => mockClient);
      },
    );
  });
}
