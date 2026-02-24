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
          // Manual refresh button in AppBar
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: '새로고침',
            onPressed: () => ref.invalidate(personStreamProvider),
          ),
        ],
      ),
      body: personListAsync.when(
        // ✅ Pull-to-refresh: RefreshIndicator로 전체 감싸기
        data: (people) => RefreshIndicator(
          onRefresh: () async {
            ref.invalidate(personStreamProvider);
            // Stream 재구독 대기 (짧은 딜레이로 스피너 유지)
            await Future.delayed(const Duration(milliseconds: 500));
          },
          child: people.isEmpty
              // ✅ 빈 리스트에서도 스와이프 제스처 동작하도록 ListView 유지
              ? ListView(
                  physics: const AlwaysScrollableScrollPhysics(),
                  children: const [
                    SizedBox(height: 200),
                    Center(child: Text('등록된 인물이 없습니다.')),
                  ],
                )
              : ListView.builder(
                  // ✅ 스크롤 짧아도 당기기 제스처 동작
                  physics: const AlwaysScrollableScrollPhysics(),
                  itemCount: people.length,
                  itemBuilder: (context, index) {
                    final person = people[index];
                    return ResponsiveListTile(
                      onEdit: () =>
                          _showAddOrEditDialog(context, ref, person: person),
                      onDelete: () => ref
                          .read(personRepositoryProvider)
                          .deletePerson(person.id),
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
    Map<String, dynamic> currentAttributes =
        Map.from(person?.attributes ?? {});

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => StatefulBuilder(
        builder: (context, setState) {
          return AlertDialog(
            // ✅ 타이틀도 "수정" → "편집"으로 통일 (선택사항)
            title: Text(isEdit ? 'Person 편집' : 'Person 추가'),
            content: SizedBox(
              width: double.maxFinite,
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    // Profile image picker (circular)
                    CustomImagePicker(
                      initialUrl: currentPhotoUrl,
                      onImageSelected: (url) {
                        currentPhotoUrl = url;
                      },
                      isCircle: true,
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

                  // ✅ 저장/수정 완료 후 Provider 강제 갱신
                  // Firestore Stream이 실시간 반영 안 될 경우 대비
                  ref.invalidate(personStreamProvider);

                  if (context.mounted) Navigator.pop(context);
                },
                // ✅ "수정" → "저장" 으로 텍스트 통일
                child: const Text('저장'),
              ),
            ],
          );
        },
      ),
    );
  }
}
