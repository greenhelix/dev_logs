import 'package:flutter/material.dart';
import '../domain/person_model.dart';
// Import the custom network image widget
import '../../../core/widgets/custom_network_image.dart';

class PersonDetailScreen extends StatelessWidget {
  final PersonModel person;

  const PersonDetailScreen({Key? key, required this.person}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(person.name)),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            // Replaced CircleAvatar with Container + CustomNetworkImage for safety
            Container(
              width: 120,
              height: 120,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.grey[200],
              ),
              clipBehavior: Clip.hardEdge,
              child: (person.photoUrl != null && person.photoUrl!.isNotEmpty)
                  ? CustomNetworkImage(
                      imageUrl: person.photoUrl!,
                      width: 120,
                      height: 120,
                      fit: BoxFit.cover,
                    )
                  : const Icon(Icons.person, size: 60, color: Colors.grey),
            ),
            const SizedBox(height: 20),
            _buildInfoTile('이름', person.name),
            _buildInfoTile('나이', person.age?.toString() ?? '정보 없음'),
            _buildInfoTile('ID', person.id),

            const Divider(),
            const Text('추가 속성 (Attributes)',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            const SizedBox(height: 10),

            if (person.attributes.isEmpty)
              const Text('없음', style: TextStyle(color: Colors.grey))
            else
              ...person.attributes.entries
                  .map((e) => _buildInfoTile(e.key, e.value.toString())),
          ],
        ),
      ),
    );
  }

  // Helper method to build info rows
  Widget _buildInfoTile(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
              width: 100,
              child: Text(label,
                  style: const TextStyle(
                      fontWeight: FontWeight.bold, color: Colors.grey))),
          Expanded(child: Text(value, style: const TextStyle(fontSize: 16))),
        ],
      ),
    );
  }
}
