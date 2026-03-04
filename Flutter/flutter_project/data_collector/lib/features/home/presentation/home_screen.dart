import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

// The Home Screen provides navigation to the core modules of Data Collector.
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text("Data Collector"),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              "Select Module",
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 20),

            // Use LayoutBuilder to create a responsive grid that adjusts column count
            // based on the available maximum width.
            Expanded(
              child: LayoutBuilder(
                builder: (context, constraints) {
                  int crossAxisCount = 2; // Default for mobile/tablet
                  double childAspectRatio = 1.0;

                  // If screen is wide (e.g., PC/Web), show 3 items in a row
                  if (constraints.maxWidth > 800) {
                    crossAxisCount = 3;
                    childAspectRatio = 1.5; // Make cards wider, less tall on PC
                  }
                  // If screen is very narrow (small mobile), show 1 item in a row
                  else if (constraints.maxWidth < 400) {
                    crossAxisCount = 1;
                    childAspectRatio = 2.0;
                  }

                  return GridView.count(
                    crossAxisCount: crossAxisCount,
                    crossAxisSpacing: 16,
                    mainAxisSpacing: 16,
                    childAspectRatio: childAspectRatio,
                    children: [
                      _MenuCard(
                        icon: Icons.person_search,
                        title: "Person Wiki",
                        color: Colors.blue.shade100,
                        onTap: () {
                          context.go('/person');
                        },
                      ),
                      _MenuCard(
                        icon: Icons.newspaper,
                        title: "News Archive",
                        color: Colors.green.shade100,
                        onTap: () {
                          context.go('/news');
                        },
                      ),
                      _MenuCard(
                        icon: Icons.map,
                        title: "Geo Tracker",
                        color: Colors.orange.shade100,
                        onTap: () {
                          // Note: Matches the existing routing path mapped to Geo Tracker
                          context.go('/maps');
                        },
                      ),
                    ],
                  );
                },
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ─── Sub Widget: Menu Card ───────────────────
class _MenuCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final Color color;
  final VoidCallback onTap;

  const _MenuCard({
    required this.icon,
    required this.title,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        decoration: BoxDecoration(
          color: color,
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.1),
              blurRadius: 10,
              offset: const Offset(0, 4),
            ),
          ],
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icon, size: 48, color: Colors.black87),
            const SizedBox(height: 12),
            Text(
              title,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
