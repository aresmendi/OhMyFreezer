import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/auth_provider.dart';
import '../../providers/usuarios_provider.dart';

class UsuarioFormScreen extends StatefulWidget {
  const UsuarioFormScreen({super.key});

  @override
  State<UsuarioFormScreen> createState() => _UsuarioFormScreenState();
}

class _UsuarioFormScreenState extends State<UsuarioFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _usernameCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  bool _esJefe = false;

  @override
  Widget build(BuildContext context) {
    final isLoading = context.watch<UsuariosProvider>().isLoading;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Registrar Empleado'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              TextFormField(
                controller: _usernameCtrl,
                decoration: const InputDecoration(
                  labelText: 'Nombre de usuario',
                  prefixIcon: Icon(Icons.person_outline),
                ),
                validator: (v) => v!.isEmpty ? 'Requerido' : null,
              ),
              const SizedBox(height: 16),
              TextFormField(
                controller: _passwordCtrl,
                decoration: const InputDecoration(
                  labelText: 'Contraseña temporal',
                  prefixIcon: Icon(Icons.lock_outline),
                ),
                obscureText: true,
                validator: (v) => v!.length < 4 ? 'Mínimo 4 caracteres' : null,
              ),
              const SizedBox(height: 16),
              SwitchListTile(
                title: const Text('¿Es Jefe de Cocina?'),
                subtitle: const Text('Tendrá permisos para editar recetas y personal.'),
                value: _esJefe,
                onChanged: (val) => setState(() => _esJefe = val),
              ),
              const Spacer(),
              FilledButton(
                onPressed: isLoading ? null : _guardar,
                child: const Text('Crear Usuario'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;

    final provider = context.read<UsuariosProvider>();
    final auth = context.read<AuthProvider>();

    try {
      await provider.registrar(
        _usernameCtrl.text.trim(),
        _passwordCtrl.text,
        _esJefe,
        auth.token!,
      );
      if (mounted) Navigator.pop(context);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: ${e.toString()}')),
        );
      }
    }
  }
}
