import 'dart:io';
import 'package:flutter/foundation.dart'; // kIsWeb
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:http/http.dart' as http; // URL 헤더 체크용

class CustomImagePicker extends StatefulWidget {
  final String? initialUrl;
  final Function(String? url) onImageSelected; // null이면 삭제
  final int maxSizeInBytes; // 기본 2MB

  const CustomImagePicker({
    Key? key,
    this.initialUrl,
    required this.onImageSelected,
    this.maxSizeInBytes = 2 * 1024 * 1024, // 2MB
  }) : super(key: key);

  @override
  State<CustomImagePicker> createState() => _CustomImagePickerState();
}

class _CustomImagePickerState extends State<CustomImagePicker> {
  String? _displayUrl;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _displayUrl = widget.initialUrl;
  }

  // 에러 알림
  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red),
    );
  }

  // 1. URL 직접 입력 및 검증
  Future<void> _validateAndSetUrl(String url) async {
    if (url.isEmpty) return;
    setState(() => _isLoading = true);

    try {
      final uri = Uri.tryParse(url);
      if (uri == null || !uri.hasScheme) throw "유효하지 않은 URL입니다.";

      // HEAD 요청으로 크기 확인 (CORS 문제로 실패할 수 있으므로 실패 시 경고만 하고 통과)
      try {
        final response = await http.head(uri);
        if (response.statusCode == 200) {
          final contentLength = response.headers['content-length'];
          if (contentLength != null) {
            final size = int.parse(contentLength);
            if (size > widget.maxSizeInBytes) {
              throw "이미지가 너무 큽니다. (제한: 2MB)";
            }
          }
        }
      } catch (e) {
        debugPrint("URL 헤더 체크 실패(무시됨): $e");
      }

      setState(() => _displayUrl = url);
      widget.onImageSelected(url);
    } catch (e) {
      _showError(e.toString());
    } finally {
      setState(() => _isLoading = false);
    }
  }

  // 2. 파일 선택 및 업로드 (압축 포함)
  Future<void> _pickAndUploadImage() async {
    final picker = ImagePicker();
    final XFile? image = await picker.pickImage(source: ImageSource.gallery);

    if (image == null) return;

    setState(() => _isLoading = true);

    try {
      Uint8List? fileBytes;

      // 웹/모바일 분기
      if (kIsWeb) {
        fileBytes = await image.readAsBytes();
        if (fileBytes.length > widget.maxSizeInBytes) {
          throw "이미지가 너무 큽니다. (웹에서는 2MB 이하만 가능)";
        }
      } else {
        // 모바일: 압축 시도
        final File file = File(image.path);
        int size = await file.length();

        if (size > widget.maxSizeInBytes) {
          final compressed = await FlutterImageCompress.compressWithFile(
            file.absolute.path,
            minWidth: 1024,
            minHeight: 1024,
            quality: 80,
          );
          fileBytes = compressed;
        } else {
          fileBytes = await file.readAsBytes();
        }
      }

      if (fileBytes == null) throw "이미지 처리 실패";

      // 업로드
      final fileName = '${DateTime.now().millisecondsSinceEpoch}_${image.name}';
      final ref = FirebaseStorage.instance.ref().child('uploads/$fileName');

      await ref.putData(fileBytes, SettableMetadata(contentType: 'image/jpeg'));
      final downloadUrl = await ref.getDownloadURL();

      setState(() => _displayUrl = downloadUrl);
      widget.onImageSelected(downloadUrl);
    } catch (e) {
      _showError("업로드 실패: $e");
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        GestureDetector(
          onTap: () {
            showModalBottomSheet(
              context: context,
              builder: (context) => Wrap(
                children: [
                  ListTile(
                    leading: const Icon(Icons.photo_library),
                    title: const Text('갤러리에서 선택 (자동 압축)'),
                    onTap: () {
                      Navigator.pop(context);
                      _pickAndUploadImage();
                    },
                  ),
                  ListTile(
                    leading: const Icon(Icons.link),
                    title: const Text('이미지 주소(URL) 입력'),
                    onTap: () {
                      Navigator.pop(context);
                      _showUrlInputDialog();
                    },
                  ),
                  if (_displayUrl != null)
                    ListTile(
                      leading: const Icon(Icons.delete, color: Colors.red),
                      title: const Text('이미지 삭제',
                          style: TextStyle(color: Colors.red)),
                      onTap: () {
                        setState(() => _displayUrl = null);
                        widget.onImageSelected(null);
                        Navigator.pop(context);
                      },
                    ),
                ],
              ),
            );
          },
          child: CircleAvatar(
            radius: 50,
            backgroundColor: Colors.grey[200],
            backgroundImage:
                _displayUrl != null ? NetworkImage(_displayUrl!) : null,
            child: _isLoading
                ? const CircularProgressIndicator()
                : (_displayUrl == null
                    ? const Icon(Icons.camera_alt, size: 30, color: Colors.grey)
                    : null),
          ),
        ),
        const SizedBox(height: 8),
        const Text("탭하여 사진 추가/변경",
            style: TextStyle(fontSize: 12, color: Colors.grey)),
      ],
    );
  }

  void _showUrlInputDialog() {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("이미지 주소 입력"),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(hintText: "https://..."),
          autofocus: true,
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context), child: const Text("취소")),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              _validateAndSetUrl(controller.text);
            },
            child: const Text("확인"),
          ),
        ],
      ),
    );
  }
}
