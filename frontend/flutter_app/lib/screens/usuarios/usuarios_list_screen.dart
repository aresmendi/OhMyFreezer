import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../providers/usuarios_provider.dart';
import '../home_screen.dart';

class UsuariosListScreen extends StatefulWidget {
  const UsuariosListScreen({super.key});

  @override
  State<UsuariosListScreen> createState() => _UsuariosListScreenState();
}

class _UsuariosListScreenState extends State<UsuariosListScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      final auth = context.read<AuthProvider>();
      context.read<UsuariosProvider>().cargar(auth.token!);
    });
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<UsuariosProvider>();
    final auth = context.read<AuthProvider>();

    return Scaffold(
      appBar: AppBar(title: const Text('Gestión de Personal')),
      body: provider.isLoading
          ? const Center(child: CircularProgressIndicator())
          : provider.usuarios.isEmpty
          ? const EmptyState(
              titulo: 'No hay más usuarios',
              subtitulo: 'Añade empleados para que puedan gestionar el stock.',
            )
          : ListView.builder(
              itemCount: provider.usuarios.length,
              itemBuilder: (context, index) {
                final usu = provider.usuarios[index];
                final esMiUsuario = usu.id == auth.usuarioId;
                final esEmpleado = !usu.esJefeCocina;

                return ListTile(
                  leading: CircleAvatar(
                    child: Icon(
                      usu.esJefeCocina
                          ? Icons.admin_panel_settings
                          : Icons.person,
                    ),
                  ),
                  title: Text(usu.username),
                  subtitle: Text(
                    usu.esJefeCocina ? 'Jefe de Cocina' : 'Empleado',
                  ),
                  trailing: esMiUsuario
                      ? const Text(
                          'Tú',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        )
                      : esEmpleado && auth.esJefeCocina
                      ? IconButton(
                          icon: const Icon(
                            Icons.delete_outline,
                            color: Colors.red,
                          ),
                          onPressed: () =>
                              _confirmarEliminar(context, usu.id, usu.username),
                        )
                      : null,
                );
              },
            ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => Navigator.pushNamed(context, '/usuarios/nuevo'),
        icon: const Icon(Icons.person_add_rounded),
        label: const Text('Nuevo Empleado'),
      ),
    );
  }

  Future<void> _confirmarEliminar(
    BuildContext context,
    int id,
    String username,
  ) async {
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Eliminar empleado'),
        content: Text(
          '¿Eliminar a "$username"? Esta acción no se puede deshacer.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(context, true),
            child: const Text('Eliminar'),
          ),
        ],
      ),
    );

    if (confirmar == true && context.mounted) {
      final auth = context.read<AuthProvider>();
      try {
        await context.read<UsuariosProvider>().eliminar(id, auth.token!);
        if (context.mounted) {
          ScaffoldMessenger.of(
            context,
          ).showSnackBar(SnackBar(content: Text('$username eliminado')));
        }
      } catch (e) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red),
          );
        }
      }
    }
  }
}
