import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../providers/usuarios_provider.dart';
import '../home_screen.dart'; // EmptyState

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
      appBar: AppBar(
        title: const Text('Gestión de Personal'),
      ),
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
                    return ListTile(
                      leading: CircleAvatar(
                        child: Icon(usu.esJefeCocina ? Icons.admin_panel_settings : Icons.person),
                      ),
                      title: Text(usu.username),
                      subtitle: Text(usu.esJefeCocina ? 'Jefe de Cocina' : 'Empleado'),
                      trailing: usu.id == auth.usuarioId ? const Text('Tú', style: TextStyle(fontWeight: FontWeight.bold)) : null,
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
}
