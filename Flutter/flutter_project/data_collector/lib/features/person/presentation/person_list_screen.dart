import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/widgets/responsive_list_tile.dart';
import '../../../core/widgets/custom_image_picker.dart'; // 이미지 피커
import '../../../core/widgets/attribute_input_widget.dart'; // 속성 입력
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

    // 상태 변수 (이미지 & 속성)
    String? currentPhotoUrl = person?.photoUrl;
    Map<String, dynamic> currentAttributes = person?.attributes ?? {};

    showDialog(
      context: context,
      barrierDismissible: false, // 실수로 닫기 방지
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
                    // 1. 이미지 피커 (원형 모드)
                    CustomImagePicker(
                      initialUrl: currentPhotoUrl,
                      onImageSelected: (url) {
                        currentPhotoUrl = url;
                      },
                      isCircle: true, // 프로필용 원형
                    ),
                    const SizedBox(height: 16),

                    // 2. 기본 정보 입력
                    TextField(
                        controller: nameCtrl,
                        decoration:
                            const InputDecoration(labelText: '이름 (Name)')),
                    TextField(
                        controller: ageCtrl,
                        decoration:
                            const InputDecoration(labelText: '나이 (Age)'),
                        keyboardType: TextInputType.number),
                    const SizedBox(height: 24),

                    // 3. 속성 입력 위젯 (개선된 버전)
                    const Align(
                      alignment: Alignment.centerLeft,
                      child: Text("추가 속성 (Attributes)",
                          style: TextStyle(fontWeight: FontWeight.bold)),
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
                  child: const Text('취소')),
              ElevatedButton(
                onPressed: () async {
                  final newPerson = PersonModel(
                    id: isEdit
                        ? person!.id
                        : DateTime.now().millisecondsSinceEpoch.toString(),
                    name: nameCtrl.text,
                    age: int.tryParse(ageCtrl.text),
                    photoUrl: currentPhotoUrl,
                    attributes: currentAttributes, // 바로 사용
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

                  if (context.mounted) Navigator.pop(context);
                },
                child: Text(isEdit ? '수정' : '추가'),
              ),
            ],
          );
        },
      ),
    );
  }
}
