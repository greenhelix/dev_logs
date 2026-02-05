import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../data/news_repository.dart';
import '../presentation/news_list_screen.dart'; // 목록 새로고침(newsListProvider)을 위해

class NewsAddScreen extends ConsumerStatefulWidget {
  const NewsAddScreen({super.key});

  @override
  ConsumerState<NewsAddScreen> createState() => _NewsAddScreenState();
}

class _NewsAddScreenState extends ConsumerState<NewsAddScreen> {
  final _titleController = TextEditingController();
  final _contentController = TextEditingController();
  final _imageUrlController = TextEditingController();
  bool _isSaving = false;

  Future<void> _saveNews() async {
    if (_titleController.text.isEmpty || _contentController.text.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text("Title and Content are required.")),
      );
      return;
    }

    setState(() => _isSaving = true);

    try {
      await ref.read(newsRepositoryProvider).addNews(
            title: _titleController.text,
            content: _contentController.text,
            imageUrl: _imageUrlController.text.isNotEmpty
                ? _imageUrlController.text
                : null,
          );

      // 목록 새로고침
      ref.invalidate(newsListProvider);

      if (mounted) {
        context.pop(); // 뒤로 가기
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text("Error: $e")));
        setState(() => _isSaving = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text("Archive News")),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            TextField(
              controller: _titleController,
              decoration: const InputDecoration(
                labelText: "News Title",
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _contentController,
              maxLines: 5,
              decoration: const InputDecoration(
                labelText: "Content / Summary",
                border: OutlineInputBorder(),
                alignLabelWithHint: true,
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _imageUrlController,
              decoration: const InputDecoration(
                labelText: "Image URL (Optional)",
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.link),
              ),
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              height: 50,
              child: ElevatedButton.icon(
                onPressed: _isSaving ? null : _saveNews,
                icon: _isSaving
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                            strokeWidth: 2, color: Colors.white))
                    : const Icon(Icons.save),
                label: Text(_isSaving ? "Saving..." : "Save to Archive"),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
