import 'package:data_accumulator_app/features/person/data/person_repository.dart';
import 'package:drift/src/dsl/dsl.dart' hide Column;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../data/person_repository.dart';
import '../../../data/local/app_database.dart'; // 데이터 모델(Person) 사용을 위해

// ─── Controller (Provider) ───────────────────
// 화면에 데이터를 공급하는 역할 (비동기 데이터)
final personListProvider =
    FutureProvider.autoDispose<List<Person>>((ref) async {
  final repository = ref.watch(personRepositoryProvider);
  return repository.getAllPeople();
});

// ─── Screen (UI) ─────────────────────────────
class PersonListScreen extends ConsumerWidget {
  const PersonListScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // Provider의 상태(로딩중, 에러, 데이터있음)를 구독
    final personListAsync = ref.watch(personListProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text("Person Wiki"),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              // 목록 새로고침
              ref.invalidate(personListProvider);
            },
          )
        ],
      ),
      // 데이터 상태에 따라 다른 UI 보여주기 (.when)
      body: personListAsync.when(
        data: (people) => people.isEmpty
            ? const Center(child: Text("No people yet. Add one!"))
            : ListView.builder(
                itemCount: people.length,
                itemBuilder: (context, index) {
                  final person = people[index];
                  final String? name = person.name as String?;
                  final String nameStr = name ?? '';
                  final firstChar = nameStr.isNotEmpty
                      ? nameStr.substring(0, 1).toUpperCase()
                      : '?';
                  return ListTile(
                    leading: CircleAvatar(
                      child: Text(firstChar), // 이름 첫 글자
                    ),
                    title: Text(person.name as String),
                    subtitle: Text("Age: ${person.age ?? 'Unknown'}"),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () {
                      // TODO: 상세 화면 이동
                    },
                  );
                },
              ),
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text("Error: $err")),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          // 인물 추가 다이얼로그 띄우기 (다음 단계에서 상세 구현)
          _showAddPersonDialog(context, ref);
        },
        child: const Icon(Icons.add),
      ),
    );
  }

  // 간단한 추가 다이얼로그 (임시 구현)
  void _showAddPersonDialog(BuildContext context, WidgetRef ref) {
    final nameController = TextEditingController();
    final ageController = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("Add Person"),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: nameController,
              decoration: const InputDecoration(labelText: "Name"),
            ),
            TextField(
              controller: ageController,
              decoration: const InputDecoration(labelText: "Age"),
              keyboardType: TextInputType.number,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("Cancel"),
          ),
          ElevatedButton(
            onPressed: () async {
              final name = nameController.text;
              final age = int.tryParse(ageController.text);

              if (name.isNotEmpty) {
                // 저장소 호출
                await ref.read(personRepositoryProvider).addPerson(
                      name: name,
                      age: age,
                    );

                // 목록 새로고침 (invalidate)
                ref.invalidate(personListProvider);

                if (context.mounted) Navigator.pop(context);
              }
            },
            child: const Text("Save"),
          ),
        ],
      ),
    );
  }
}
