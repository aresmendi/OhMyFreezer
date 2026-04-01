import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import 'alerta_badge.dart';

/// AppBar reutilizable para todas las pantallas autenticadas.
///
/// - Muestra el [titulo] de la pantalla actual.
/// - Si [mostrarBadge] == true y el usuario es jefe de cocina → [AlertaBadge].
/// - Si [mostrarBack] == true → botón de retroceso personalizado.
/// - [acciones] adicionales se añaden después del badge.
///
/// Implementa [PreferredSizeWidget] para usarse directamente en [Scaffold.appBar].
///
/// Ejemplo:
/// ```dart
/// appBar: AppBarCustom(titulo: 'Ingredientes', mostrarBadge: true),
/// ```
class AppBarCustom extends StatelessWidget implements PreferredSizeWidget {
  final String titulo;
  final bool mostrarBadge;
  final bool mostrarBack;
  final List<Widget> acciones;
  final VoidCallback? onBadgeTap;

  const AppBarCustom({
    super.key,
    required this.titulo,
    this.mostrarBadge = true,
    this.mostrarBack = false,
    this.acciones = const [],
    this.onBadgeTap,
  });

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    final esJefe = context.watch<AuthProvider>().esJefeCocina;

    return AppBar(
      title: Text(titulo),
      centerTitle: false,
      automaticallyImplyLeading: mostrarBack,
      actions: [
        // Badge solo visible para jefe de cocina
        if (mostrarBadge && esJefe)
          AlertaBadge(onTap: onBadgeTap ?? () => _navegarAlertas(context)),

        // Acciones extra que pase cada pantalla
        ...acciones,

        // Menú de usuario (logout)
        _MenuUsuario(),

        const SizedBox(width: 4),
      ],
    );
  }

  void _navegarAlertas(BuildContext context) {
    Navigator.pushNamed(context, '/alertas');
  }
}

/// Menú desplegable con nombre de usuario y opción de cerrar sesión.
class _MenuUsuario extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthProvider>();
    final username = auth.username;
    final esJefe = auth.esJefeCocina;
    final cs = Theme.of(context).colorScheme;

    return PopupMenuButton<_MenuOpcion>(
      icon: CircleAvatar(
        radius: 16,
        backgroundColor: cs.primaryContainer,
        child: Text(
          username?.substring(0, 1).toUpperCase() ?? '?',
          style: TextStyle(
            color: cs.onPrimaryContainer,
            fontWeight: FontWeight.w700,
            fontSize: 14,
          ),
        ),
      ),
      onSelected: (opcion) {
        if (opcion == _MenuOpcion.logout) {
          auth.logout();
          Navigator.pushNamedAndRemoveUntil(context, '/login', (_) => false);
        } else if (opcion == _MenuOpcion.usuarios) {
          Navigator.pushNamed(context, '/usuarios');
        }
      },
      itemBuilder: (_) => [
        PopupMenuItem(
          enabled: false,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                username ?? '',
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
              Text(
                esJefe ? 'Jefe de cocina' : 'Cocinero',
                style: TextStyle(
                  fontSize: 12,
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        if (esJefe) ...[
          const PopupMenuDivider(),
          const PopupMenuItem(
            value: _MenuOpcion.usuarios,
            child: Row(
              children: [
                Icon(Icons.people_outline_rounded, size: 18),
                SizedBox(width: 10),
                Text('Gestión de Personal'),
              ],
            ),
          ),
        ],
        const PopupMenuDivider(),
        const PopupMenuItem(
          value: _MenuOpcion.logout,
          child: Row(
            children: [
              Icon(Icons.logout_rounded, size: 18),
              SizedBox(width: 10),
              Text('Cerrar sesión'),
            ],
          ),
        ),
      ],
    );
  }
}

enum _MenuOpcion { logout, usuarios }
