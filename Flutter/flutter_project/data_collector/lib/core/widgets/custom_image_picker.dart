import 'dart:io';
import 'package:flutter/foundation.dart'; // kIsWeb
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:firebase_storage/firebase_storage.dart';
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:http/http.dart' as http;
import 'package:permission_handler/permission_handler.dart';

class CustomImagePicker extends StatefulWidget {
  final String? initialUrl;
  final Function(String? url) onImageSelected;
  final int maxSizeInBytes; // 기본 2MB
  final bool isCircle; // 원형(프로필) or 사각형(뉴스/일반)

  const CustomImagePicker({
    Key? key,
    this.initialUrl,
    required this.onImageSelected,
    this.maxSizeInBytes = 2 * 1024 * 1024, // 2MB
    this.isCircle = false, // 기본값: 사각형
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

  // 에러 메시지 표시
  void _showError(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red),
    );
  }

  // 1. URL 유효성 및 크기 검증 (HEAD 요청)
  Future<void> _validateAndSetUrl(String url) async {
    if (url.isEmpty) return;

    setState(() => _isLoading = true);

    try {
      final uri = Uri.tryParse(url);
      if (uri == null || !uri.hasScheme) throw "유효하지 않은 URL입니다.";

      // HEAD 요청으로 파일 크기 확인 (CORS 에러 발생 시 무시하고 통과)
      try {
        final response = await http.head(uri);
        if (response.statusCode == 200) {
          final contentLength = response.headers['content-length'];
          if (contentLength != null) {
            final size = int.parse(contentLength);
            if (size > widget.maxSizeInBytes) {
              throw "이미지가 너무 큽니다. (제한: ${widget.maxSizeInBytes ~/ 1024 ~/ 1024}MB)";
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

  // 2. 파일 선택 및 업로드 (권한 체크 + 압축)
  Future<void> _pickAndUploadImage() async {
    // 권한 체크 (모바일만)
    if (!kIsWeb) {
      PermissionStatus status;
      if (Platform.isAndroid) {
        // Android 13 이상(photos) vs 이하(storage)
        final status13 = await Permission.photos.status;
        if (status13.isDenied) {
          status = await Permission.photos.request();
        } else {
          status = await Permission.storage.request();
        }
      } else {
        // iOS
        status = await Permission.photos.request();
      }

      if (status.isPermanentlyDenied) {
        _showError("갤러리 접근 권한이 필요합니다. 설정에서 허용해주세요.");
        openAppSettings();
        return;
      }
    }

    // 이미지 선택
    final picker = ImagePicker();
    final XFile? image = await picker.pickImage(source: ImageSource.gallery);

    if (image == null) return;

    setState(() => _isLoading = true);

    try {
      Uint8List? fileBytes;

      if (kIsWeb) {
        fileBytes = await image.readAsBytes();
        if (fileBytes.length > widget.maxSizeInBytes) {
          throw "이미지가 너무 큽니다. (웹 업로드 제한: 2MB)";
        }
      } else {
        // 모바일: 파일 압축
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

      // Firebase Storage 업로드
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

  // 3. URL 입력 다이얼로그
  void _showUrlInputDialog() {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text("이미지 주소 입력"),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
              hintText: "https://example.com/image.jpg", labelText: "URL 붙여넣기"),
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

  @override
  Widget build(BuildContext context) {
    // A. 이미지 위젯 결정 (원형 vs 사각형)
    Widget imageWidget;

    if (_displayUrl != null) {
      // 이미지가 있을 때
      if (widget.isCircle) {
        imageWidget = CircleAvatar(
          radius: 60,
          backgroundImage: NetworkImage(_displayUrl!),
          backgroundColor: Colors.grey[200],
        );
      } else {
        // 사각형 (News 등)
        imageWidget = ClipRRect(
          borderRadius: BorderRadius.circular(12),
          child: Image.network(
            _displayUrl!,
            width: double.infinity,
            height: 200,
            fit: BoxFit.cover,
            errorBuilder: (context, error, stackTrace) {
              return Container(
                height: 200,
                color: Colors.grey[200],
                alignment: Alignment.center,
                child: const Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(Icons.broken_image, color: Colors.grey, size: 40),
                    SizedBox(height: 8),
                    Text("이미지를 불러올 수 없습니다",
                        style: TextStyle(color: Colors.grey, fontSize: 12)),
                  ],
                ),
              );
            },
            loadingBuilder: (context, child, loadingProgress) {
              if (loadingProgress == null) return child;
              return Container(
                height: 200,
                color: Colors.grey[100],
                alignment: Alignment.center,
                child: CircularProgressIndicator(
                  value: loadingProgress.expectedTotalBytes != null
                      ? loadingProgress.cumulativeBytesLoaded /
                          loadingProgress.expectedTotalBytes!
                      : null,
                ),
              );
            },
          ),
        );
      }
    } else {
      // 이미지가 없을 때 (Placeholder)
      if (widget.isCircle) {
        imageWidget = CircleAvatar(
          radius: 60,
          backgroundColor: Colors.grey[200],
          child: const Icon(Icons.camera_alt, size: 40, color: Colors.grey),
        );
      } else {
        imageWidget = Container(
          width: double.infinity,
          height: 200,
          decoration: BoxDecoration(
            color: Colors.grey[200],
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: Colors.grey[300]!),
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: const [
              Icon(Icons.add_photo_alternate, size: 50, color: Colors.grey),
              SizedBox(height: 8),
              Text("사진 추가 (탭)", style: TextStyle(color: Colors.grey)),
            ],
          ),
        );
      }
    }

    // B. 최종 레이아웃 구성
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        GestureDetector(
          onTap: _showSelectionSheet,
          child: Stack(
            alignment: Alignment.center,
            children: [
              imageWidget,
              if (_isLoading)
                Positioned.fill(
                  child: Container(
                    decoration: BoxDecoration(
                      color: Colors.black38,
                      borderRadius:
                          BorderRadius.circular(widget.isCircle ? 60 : 12),
                    ),
                    child: const Center(
                      child: CircularProgressIndicator(color: Colors.white),
                    ),
                  ),
                ),
            ],
          ),
        ),

        // 삭제/변경 버튼 (사각형 모드일 때만 하단에 표시, 원형은 탭으로 충분)
        if (!widget.isCircle && _displayUrl != null && !_isLoading)
          Padding(
            padding: const EdgeInsets.only(top: 8.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton.icon(
                  onPressed: () {
                    setState(() => _displayUrl = null);
                    widget.onImageSelected(null);
                  },
                  icon: const Icon(Icons.delete, size: 16, color: Colors.red),
                  label: const Text("삭제", style: TextStyle(color: Colors.red)),
                ),
                TextButton.icon(
                  onPressed: _showSelectionSheet,
                  icon: const Icon(Icons.edit, size: 16),
                  label: const Text("변경"),
                ),
              ],
            ),
          ),
      ],
    );
  }

  void _showSelectionSheet() {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (context) => SafeArea(
        child: Wrap(
          children: [
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: const Text('갤러리에서 선택'),
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
          ],
        ),
      ),
    );
  }
}
