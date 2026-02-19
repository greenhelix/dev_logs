import 'package:data_accumulator_app/core/widgets/custom_image_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:path/path.dart';
import '../../../core/widgets/responsive_list_tile.dart';
import '../../../data/providers.dart';
import '../domain/person_model.dart';

class PersonListScreen extends ConsumerWidget {
  const PersonListScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final personListAsync = ref.watch(personStreamProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Person List')),
      body: personListAsync.when(
        data: (people) => ListView.builder(
          itemCount: people.length,
          itemBuilder: (context, index) {
            final person = people[index];
            return ResponsiveListTile(
              onEdit: () => _showAddOrEditDialog(context, ref, person: person),
              onDelete: () =>
                  ref.read(personRepositoryProvider).deletePerson(person.id),
              child: ListTile(
                leading: CircleAvatar(
                  backgroundImage:
                      person.photoUrl != null && person.photoUrl!.isNotEmpty
                          ? NetworkImage(person.photoUrl!)
                          : null,
                  child: (person.photoUrl == null || person.photoUrl!.isEmpty)
                      ? const Icon(Icons.person)
                      : null,
                ),
                title: Text(person.name),
                subtitle: Text('Age: ${person.age ?? 'N/A'}'),
                onTap: () => context.push('/person/detail', extra: person),
              ),
            );
          },
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('Error: $err')),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () => _showAddOrEditDialog(context, ref),
        child: const Icon(Icons.add),
      ),
    );
  }

  void _showAddOrEditDialog(BuildContext context, WidgetRef ref,
      {PersonModel? person}) {
    final isEdit = person != null;
    final nameCtrl = TextEditingController(text: person?.name ?? '');
    final ageCtrl = TextEditingController(text: person?.age?.toString() ?? '');
    final urlCtrl = TextEditingController(text: person?.photoUrl ?? '');
    String? currenPhotoUrl = person?.photoUrl;

    // Attributes handling (Simple JSON-like string for demo)
    final attrCtrl = TextEditingController(
        text: person?.attributes.entries
                .map((e) => "${e.key}:${e.value}")
                .join(", ") ??
            "");

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(isEdit ? 'Person 수정' : 'Person 추가'),
        content: SingleChildScrollView(
          child: Column(
            children: [
              TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(labelText: 'Name')),
              TextField(
                  controller: ageCtrl,
                  decoration: const InputDecoration(labelText: 'Age'),
                  keyboardType: TextInputType.number),
              CustomImagePicker(
                  initialUrl: currenPhotoUrl,
                  onImageSelected: (url) {
                    currenPhotoUrl = url;
                  }),
              TextField(
                  controller: attrCtrl,
                  decoration: const InputDecoration(
                      labelText: 'Attributes (key:value, k:v)')),
            ],
          ),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context), child: const Text('취소')),
          ElevatedButton(
            onPressed: () async {
              // Parse attributes string back to map
              Map<String, dynamic> newAttributes = {};
              if (attrCtrl.text.isNotEmpty) {
                for (var item in attrCtrl.text.split(',')) {
                  var parts = item.split(':');
                  if (parts.length == 2) {
                    newAttributes[parts[0].trim()] = parts[1].trim();
                  }
                }
              }

              final newPerson = PersonModel(
                // 수정 시 기존 ID 유지, 추가 시 임의의 ID 생성 (또는 Firestore 자동 ID 사용 가능)
                id: isEdit
                    ? person.id
                    : DateTime.now().millisecondsSinceEpoch.toString(),
                name: nameCtrl.text,
                age: int.tryParse(ageCtrl.text),
                photoUrl: currenPhotoUrl,
                attributes: newAttributes,
              );

              if (isEdit) {
                await ref
                    .read(personRepositoryProvider)
                    .updatePerson(newPerson);
              } else {
                await ref.read(personRepositoryProvider).addPerson(newPerson);
              }
              if (context.mounted) Navigator.pop(context);
            },
            child: Text(isEdit ? '수정' : '추가'),
          ),
        ],
      ),
    );
  }
}
