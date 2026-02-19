import 'package:flutter/material.dart';

class TagInputWidget extends StatefulWidget {
  final List<String> initialTags;
  final Function(List<String>) onChanged;

  const TagInputWidget({
    Key? key,
    required this.initialTags,
    required this.onChanged,
  }) : super(key: key);

  @override
  State<TagInputWidget> createState() => _TagInputWidgetState();
}

class _TagInputWidgetState extends State<TagInputWidget> {
  final TextEditingController _controller = TextEditingController();
  late List<String> _tags;

  @override
  void initState() {
    super.initState();
    _tags = List.from(widget.initialTags);
  }

  void _addTag(String value) {
    final tag = value.trim();
    if (tag.isNotEmpty && !_tags.contains(tag)) {
      setState(() {
        _tags.add(tag);
        _controller.clear();
      });
      widget.onChanged(_tags);
    }
  }

  void _removeTag(String tag) {
    setState(() {
      _tags.remove(tag);
    });
    widget.onChanged(_tags);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          controller: _controller,
          decoration: const InputDecoration(
            labelText: '태그 추가 (Enter/Tab으로 입력)',
            suffixIcon: Icon(Icons.tag),
            border: OutlineInputBorder(),
          ),
          onSubmitted: _addTag, // Enter 키 처리
          // Tab 키 처리는 FocusNode 등으로 복잡해질 수 있어 Enter 권장
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 4,
          children: _tags
              .map((tag) => Chip(
                    label: Text(tag),
                    deleteIcon: const Icon(Icons.close, size: 18),
                    onDeleted: () => _removeTag(tag),
                    backgroundColor: Colors.blue[50],
                    labelStyle: TextStyle(color: Colors.blue[800]),
                  ))
              .toList(),
        ),
      ],
    );
  }
}
