import '../models/movimiento_stock.dart';
import 'api_client.dart';

class MovimientoStockService {
  static Future<List<MovimientoStock>> obtenerMovimientos({
    required List<int> ingredienteIds,
    required DateTime fechaDesde,
    required DateTime fechaHasta,
    required String token,
  }) async {
    final ids = ingredienteIds.join(',');
    final desde = fechaDesde.toIso8601String();
    final hasta = fechaHasta.toIso8601String();

    final data = await ApiClient.get(
      '/movimientos?ingredienteIds=$ids&fechaDesde=$desde&fechaHasta=$hasta',
      token: token,
    );
    return (data as List).map((e) => MovimientoStock.fromJson(e)).toList();
  }
}
