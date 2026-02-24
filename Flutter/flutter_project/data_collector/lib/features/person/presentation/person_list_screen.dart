import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/widgets/responsive_list_tile.dart';
import '../../../core/widgets/custom_image_picker.dart';
import '../../../core/widgets/attribute_input_widget.dart';
import '../../../data/providers.dart';
import '../domain/person_model.dart';

class PersonListScreen extends ConsumerWidget {
  const PersonListScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final personListAsync = ref.watch(personStreamProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Person List'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: '새로고침',
            onPressed: () => ref.invalidate(personStreamProvider),
          ),
        ],
      ),
      body: personListAsync.when(
        data: (people) => RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(personStreamProvider);
            await Future.delayed(const Duration(milliseconds: 500));
          },
          child: people.isEmpty
              ? ListView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  children: const [
                    SizedBox(height: 200),
                    Center(child: Text('등록된 인물이 없습니다.', style: TextStyle(color: Colors.grey))),
                  ],
                )
              : ListView.builder(
                  physics: const AlwaysScrollableScrollPhysics(),
                  itemCount: people.length,
                  itemBuilder: (context, index) {
                    final person = people[index];
                    return ResponsiveListTile(
                      onEdit: () =>
                          _showAddOrEditDialog(context, ref, person: person),
                      onDelete: () async {
                        await ref
                            .read(personRepositoryProvider)
                            .deletePerson(person.id);
                        ref.invalidate(personStreamProvider);
                      },
                      child: ListTile(
                        leading: CircleAvatar(
                          backgroundImage:
                              person.photoUrl != null &&
                                      person.photoUrl!.isNotEmpty
                                  ? NetworkImage(person.photoUrl!)
                                  : null,
                          child:
                              (person.photoUrl == null ||
                                      person.photoUrl!.isEmpty)
                                  ? const Icon(Icons.person)
                                  : null,
                        ),
                        title: Text(person.name),
                        subtitle: Text('Age: ${person.age ?? 'N/A'}'),
                        onTap: () =>
                            context.push('/person/detail', extra: person),
                      ),
                    );
                  },
                ),
        ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text('Error: $err'),
              const SizedBox(height: 12),
              ElevatedButton.icon(
                onPressed: () => ref.invalidate(personStreamProvider),
                icon: const Icon(Icons.refresh),
                label: const Text('다시 시도'),
              ),
            ],
          ),
        ),
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
    final ageCtrl =
        TextEditingController(text: person?.age?.toString() ?? '');
    String? currentPhotoUrl = person?.photoUrl;
    Map<String, String> currentAttributes =
        Map.from(person?.attributes ?? {});

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) {
          return AlertDialog(
            title: Text(isEdit ? 'Person 수정' : 'Person 추가'),
            content: SizedBox(
              width: double.maxFinite,
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Center(
                      child: CustomImagePicker(
                        initialUrl: currentPhotoUrl,
                        onImageSelected: (url) {
                          currentPhotoUrl = url;
                        },
                        isCircle: true,
                      ),
                    ),
                    const SizedBox(height: 16),
                    TextField(
                      controller: nameCtrl,
                      decoration:
                          const InputDecoration(labelText: '이름 (Name)'),
                    ),
                    TextField(
                      controller: ageCtrl,
                      decoration:
                          const InputDecoration(labelText: '나이 (Age)'),
                      keyboardType: TextInputType.number,
                    ),
                    const SizedBox(height: 24),
                    const Align(
                      alignment: Alignment.centerLeft,
                      child: Text(
                        '추가 속성 (Attributes)',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ),
                    const SizedBox(height: 8),
                    AttributeInputWidget(
                      initialAttributes: currentAttributes,
                      onChanged: (newMap) {
                        currentAttributes = newMap;
                      },
                    ),
                  ],
                ),
              ),
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('취소'),
              ),
              ElevatedButton(
                onPressed: () async {
                  if (nameCtrl.text.trim().isEmpty) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('이름을 입력해주세요.')),
                    );
                    return;
                  }

                  final newPerson = PersonModel(
                    id: isEdit
                        ? person!.id
                        : DateTime.now()
                            .millisecondsSinceEpoch
                            .toString(),
                    name: nameCtrl.text.trim(),
                    age: int.tryParse(ageCtrl.text),
                    photoUrl: currentPhotoUrl,
                    attributes: currentAttributes,
                  );

                  if (isEdit) {
                    await ref
                        .read(personRepositoryProvider)
                        .updatePerson(newPerson);
                  } else {
                    await ref
                        .read(personRepositoryProvider)
                        .addPerson(newPerson);
                  }

                  ref.invalidate(personStreamProvider);

                  if (context.mounted) Navigator.pop(context);
                },
                child: const Text('저장'),
              ),
            ],
          );
        },
      ),
    );
  }
}
