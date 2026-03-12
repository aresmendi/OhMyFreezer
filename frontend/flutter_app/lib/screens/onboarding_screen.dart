// lib/screens/onboarding_screen.dart

import 'package:flutter/material.dart';

class OnboardingScreen extends StatefulWidget {
  const OnboardingScreen({super.key});

  @override
  State<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends State<OnboardingScreen> {
  final PageController _pageController = PageController();
  int _currentPage = 0;

  final List<Map<String, String>> onboardingData = [
    {
      "title": "Bienvenido a OhMyFreezer",
      "text": "Gestiona tu cocina como un profesional. Mantén el control total de tu inventario.",
      "image": "assets/images/gorila_chef.png" // Opcional o usar un icono
    },
    {
      "title": "Controla tus Ingredientes",
      "text": "Recibe alertas automáticamente cuando el stock cae por debajo del mínimo.",
      "image": "assets/images/pinguin_chef.png" 
    },
    {
      "title": "Recetas Paso a Paso",
      "text": "Todos los detalles para elaborar platos estandarizados y reducir mermas.",
      "image": "assets/images/skeleton_chef.png" 
    },
  ];

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _skip() {
    _pageController.animateToPage(
      onboardingData.length - 1,
      duration: const Duration(milliseconds: 400),
      curve: Curves.easeIn,
    );
  }

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;

    return Scaffold(
      backgroundColor: cs.surface,
      body: SafeArea(
        child: Column(
          children: [
            // Botón Skip
            Align(
              alignment: Alignment.topRight,
              child: TextButton(
                onPressed: _skip,
                child: const Text('Saltar'),
              ),
            ),
            
            // Carrusel
            Expanded(
              flex: 3,
              child: PageView.builder(
                controller: _pageController,
                onPageChanged: (value) {
                  setState(() {
                    _currentPage = value;
                  });
                },
                itemCount: onboardingData.length,
                itemBuilder: (context, index) => OnboardingContent(
                  image: onboardingData[index]["image"],
                  title: onboardingData[index]["title"],
                  text: onboardingData[index]["text"],
                  cs: cs,
                ),
              ),
            ),

            // Indicador de Progreso
            Expanded(
              flex: 1,
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(
                      onboardingData.length,
                      (index) => buildDot(index: index, cs: cs),
                    ),
                  ),
                  const Spacer(),
                  
                  // Botones de acción (Aparecen todos, o al final)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 20),
                    child: Column(
                      children: [
                        FilledButton(
                          onPressed: () {
                            Navigator.pushNamed(context, '/setup_jefe');
                          },
                          style: FilledButton.styleFrom(
                            minimumSize: const Size.fromHeight(50),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                          child: const Text('Configurar como Jefe de Cocina'),
                        ),
                        const SizedBox(height: 12),
                        OutlinedButton(
                          onPressed: () {
                            Navigator.pushReplacementNamed(context, '/login');
                          },
                          style: OutlinedButton.styleFrom(
                            minimumSize: const Size.fromHeight(50),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                          child: const Text('Ya tengo cuenta (Acceder)'),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  AnimatedContainer buildDot({required int index, required ColorScheme cs}) {
    return AnimatedContainer(
      duration: const Duration(milliseconds: 200),
      margin: const EdgeInsets.only(right: 5),
      height: 6,
      width: _currentPage == index ? 20 : 6,
      decoration: BoxDecoration(
        color: _currentPage == index ? cs.primary : const Color(0xFFD8D8D8),
        borderRadius: BorderRadius.circular(3),
      ),
    );
  }
}

class OnboardingContent extends StatelessWidget {
  const OnboardingContent({
    super.key,
    this.text,
    this.title,
    this.image,
    required this.cs,
  });

  final String? text, title, image;
  final ColorScheme cs;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        const Spacer(),
        // Intentar usar la imagen principal (gorila_chef) u otras dependiendo del index actual
        Image.asset(
          image ?? "assets/images/gorila_chef.png",
          height: 250,
          width: 250,
          errorBuilder: (context, error, stackTrace) {
            return Icon(Icons.kitchen, size: 100, color: cs.primary);
          },
        ),
        const Spacer(),
        Text(
          title!,
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.bold,
                color: cs.onSurface,
              ),
        ),
        const SizedBox(height: 16),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Text(
            text!,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
              color: cs.onSurfaceVariant,
            ),
          ),
        ),
      ],
    );
  }
}
