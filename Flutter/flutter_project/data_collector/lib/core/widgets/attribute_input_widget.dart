import 'package:flutter/material.dart';

class AttributeInputWidget extends StatefulWidget {
  final Map<String, dynamic> initialAttributes;
  final Function(Map<String, dynamic>) onChanged;

  const AttributeInputWidget({
    Key? key,
    required this.initialAttributes,
    required this.onChanged,
  }) : super(key: key);

  @override
  State<AttributeInputWidget> createState() => _AttributeInputWidgetState();
}

class _AttributeInputWidgetState extends State<AttributeInputWidget> {
  final _keyCtrl = TextEditingController();
  final _valCtrl = TextEditingController();
  late Map<String, dynamic> _attributes;

  // 포커스 이동용
  final _keyFocus = FocusNode();
  final _valFocus = FocusNode();

  @override
  void initState() {
    super.initState();
    _attributes = Map.from(widget.initialAttributes);
  }

  void _addAttribute() {
    final key = _keyCtrl.text.trim();
    final val = _valCtrl.text.trim();

    if (key.isNotEmpty && val.isNotEmpty) {
      setState(() {
        _attributes[key] = val;
        _keyCtrl.clear();
        _valCtrl.clear();
      });
      widget.onChanged(_attributes);
      _keyFocus.requestFocus(); // 입력 후 다시 Key로 포커스 이동
    }
  }

  void _removeAttribute(String key) {
    setState(() {
      _attributes.remove(key);
    });
    widget.onChanged(_attributes);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _keyCtrl,
                focusNode: _keyFocus,
                decoration: const InputDecoration(labelText: 'Key (예: 직업)'),
                textInputAction: TextInputAction.next, // 다음으로
                onSubmitted: (_) => _valFocus.requestFocus(),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: TextField(
                controller: _valCtrl,
                focusNode: _valFocus,
                decoration: const InputDecoration(labelText: 'Value (예: 개발자)'),
                textInputAction: TextInputAction.done, // 완료
                onSubmitted: (_) => _addAttribute(),
              ),
            ),
            IconButton(
              onPressed: _addAttribute,
              icon: const Icon(Icons.add_circle, color: Colors.blue),
            ),
          ],
        ),
        const SizedBox(height: 10),
        // 추가된 속성 리스트 보여주기
        if (_attributes.isNotEmpty)
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: Colors.grey[100],
              borderRadius: BorderRadius.circular(8),
            ),
            child: Column(
              children: _attributes.entries
                  .map((e) => Padding(
                        padding: const EdgeInsets.symmetric(vertical: 4),
                        child: Row(
                          children: [
                            Text(e.key,
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold)),
                            const Text(" : "),
                            Expanded(child: Text(e.value.toString())),
                            InkWell(
                              onTap: () => _removeAttribute(e.key),
                              child: const Icon(Icons.remove_circle_outline,
                                  color: Colors.red, size: 20),
                            )
                          ],
                        ),
                      ))
                  .toList(),
            ),
          ),
      ],
    );
  }
}
