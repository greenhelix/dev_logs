import 'package:data_accumulator_app/features/person/domain/person_model.dart';
import 'package:flutter/material.dart';
import '../../../data/local/app_database.dart';

class PersonDetailScreen extends StatelessWidget {
  final PersonModel person;
  // final Person person;

  const PersonDetailScreen({super.key, required this.person});

  @override
  Widget build(BuildContext context) {
    final String name = (person.name as String) ?? 'Unknown';
    final int? age = person.age;
    final String? photoUrl = person.photoUrl;

    final Map<String, dynamic> attributes = person.attributes;

    return Scaffold(
      appBar: AppBar(title: Text(name)),
      body: SingleChildScrollView(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              height: 250,
              color: Colors.grey.shade200,
              child: photoUrl != null && photoUrl.isNotEmpty
                  ? Image.network(
                      photoUrl,
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, StackTrace) => const Icon(
                          Icons.person,
                          size: 100,
                          color: Colors.grey),
                    )
                  : const Icon(Icons.person, size: 100, color: Colors.grey),
            ),
            Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text("Base Info",
                        style: TextStyle(
                            fontSize: 20, fontWeight: FontWeight.bold)),
                    const Divider(),
                    _buildInfoRow("Name", name),
                    _buildInfoRow("Age", age?.toString() ?? "Unknow"),
                    const SizedBox(height: 24),
                    if (attributes.isNotEmpty) ...[
                      const Text(
                        "Wiki Attributes",
                        style: TextStyle(
                            fontSize: 20, fontWeight: FontWeight.bold),
                      ),
                      const Divider(),
                      ...attributes.entries.map((entry) {
                        return _buildInfoRow(entry.key, entry.value.toString());
                      }),
                    ] else
                      const Text("No Additional Wiki Data",
                          style: TextStyle(color: Colors.grey)),
                  ],
                ))
          ],
        ),
      ),
    );
  }
}

Widget _buildInfoRow(String label, String value) {
  return Padding(
    padding: const EdgeInsets.symmetric(vertical: 8.0),
    child: Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 100,
          child: Text(
            label,
            style: const TextStyle(
                fontWeight: FontWeight.w600, color: Colors.black54),
          ),
        ),
        Expanded(
            child: Text(
          value,
          style: const TextStyle(fontSize: 16),
        ))
      ],
    ),
  );
}
