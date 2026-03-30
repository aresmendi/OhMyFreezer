import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../providers/auth_provider.dart';
import '../providers/alertas_provider.dart';
import '../providers/ingredientes_provider.dart';
import '../providers/recetas_provider.dart';
import '../providers/estadisticas_provider.dart';
import '../widgets/app_bar_custom.dart';
import '../theme/app_assets.dart';

// Tabs
import 'ingredientes/ingredientes_list_screen.dart';
import 'recetas/recetas_list_screen.dart';
import 'estadisticas/estadisticas_screen.dart';
import 'alertas/alertas_screen.dart';
import 'favoritos/favoritos_screen.dart';

/// Pantalla principal de OhMyFreezer tras el login.
///
/// Estructura:
/// - [BottomNavigationBar] con tabs según el rol del usuario.
/// - Cocinero     → Recetas | Ingredientes
/// - Jefe cocina  → Recetas | Ingredientes | Estadísticas | Alertas
///
/// Al montarse:
/// 1. Registra los providers necesarios según el rol.
/// 2. Inicia el polling de alertas si es jefe de cocina.
/// 3. Carga recetas e ingredientes en paralelo.
class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _tabIndex = 0;
  bool _iniciado = false;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (!_iniciado) {
      _iniciado = true;
      _iniciarProviders();
    }
  }

  // ─── Inicialización ──────────────────────────────────────────

  Future<void> _iniciarProviders() async {
    final auth = context.read<AuthProvider>();
    final token = auth.token!;

    // Carga en paralelo recetas e ingredientes
    await Future.wait([
      context.read<RecetasProvider>().cargar(token),
      context.read<IngredientesProvider>().cargar(token),
    ]);

    if (!mounted) return;

    // Solo jefe de cocina: estadísticas + polling de alertas
    if (auth.esJefeCocina) {
      context.read<EstadisticasProvider>().cargar(token);
      context.read<AlertasProvider>().iniciarPolling(token);
    }
  }

  // ─── Tabs según rol ──────────────────────────────────────────

  List<_TabItem> _tabs(bool esJefe) => [
    const _TabItem(
      label: 'Recetas',
      icon: Icons.menu_book_outlined,
      iconActivo: Icons.menu_book_rounded,
      screen: RecetasListScreen(),
    ),
    const _TabItem(
      label: 'Ingredientes',
      icon: Icons.kitchen_outlined,
      iconActivo: Icons.kitchen_rounded,
      screen: IngredientesListScreen(),
    ),
    const _TabItem(
      label: 'Favoritos',
      icon: Icons.star_border_rounded,
      iconActivo: Icons.star_rounded,
      screen: FavoritosScreen(),
    ),
    if (esJefe) ...[
      const _TabItem(
        label: 'Estadísticas',
        icon: Icons.bar_chart_outlined,
        iconActivo: Icons.bar_chart_rounded,
        screen: EstadisticasScreen(),
      ),
      const _TabItem(
        label: 'Alertas',
        icon: Icons.notifications_outlined,
        iconActivo: Icons.notifications_rounded,
        screen: AlertasScreen(),
      ),
    ],
  ];

  // ─── UI ──────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final esJefe = auth.esJefeCocina;
    final tabs = _tabs(esJefe);

    // Clamp por si el rol cambia en caliente (edge case)
    if (_tabIndex >= tabs.length) _tabIndex = 0;

    return Scaffold(
      appBar: AppBarCustom(
        titulo: tabs[_tabIndex].label,
        mostrarBadge: esJefe,
        onBadgeTap: esJefe
            ? () =>
                  setState(() => _tabIndex = tabs.length - 1) // tab Alertas
            : null,
      ),
      body: _Body(screen: tabs[_tabIndex].screen),
      bottomNavigationBar: _BottomNav(
        tabs: tabs,
        index: _tabIndex,
        esJefe: esJefe,
        onChange: (i) => setState(() => _tabIndex = i),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Subwidgets privados
// ─────────────────────────────────────────────────────────────────────────────

/// Cuerpo principal: muestra la pantalla activa con animación de fade.
class _Body extends StatelessWidget {
  final Widget screen;
  const _Body({required this.screen});

  @override
  Widget build(BuildContext context) {
    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 220),
      switchInCurve: Curves.easeIn,
      switchOutCurve: Curves.easeOut,
      child: KeyedSubtree(key: ValueKey(screen.runtimeType), child: screen),
    );
  }
}

/// BottomNavigationBar con badge en el tab de Alertas.
class _BottomNav extends StatelessWidget {
  final List<_TabItem> tabs;
  final int index;
  final bool esJefe;
  final ValueChanged<int> onChange;

  const _BottomNav({
    required this.tabs,
    required this.index,
    required this.esJefe,
    required this.onChange,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final contadorAlertas = esJefe
        ? context.watch<AlertasProvider>().contadorNoLeidas
        : 0;

    return NavigationBar(
      selectedIndex: index,
      onDestinationSelected: onChange,
      indicatorColor: cs.primaryContainer,
      destinations: tabs.asMap().entries.map((entry) {
        final i = entry.key;
        final tab = entry.value;
        final bool esAlertas = esJefe && i == tabs.length - 1;

        return NavigationDestination(
          icon: Badge(
            isLabelVisible: esAlertas && contadorAlertas > 0,
            label: Text(
              contadorAlertas > 99 ? '99+' : '$contadorAlertas',
              style: const TextStyle(fontSize: 10),
            ),
            child: Icon(tab.icon),
          ),
          selectedIcon: Badge(
            isLabelVisible: esAlertas && contadorAlertas > 0,
            label: Text(
              contadorAlertas > 99 ? '99+' : '$contadorAlertas',
              style: const TextStyle(fontSize: 10),
            ),
            child: Icon(tab.iconActivo),
          ),
          label: tab.label,
        );
      }).toList(),
    );
  }
}

/// Widget de estado vacío genérico reutilizable en cualquier tab.
///
/// Usa [skeleton_chef] como ilustración de "sin datos".
/// Exportado para que las screens hijas puedan usarlo.
class EmptyState extends StatelessWidget {
  final String titulo;
  final String subtitulo;
  final VoidCallback? onRecargar;

  const EmptyState({
    super.key,
    required this.titulo,
    required this.subtitulo,
    this.onRecargar,
  });

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 40),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Image.asset(
              AppAssets.skeletonChef,
              height: 160,
              fit: BoxFit.contain,
            ),
            const SizedBox(height: 24),
            Text(
              titulo,
              style: tt.titleMedium?.copyWith(fontWeight: FontWeight.w700),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              subtitulo,
              style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
            if (onRecargar != null) ...[
              const SizedBox(height: 24),
              OutlinedButton.icon(
                onPressed: onRecargar,
                icon: const Icon(Icons.refresh_rounded),
                label: const Text('Reintentar'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
// Modelo interno de tab
// ─────────────────────────────────────────────────────────────────────────────

class _TabItem {
  final String label;
  final IconData icon;
  final IconData iconActivo;
  final Widget screen;

  const _TabItem({
    required this.label,
    required this.icon,
    required this.iconActivo,
    required this.screen,
  });
}
