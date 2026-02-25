import 'dart:typed_data';
import 'package:flutter/foundation.dart'; // Needed for kIsWeb
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:permission_handler/permission_handler.dart';

class CustomImagePicker extends StatefulWidget {
  final String? initialUrl;
  final void Function(String url) onImageSelected;
  final bool isCircle;

  const CustomImagePicker({
    Key? key,
    this.initialUrl,
    required this.onImageSelected,
    this.isCircle = false,
  }) : super(key: key);

  @override
  State<CustomImagePicker> createState() => _CustomImagePickerState();
}

class _CustomImagePickerState extends State<CustomImagePicker> {
  String? _currentUrl;
  bool _isUploading = false;

  @override
  void initState() {
    super.initState();
    _currentUrl = widget.initialUrl;
  }

  // Detect if platform is wide (PC Web) or mobile
  bool get _isDesktop {
    if (kIsWeb) {
      final width = WidgetsBinding.instance.platformDispatcher.views.first.physicalSize.width;
      return width > 600;
    }
    return false;
  }

  // Shows options dynamically based on screen size
  void _showPickerOptions(BuildContext context) {
    if (_isDesktop) {
      // Desktop: Show center dialog
      showDialog(
        context: context,
        builder: (ctx) => SimpleDialog(
          title: const Text('이미지 추가'),
          children: [
            SimpleDialogOption(
              onPressed: () {
                Navigator.pop(ctx);
                _pickFromGallery();
              },
              child: const Row(
                children: [
                  Icon(Icons.photo_library_outlined),
                  SizedBox(width: 12),
                  Text('갤러리에서 선택'),
                ],
              ),
            ),
            SimpleDialogOption(
              onPressed: () {
                Navigator.pop(ctx);
                _showUrlInputDialog(context);
              },
              child: const Row(
                children: [
                  Icon(Icons.link),
                  SizedBox(width: 12),
                  Text('URL 직접 입력'),
                ],
              ),
            ),
            if (_currentUrl != null && _currentUrl!.isNotEmpty)
              SimpleDialogOption(
                onPressed: () {
                  Navigator.pop(ctx);
                  setState(() => _currentUrl = null);
                  widget.onImageSelected('');
                },
                child: const Row(
                  children: [
                    Icon(Icons.delete_outline, color: Colors.red),
                    SizedBox(width: 12),
                    Text('이미지 삭제', style: TextStyle(color: Colors.red)),
                  ],
                ),
              ),
          ],
        ),
      );
    } else {
      // Mobile: Show bottom sheet
      showModalBottomSheet(
        context: context,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
        ),
        builder: (ctx) => SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(height: 8),
              Container(
                width: 36,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.grey[300],
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              const SizedBox(height: 12),
              ListTile(
                leading: const Icon(Icons.photo_library_outlined),
                title: const Text('갤러리에서 선택'),
                onTap: () {
                  Navigator.pop(ctx);
                  _pickFromGallery();
                },
              ),
              ListTile(
                leading: const Icon(Icons.link),
                title: const Text('URL 직접 입력'),
                onTap: () {
                  Navigator.pop(ctx);
                  _showUrlInputDialog(context);
                },
              ),
              if (_currentUrl != null && _currentUrl!.isNotEmpty)
                ListTile(
                  leading: const Icon(Icons.delete_outline, color: Colors.red),
                  title: const Text('이미지 삭제', style: TextStyle(color: Colors.red)),
                  onTap: () {
                    Navigator.pop(ctx);
                    setState(() => _currentUrl = null);
                    widget.onImageSelected('');
                  },
                ),
              const SizedBox(height: 8),
            ],
          ),
        ),
      );
    }
  }

  // Gallery picking logic
  Future<void> _pickFromGallery() async {
    if (!kIsWeb) {
      final status = await Permission.photos.request();
      if (!status.isGranted) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('갤러리 접근 권한이 필요합니다.')),
          );
        }
        return;
      }
    }

    final picker = ImagePicker();
    final picked = await picker.pickImage(source: ImageSource.gallery);
    if (picked == null) return;

    setState(() => _isUploading = true);

    try {
      Uint8List imageBytes = await picked.readAsBytes();

      if (imageBytes.length > 2 * 1024 * 1024) {
        final compressed = await FlutterImageCompress.compressWithList(
          imageBytes,
          quality: 75,
        );
        imageBytes = compressed;
      }

      final fileName = '${DateTime.now().millisecondsSinceEpoch}_${picked.name}';
      final ref = FirebaseStorage.instance.ref().child('uploads/$fileName');
      await ref.putData(imageBytes);
      final url = await ref.getDownloadURL();

      setState(() => _currentUrl = url);
      widget.onImageSelected(url);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('업로드 실패: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isUploading = false);
    }
  }

  // URL dialog logic
  void _showUrlInputDialog(BuildContext context) {
    final urlCtrl = TextEditingController();
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('이미지 URL 입력'),
        content: TextField(
          controller: urlCtrl,
          autofocus: true,
          decoration: const InputDecoration(
            labelText: 'https://...',
            border: OutlineInputBorder(),
          ),
          keyboardType: TextInputType.url,
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('취소'),
          ),
          ElevatedButton(
            onPressed: () {
              final url = urlCtrl.text.trim();
              if (url.isNotEmpty) {
                setState(() => _currentUrl = url);
                widget.onImageSelected(url);
              }
              Navigator.pop(ctx);
            },
            child: const Text('확인'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (widget.isCircle) {
      return GestureDetector(
        onTap: () => _showPickerOptions(context),
        child: Stack(
          alignment: Alignment.bottomRight,
          children: [
            CircleAvatar(
              radius: 48,
              backgroundImage:
                  _currentUrl != null && _currentUrl!.isNotEmpty
                      ? NetworkImage(_currentUrl!)
                      : null,
              child: (_currentUrl == null || _currentUrl!.isEmpty)
                  ? const Icon(Icons.person, size: 48)
                  : null,
            ),
            CircleAvatar(
              radius: 14,
              backgroundColor: Colors.blue,
              child: _isUploading
                  ? const SizedBox(
                      width: 12,
                      height: 12,
                      child: CircularProgressIndicator(
                          strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.edit, size: 14, color: Colors.white),
            ),
          ],
        ),
      );
    }

    // Fixed height (150) prevents the image from taking over the dialog
    return GestureDetector(
      onTap: () => _showPickerOptions(context),
      child: Container(
        height: 150,
        width: double.infinity,
        decoration: BoxDecoration(
          color: Colors.grey[100],
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.grey[300]!),
        ),
        child: _isUploading
            ? const Center(child: CircularProgressIndicator())
            : _currentUrl != null && _currentUrl!.isNotEmpty
                ? ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: Image.network(
                      _currentUrl!,
                      fit: BoxFit.cover,
                      width: double.infinity,
                      errorBuilder: (_, __, ___) =>
                          const Center(child: Icon(Icons.broken_image, size: 48)),
                    ),
                  )
                : Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.add_photo_alternate_outlined,
                          size: 40, color: Colors.grey[400]),
                      const SizedBox(height: 8),
                      Text('사진 추가',
                          style: TextStyle(color: Colors.grey[500])),
                    ],
                  ),
      ),
    );
  }
}
